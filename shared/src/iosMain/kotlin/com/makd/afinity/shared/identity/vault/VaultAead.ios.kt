package com.makd.afinity.shared.identity.vault

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.secretbox.SecretBox

/**
 * iOS AEAD via libsodium secretbox (XSalsa20-Poly1305) through the ionspin bindings (#142 rung 2) —
 * the same vetted crypto stack rung 1 uses for Ed25519. secretbox authenticates on decrypt, so a
 * wrong key or tampered ciphertext throws and [open] returns null rather than any unverified bytes.
 *
 * libsodium's native init is synchronous, so the callback completes inline.
 */
private fun ensureSodium() {
    if (!LibsodiumInitializer.isInitialized()) {
        LibsodiumInitializer.initializeWithCallback { }
    }
}

internal actual object VaultAead {

    // crypto_secretbox_NONCEBYTES; key is 32 (crypto_secretbox_KEYBYTES == KEY_SIZE).
    actual val nonceSize: Int = 24

    actual fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) { "key must be $KEY_SIZE bytes, got ${key.size}" }
        require(nonce.size == nonceSize) { "nonce must be $nonceSize bytes, got ${nonce.size}" }
        ensureSodium()
        return SecretBox.easy(
            message = plaintext.toUByteArray(),
            nonce = nonce.toUByteArray(),
            key = key.toUByteArray(),
        ).toByteArray()
    }

    actual fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray? {
        require(key.size == KEY_SIZE) { "key must be $KEY_SIZE bytes, got ${key.size}" }
        require(nonce.size == nonceSize) { "nonce must be $nonceSize bytes, got ${nonce.size}" }
        ensureSodium()
        return try {
            SecretBox.openEasy(
                ciphertext = ciphertext.toUByteArray(),
                nonce = nonce.toUByteArray(),
                key = key.toUByteArray(),
            ).toByteArray()
        } catch (_: Exception) {
            // SecretBoxCorruptedOrTamperedDataExceptionOrInvalidKey on wrong key / tampered data.
            null
        }
    }
}
