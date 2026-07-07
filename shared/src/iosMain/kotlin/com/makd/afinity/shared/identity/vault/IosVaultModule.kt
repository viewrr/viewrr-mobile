package com.makd.afinity.shared.identity.vault

import org.koin.dsl.module

/**
 * Binds the Keychain-backed [VaultStorage] for the identity vault (#142 rung 3 wiring).
 * Loaded at startKoin in MainViewController, alongside iosPlayerModule.
 */
val iosVaultModule = module {
    single<VaultStorage> { KeychainVaultStorage() }
}
