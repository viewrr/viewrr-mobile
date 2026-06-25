package com.makd.afinity.shared.player

import org.koin.dsl.module

/** Provides the AVPlayer-backed [Player] on iOS. Loaded at startKoin in MainViewController. */
val iosPlayerModule = module {
    factory<Player> { AvPlayer() }
}
