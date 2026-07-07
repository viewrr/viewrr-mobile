package com.makd.afinity.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.makd.afinity.shared.identity.vault.iosVaultModule
import com.makd.afinity.shared.ui.App
import com.makd.afinity.shared.ui.detail.detailModule
import com.makd.afinity.shared.ui.history.historyModule
import com.makd.afinity.shared.ui.library.libraryModule
import com.makd.afinity.shared.ui.onboarding.onboardingModule
import com.makd.afinity.shared.ui.payments.paymentsModule
import com.makd.afinity.shared.ui.player.playerModule
import com.makd.afinity.shared.player.iosPlayerModule
import com.makd.afinity.shared.ui.search.searchModule
import com.makd.afinity.shared.ui.series.seriesModule
import com.makd.afinity.shared.ui.settings.settingsModule
import com.makd.afinity.shared.viewrr.viewrrModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var koinStarted = false

/** iOS entry — call [initKoin] once from Swift, then present [MainViewController]. */
fun initKoin() {
    if (koinStarted) return
    koinStarted = true
    startKoin {
        // ponytail: live Hub hardcoded; split debug/release base URL when a staging env exists.
        modules(
            viewrrModule(baseUrl = "https://api.viewrr.stream"),
            searchModule,
            detailModule,
            libraryModule,
            playerModule,
            settingsModule,
            historyModule,
            onboardingModule,
            paymentsModule,
            iosPlayerModule,
            iosVaultModule,
            seriesModule,
        )
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
