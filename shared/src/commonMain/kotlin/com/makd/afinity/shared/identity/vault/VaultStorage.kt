package com.makd.afinity.shared.identity.vault

/**
 * Platform secure storage for the (already password-encrypted) identity vault blob (#142 rung 2).
 *
 * The blob handed to [write] is ALREADY encrypted with a master-password-derived key (see
 * [IdentityVault]); the plaintext seed never reaches this layer. Platform implementations add
 * defense-in-depth by keeping the blob behind hardware-backed key material:
 *   - Android: an AndroidKeyStore-wrapped AES key over SharedPreferences (AndroidKeystoreVaultStorage).
 *   - iOS: the Keychain (KeychainVaultStorage).
 *
 * It is a plain interface (not expect/actual) so [IdentityVault]'s crypto orchestration stays in
 * commonMain and is host-testable with an in-memory implementation; the real Keystore/Keychain
 * classes are constructed by each platform app and injected.
 */
interface VaultStorage {
    /** Returns the stored vault blob, or null if none has been written. */
    fun read(): ByteArray?

    /** Persists (overwriting any prior) the encrypted vault blob. */
    fun write(blob: ByteArray)

    /** True iff a vault blob is currently stored. */
    fun exists(): Boolean

    /** Removes any stored vault blob. Safe to call when none exists. */
    fun clear()
}
