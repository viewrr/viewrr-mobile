package com.makd.afinity.shared.ui.player

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin wiring for the player layer (commonMain). The concrete
 * [com.makd.afinity.shared.player.Player] is provided per platform
 * (androidPlayerModule = media3, iosPlayerModule = AVPlayer), both loaded at startKoin.
 * commonTest uses StubPlayer directly. #100.
 *
 * Explicit lambda (not viewModelOf) so PlayerViewModel's `debug` param uses its default;
 * the constructor DSL would otherwise try to resolve a Boolean from the graph and crash.
 */
val playerModule = module {
    viewModel { PlayerViewModel(get(), get()) }
}
