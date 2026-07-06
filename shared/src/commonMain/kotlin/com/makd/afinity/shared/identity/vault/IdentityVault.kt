package com.makd.afinity.shared.identity.vault

import com.makd.afinity.shared.identity.IdentityKeyPair
import com.makd.afinity.shared.identity.ViewrrIdentity
import com.makd.afinity.shared.identity.pbkdf2HmacSha512
import com.makd.afinity.shared.identity.secureRandomBytes

/**
 * Secure at-rest storage for the self-custody Ed25519 identity seed (#142 rung 2).
 *
 * The 32-byte seed is encrypted behind a local master password before it ever touches disk:
 *   1. KEK = PBKDF2-HMAC-SHA512(masterPassword, random salt, [KDF_ITERATIONS]) — the vetted KDF
 *      primitive from rung 1, identical on every platform, so unlock is host-testable.
 *   2. ciphertext = AEAD_seal(KEK, random nonce, seed) — AES-256-GCM on Android/JVM, libsodium
 *      secretbox on iOS ([VaultAead]). No hand-rolled crypto anywhere.
 *   3. The [VaultBlob] (version, iterations, salt, nonce, ciphertext) is written to platform secure
 *      storage ([VaultStorage]). The plaintext seed / secretKey is NEVER persisted.
 *
 * The storage backend is injected so this orchestration stays in commonMain and unit-testable with
 * an in-memory stub; production code passes the Keystore/Keychain-backed implementation.
 */
class IdentityVault(private val storage: VaultStorage) {

    /**
     * Encrypts [seed] (the 32-byte identity seed) behind [masterPassword] and persists it,
     * replacing any existing vault. A fresh salt and nonce are generated on every call.
     */
    fun store(seed: ByteArray, masterPassword: String) {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        require(masterPassword.isNotEmpty()) { "master password must not be empty" }

        val salt = secureRandomBytes(SALT_SIZE)
        val nonce = secureRandomBytes(VaultAead.nonceSize)
        val kek = deriveKek(masterPassword, salt, KDF_ITERATIONS)
        val ciphertext = VaultAead.seal(kek, nonce, seed)

        storage.write(VaultBlob(KDF_ITERATIONS, salt, nonce, ciphertext).encode())
    }

    /**
     * Decrypts the stored vault with [masterPassword] and returns the recovered [IdentityKeyPair].
     *
     * Returns null when there is no vault, the master password is wrong, or the blob was tampered
     * with — AEAD authentication guarantees a wrong password can never yield garbage key material.
     */
    fun unlock(masterPassword: String): IdentityKeyPair? {
        val raw = storage.read() ?: return null
        val blob = try {
            VaultBlob.decode(raw)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val kek = deriveKek(masterPassword, blob.salt, blob.kdfIterations)
        val seed = VaultAead.open(kek, blob.nonce, blob.ciphertext) ?: return null
        if (seed.size != 32) return null
        return ViewrrIdentity.keyPairFromSeed(seed)
    }

    /** True iff an identity vault is currently stored. */
    fun exists(): Boolean = storage.exists()

    /** Permanently deletes the stored vault. */
    fun clear() = storage.clear()

    private fun deriveKek(masterPassword: String, salt: ByteArray, iterations: Int): ByteArray =
        pbkdf2HmacSha512(
            password = masterPassword.encodeToByteArray(),
            salt = salt,
            iterations = iterations,
            dkLenBytes = KEY_SIZE,
        )

    companion object {
        /**
         * PBKDF2-HMAC-SHA512 work factor. High-iteration per the rung 2 spec; a one-time cost paid
         * on store/unlock only. Stored in the blob so it can be raised later without breaking
         * existing vaults.
         */
        const val KDF_ITERATIONS: Int = 210_000

        /** KDF salt size in bytes. */
        const val SALT_SIZE: Int = 16
    }
}
