package com.makd.afinity.shared.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform video surface bound to a [Player]. androidMain renders a media3 PlayerView;
 * iosMain renders an AVPlayerLayer. The [player] must be the platform impl created by the
 * platform Koin module (cast inside the actual). #100.
 */
@Composable
expect fun VideoSurface(player: Player, modifier: Modifier)
