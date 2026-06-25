package com.makd.afinity.shared.player

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Provides the media3-backed [Player] on Android. Loaded at startKoin in AfinityApplication. */
val androidPlayerModule = module {
    factory<Player> { Media3Player(androidContext()) }
}
