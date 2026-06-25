package com.makd.afinity.shared.ui.player

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin wiring for the player layer (commonMain). The concrete
 * [com.makd.afinity.shared.player.Player] is provided per platform
 * (androidPlayerModule = media3, iosPlayerModule = AVPlayer), both loaded at startKoin.
 * commonTest uses StubPlayer directly. #100.
 */
val playerModule = module {
    viewModelOf(::PlayerViewModel)
}
