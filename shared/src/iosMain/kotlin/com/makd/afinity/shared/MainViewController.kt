package com.makd.afinity.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.makd.afinity.shared.ui.home.HomeScreen
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
        modules(viewrrModule(baseUrl = "http://localhost:8080") { null })
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { HomeScreen() }
