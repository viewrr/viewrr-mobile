package com.makd.afinity.shared.ui.settings

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin module for the settings feature. Register alongside the other feature modules in app startup. */
val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
