package com.makd.afinity.shared.ui.player

import com.makd.afinity.shared.player.Player
import com.makd.afinity.shared.player.StubPlayer
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin wiring for the player layer (commonMain). Binds the [StubPlayer] no-op backend
 * until the native players land (#100); androidMain/iosMain can override the [Player]
 * factory with their real mpv / AVPlayer implementations.
 */
val playerModule = module {
    factory<Player> { StubPlayer() }
    viewModelOf(::PlayerViewModel)
}
