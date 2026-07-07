package com.makd.afinity.shared.ui.onboarding

import com.makd.afinity.shared.identity.vault.IdentityVault
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Self-custody onboarding DI (#142 rung 3). Provides the [IdentityVault] (over the platform-supplied
 * [com.makd.afinity.shared.identity.vault.VaultStorage]) and the [OnboardingViewModel].
 *
 * Explicit lambda (not viewModelOf) so the ViewModel's `random` / `cryptoDispatcher` params keep
 * their defaults; the constructor DSL would otherwise try to resolve them from the graph.
 */
val onboardingModule = module {
    single { IdentityVault(get()) }
    viewModel { OnboardingViewModel(get()) }
}
