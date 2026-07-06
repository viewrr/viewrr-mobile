package com.makd.afinity.shared.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
actual fun VideoSurface(player: Player, modifier: Modifier) {
    val media3 = player as? Media3Player ?: return
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = media3.exo
            }
        },
    )
}
