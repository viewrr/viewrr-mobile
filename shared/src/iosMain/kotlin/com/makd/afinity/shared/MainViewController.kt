package com.makd.afinity.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.makd.afinity.shared.ui.App
import com.makd.afinity.shared.ui.detail.detailModule
import com.makd.afinity.shared.ui.history.historyModule
import com.makd.afinity.shared.ui.library.libraryModule
import com.makd.afinity.shared.ui.player.playerModule
import com.makd.afinity.shared.player.iosPlayerModule
import com.makd.afinity.shared.ui.search.searchModule
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
        // iOS simulator shares the host network; localhost reaches the dev Hub.
        modules(
            viewrrModule(baseUrl = "http://localhost:8080"),
            searchModule,
            detailModule,
            libraryModule,
            playerModule,
            settingsModule,
            historyModule,
            iosPlayerModule,
        )
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
