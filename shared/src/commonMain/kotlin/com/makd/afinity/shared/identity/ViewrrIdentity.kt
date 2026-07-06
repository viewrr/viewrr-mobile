package com.makd.afinity.shared.identity

/**
 * An Ed25519 identity keypair. The [seed] (32 bytes) is the BIP39-derived root secret —
 * the first 32 bytes of the BIP39-512 seed — and is what signing consumes. [publicKey] is
 * the 32-byte raw Ed25519 public key; [publicKeyHex] is its lowercase-hex wire form.
 */
class IdentityKeyPair(
    val seed: ByteArray,
    val publicKey: ByteArray,
) {
    init {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        require(publicKey.size == 32) { "publicKey must be 32 bytes, got ${publicKey.size}" }
    }

    val publicKeyHex: String get() = publicKey.toLowerHex()

    /** Derived display-only handle for this identity. */
    val handle: String get() = ViewrrIdentity.deriveHandle(publicKey)
}

/**
 * Self-custody identity core for viewrr mobile (issue #142, rung 1).
 *
 * FROZEN cross-repo contracts — must match viewrr-web#1, viewrr #120/#135, and the #121
 * worklet byte-for-byte:
 *   1. keypair: mnemonic -> BIP39 seed64 -> seed64[0:32] -> Ed25519 keyPair(seed).
 *   2. REGISTER_MESSAGE = the exact bytes "viewrr:register".
 *   3. publicKey wire format = lowercase hex of the 32 raw bytes.
 *   4. deriveHandle(pk) = BIP39[(pk0<<8|pk1)%2048]-BIP39[(pk2<<8|pk3)%2048]-hex(pk30)hex(pk31),
 *      golden: 00*32 -> "abandon-abandon-0000".
 */
object ViewrrIdentity {

    /** The message every client signs at registration (server IdentityService.REGISTER_MESSAGE). */
    const val REGISTER_MESSAGE: String = "viewrr:register"

    // ---- mnemonic ----

    fun generateMnemonic(): String = Bip39.generateMnemonic()

    fun validateMnemonic(mnemonic: String): Boolean = Bip39.validateMnemonic(mnemonic)

    /** Full 64-byte BIP39 seed (not the 32-byte keypair seed). */
    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray =
        Bip39.mnemonicToSeed(mnemonic, passphrase)

    // ---- keypair derivation ----

    /**
     * Derives the identity keypair from a mnemonic. The 32-byte keypair seed is the first
     * 32 bytes of the BIP39-512 seed (frozen reduction shared with server + web).
     */
    fun keyPairFromMnemonic(mnemonic: String, passphrase: String = ""): IdentityKeyPair {
        val seed32 = mnemonicToSeed(mnemonic, passphrase).copyOf(32)
        return keyPairFromSeed(seed32)
    }

    /** Derives the identity keypair from a raw 32-byte Ed25519 seed. */
    fun keyPairFromSeed(seed32: ByteArray): IdentityKeyPair {
        require(seed32.size == 32) { "seed must be 32 bytes, got ${seed32.size}" }
        val publicKey = Ed25519.publicKeyFromSeed(seed32)
        return IdentityKeyPair(seed = seed32.copyOf(), publicKey = publicKey)
    }

    // ---- handle ----

    fun deriveHandle(publicKey: ByteArray): String {
        require(publicKey.size == 32) {
            "deriveHandle: expected a 32-byte Ed25519 public key, got ${publicKey.size} bytes"
        }
        val w1 = BIP39_ENGLISH[(((publicKey[0].toInt() and 0xff) shl 8) or (publicKey[1].toInt() and 0xff)) % 2048]
        val w2 = BIP39_ENGLISH[(((publicKey[2].toInt() and 0xff) shl 8) or (publicKey[3].toInt() and 0xff)) % 2048]
        val suffix = byteToHex(publicKey[30]) + byteToHex(publicKey[31])
        return "$w1-$w2-$suffix"
    }

    fun deriveHandle(publicKeyHex: String): String {
        require(publicKeyHex.length % 2 == 0 && publicKeyHex.all { it in '0'..'9' || it in 'a'..'f' }) {
            "deriveHandle: publicKeyHex must be lowercase hex with an even length"
        }
        return deriveHandle(hexToBytes(publicKeyHex))
    }

    // ---- sign / verify ----

    /** Signs [message] with the 32-byte identity [seed]. Returns a 64-byte detached signature. */
    fun sign(message: ByteArray, seed: ByteArray): ByteArray {
        require(seed.size == 32) { "seed must be 32 bytes, got ${seed.size}" }
        return Ed25519.sign(message, seed)
    }

    /** Verifies a 64-byte [signature] over [message] against a 32-byte [publicKey]. */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean =
        Ed25519.verify(message, signature, publicKey)

    private fun byteToHex(b: Byte): String {
        val hexChars = "0123456789abcdef"
        val v = b.toInt() and 0xff
        return "${hexChars[v ushr 4]}${hexChars[v and 0x0f]}"
    }
}
