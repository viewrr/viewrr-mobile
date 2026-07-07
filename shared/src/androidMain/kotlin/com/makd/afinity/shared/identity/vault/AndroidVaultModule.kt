package com.makd.afinity.shared.identity.vault

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Binds the AndroidKeyStore-backed [VaultStorage] for the identity vault (#142 rung 3 wiring).
 * Loaded at startKoin in AfinityApplication, alongside androidPlayerModule.
 */
val androidVaultModule = module {
    single<VaultStorage> { AndroidKeystoreVaultStorage(androidContext()) }
}
