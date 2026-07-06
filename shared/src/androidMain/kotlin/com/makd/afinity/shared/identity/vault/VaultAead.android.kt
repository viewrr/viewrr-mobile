package com.makd.afinity.shared.identity.vault

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android / JVM-host AEAD via AES-256-GCM from the platform JCE provider (#142 rung 2). This is the
 * same code path exercised by :shared:testAndroidHostTest, so the vault round-trip is really
 * AES-GCM under test. GCM is a NIST-standard AEAD; the 16-byte auth tag is appended to the
 * ciphertext by the provider, so a wrong key or tampered blob fails [open] cleanly.
 */
internal actual object VaultAead {

    private const val GCM_TAG_BITS = 128

    actual val nonceSize: Int = 12

    actual fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) { "key must be $KEY_SIZE bytes, got ${key.size}" }
        require(nonce.size == nonceSize) { "nonce must be $nonceSize bytes, got ${nonce.size}" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(plaintext)
    }

    actual fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray? {
        require(key.size == KEY_SIZE) { "key must be $KEY_SIZE bytes, got ${key.size}" }
        require(nonce.size == nonceSize) { "nonce must be $nonceSize bytes, got ${nonce.size}" }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.doFinal(ciphertext)
        } catch (_: GeneralSecurityException) {
            // Bad tag (wrong password / tampered blob) surfaces as AEADBadTagException here.
            null
        }
    }
}
