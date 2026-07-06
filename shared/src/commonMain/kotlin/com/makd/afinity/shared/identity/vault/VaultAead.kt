package com.makd.afinity.shared.identity.vault

/**
 * Authenticated encryption (AEAD) for the 32-byte identity seed, keyed by the password-derived
 * KEK (#142 rung 2). Platform actuals use only vetted, platform-native AEADs — never hand-rolled:
 *   - Android / JVM host: AES-256-GCM via the platform JCE provider.
 *   - iOS: libsodium secretbox (XSalsa20-Poly1305) via the ionspin bindings (same stack rung 1 uses).
 *
 * The KEK is always 32 bytes ([KEY_SIZE]); each platform picks its own [nonceSize] and the nonce is
 * stored in the blob, so the two ciphertext formats never need to interoperate — a device's vault
 * blob never leaves that device.
 */
internal expect object VaultAead {
    /** Nonce length in bytes this platform's AEAD requires (12 for AES-GCM, 24 for secretbox). */
    val nonceSize: Int

    /**
     * Encrypts [plaintext] under 32-byte [key] with [nonce]. Returns ciphertext including the
     * authentication tag.
     */
    fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray): ByteArray

    /**
     * Decrypts and verifies [ciphertext] under 32-byte [key] with [nonce]. Returns the plaintext,
     * or null if authentication fails (wrong key / tampered data). NEVER returns unverified bytes.
     */
    fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray?
}

/** The KEK / AEAD key size in bytes. Fits both AES-256 and libsodium secretbox. */
internal const val KEY_SIZE: Int = 32
