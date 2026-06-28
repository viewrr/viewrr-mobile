package com.makd.afinity.shared.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL

/** iOS [Player] backed by AVPlayer. The AVPlayerLayer in VideoSurface binds [avPlayer]. */
@OptIn(ExperimentalForeignApi::class)
class AvPlayer : Player {

    val avPlayer: AVPlayer = AVPlayer()

    private val _state = MutableStateFlow(PlaybackStatus())
    override val state: StateFlow<PlaybackStatus> = _state.asStateFlow()

    override fun load(url: String, startPositionSecs: Long) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        avPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = nsUrl))
        if (startPositionSecs > 0) {
            avPlayer.seekToTime(CMTimeMakeWithSeconds(startPositionSecs.toDouble(), 1))
        }
    }

    override fun play() {
        avPlayer.play()
        _state.value = _state.value.copy(isPlaying = true)
    }

    override fun pause() {
        avPlayer.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun seekTo(positionSecs: Long) {
        avPlayer.seekToTime(CMTimeMakeWithSeconds(positionSecs.toDouble(), 1))
        _state.value = _state.value.copy(positionSecs = positionSecs)
    }

    override fun release() {
        avPlayer.pause()
        avPlayer.replaceCurrentItemWithPlayerItem(null)
        _state.value = PlaybackStatus()
    }

    override fun refreshState() {
        val pos = CMTimeGetSeconds(avPlayer.currentTime()).toLong().coerceAtLeast(0)
        val durSecs = avPlayer.currentItem?.duration?.let { CMTimeGetSeconds(it) }
        val dur = if (durSecs != null && !durSecs.isNaN() && durSecs > 0) durSecs.toLong() else 0
        _state.value = _state.value.copy(positionSecs = pos, durationSecs = dur)
    }
}
