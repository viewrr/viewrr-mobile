package com.makd.afinity.shared.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView

/** A UIView that hosts an AVPlayerLayer and keeps it sized to its bounds. */
@OptIn(ExperimentalForeignApi::class)
private class PlayerUIView(player: platform.AVFoundation.AVPlayer) : UIView(frame = CGRectZero.readValue()) {
    val playerLayer = AVPlayerLayer().apply {
        this.player = player
        videoGravity = AVLayerVideoGravityResizeAspect
    }

    init {
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.setFrame(bounds)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoSurface(player: Player, modifier: Modifier) {
    val av = player as? AvPlayer ?: return
    UIKitView(
        modifier = modifier,
        factory = { PlayerUIView(av.avPlayer) },
    )
}
