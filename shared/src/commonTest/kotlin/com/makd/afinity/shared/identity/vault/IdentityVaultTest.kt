package com.makd.afinity.shared.identity.vault

import com.makd.afinity.shared.identity.ViewrrIdentity
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Rung 2 at-rest vault tests (#142). Runs on the JVM host via :shared:testAndroidHostTest against
 * the real KDF (PBKDF2-HMAC-SHA512) + real AEAD (AES-256-GCM) with an in-memory storage stub, so
 * these exercise the actual crypto orchestration — not a mock.
 */
class IdentityVaultTest {

    private val masterPassword = "correct horse battery staple"

    /** A real 32-byte identity seed derived through the rung 1 core. */
    private fun freshSeed(): ByteArray {
        val mnemonic = ViewrrIdentity.generateMnemonic()
        return ViewrrIdentity.keyPairFromMnemonic(mnemonic).seed
    }

    @Test
    fun store_then_unlock_returns_the_same_keypair() {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        val seed = freshSeed()
        val expected = ViewrrIdentity.keyPairFromSeed(seed)

        vault.store(seed, masterPassword)
        val unlocked = vault.unlock(masterPassword)

        assertNotNull(unlocked, "unlock with the correct password must return a keypair")
        assertContentEquals(expected.seed, unlocked.seed, "recovered seed must match the stored seed")
        assertContentEquals(expected.publicKey, unlocked.publicKey, "recovered public key must match")
        assertEquals(expected.publicKeyHex, unlocked.publicKeyHex)
        assertEquals(expected.handle, unlocked.handle)
    }

    @Test
    fun unlock_with_wrong_password_fails_cleanly() {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        val seed = freshSeed()
        vault.store(seed, masterPassword)

        val result = vault.unlock("wrong password")

        // AEAD authentication must reject the wrong key — null, never garbage key material.
        assertNull(result, "wrong password must not return a keypair")
    }

    @Test
    fun persisted_blob_contains_no_plaintext_seed_bytes() {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        val seed = freshSeed()

        vault.store(seed, masterPassword)
        val blob = storage.lastWritten
        assertNotNull(blob, "store must persist a blob")

        // The raw 32-byte seed must appear nowhere in the persisted bytes.
        assertFalse(
            containsSubsequence(blob, seed),
            "plaintext seed bytes must never appear in the at-rest blob",
        )
        // And neither must the derived public key (paranoia — the whole record should be opaque).
        val publicKey = ViewrrIdentity.keyPairFromSeed(seed).publicKey
        assertFalse(
            containsSubsequence(blob, publicKey),
            "public key bytes must not leak into the at-rest blob",
        )
    }

    @Test
    fun exists_and_clear_track_vault_lifecycle() {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        assertFalse(vault.exists())

        vault.store(freshSeed(), masterPassword)
        assertTrue(vault.exists())

        vault.clear()
        assertFalse(vault.exists())
        assertNull(vault.unlock(masterPassword), "no vault after clear() -> unlock returns null")
    }

    @Test
    fun tampered_blob_fails_authentication() {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        vault.store(freshSeed(), masterPassword)

        // Flip the last ciphertext byte; AEAD must reject it.
        val blob = storage.lastWritten!!.copyOf()
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0xff).toByte()
        storage.write(blob)

        assertNull(vault.unlock(masterPassword), "tampered ciphertext must not decrypt")
    }

    @Test
    fun each_store_uses_a_fresh_salt_and_nonce() {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        val seed = freshSeed()

        vault.store(seed, masterPassword)
        val first = storage.lastWritten!!.copyOf()
        vault.store(seed, masterPassword)
        val second = storage.lastWritten!!.copyOf()

        // Same seed + password, but random salt/nonce => different ciphertext each time.
        assertFalse(first.contentEquals(second), "re-store must not produce identical blobs")
        // Both still unlock.
        assertNotNull(vault.unlock(masterPassword))
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        outer@ for (start in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[start + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }
}
