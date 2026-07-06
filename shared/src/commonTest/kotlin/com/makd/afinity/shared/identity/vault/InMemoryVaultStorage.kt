package com.makd.afinity.shared.identity.vault

/**
 * Host-test [VaultStorage] stub. Keeps the encrypted blob in memory so [IdentityVault]'s real
 * KDF + AEAD orchestration is exercised on the JVM host without a device Keystore/Keychain.
 * [lastWritten] lets tests assert on the exact bytes that were persisted.
 */
class InMemoryVaultStorage : VaultStorage {
    var lastWritten: ByteArray? = null
        private set

    override fun read(): ByteArray? = lastWritten?.copyOf()

    override fun write(blob: ByteArray) {
        lastWritten = blob.copyOf()
    }

    override fun exists(): Boolean = lastWritten != null

    override fun clear() {
        lastWritten = null
    }
}
