package com.makd.afinity.shared.identity.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android [VaultStorage] backed by the AndroidKeyStore (#142 rung 2). Defense-in-depth over the
 * already-password-encrypted [VaultBlob]: the blob is wrapped a second time under a hardware-backed
 * (TEE/StrongBox where available), non-exportable AES-256-GCM key held in the AndroidKeyStore, and
 * the wrapped bytes are kept in a private SharedPreferences file. The KEK derived from the master
 * password never enters the Keystore; these are two independent layers.
 *
 * The wrapped record is `base64( nonce(12) || gcmCiphertext )`.
 */
class AndroidKeystoreVaultStorage(
    context: Context,
    private val prefsName: String = DEFAULT_PREFS,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val recordKey: String = DEFAULT_RECORD_KEY,
) : VaultStorage {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun read(): ByteArray? {
        val stored = prefs.getString(recordKey, null) ?: return null
        val wrapped = Base64.decode(stored, Base64.NO_WRAP)
        require(wrapped.size > GCM_NONCE) { "corrupt keystore record" }
        val nonce = wrapped.copyOfRange(0, GCM_NONCE)
        val ciphertext = wrapped.copyOfRange(GCM_NONCE, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(ciphertext)
    }

    override fun write(blob: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val nonce = cipher.iv
        val ciphertext = cipher.doFinal(blob)
        val record = ByteArray(nonce.size + ciphertext.size)
        nonce.copyInto(record)
        ciphertext.copyInto(record, nonce.size)
        prefs.edit().putString(recordKey, Base64.encodeToString(record, Base64.NO_WRAP)).apply()
    }

    override fun exists(): Boolean = prefs.contains(recordKey)

    override fun clear() {
        prefs.edit().remove(recordKey).apply()
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_NONCE = 12
        private const val GCM_TAG_BITS = 128
        const val DEFAULT_PREFS = "viewrr_identity_vault"
        const val DEFAULT_KEY_ALIAS = "viewrr_vault_wrapping_key"
        const val DEFAULT_RECORD_KEY = "vault_blob"
    }
}
