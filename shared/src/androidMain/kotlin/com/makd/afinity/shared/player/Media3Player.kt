package com.makd.afinity.shared.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as Media3
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Android [Player] backed by media3 ExoPlayer. The PlayerView in VideoSurface binds [exo]. */
class Media3Player(context: Context) : Player {

    val exo: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _state = MutableStateFlow(PlaybackStatus())
    override val state: StateFlow<PlaybackStatus> = _state.asStateFlow()

    init {
        exo.addListener(object : Media3.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = sync()
            override fun onPlaybackStateChanged(playbackState: Int) = sync()
        })
    }

    private fun sync() {
        _state.value = PlaybackStatus(
            isPlaying = exo.isPlaying,
            positionSecs = exo.currentPosition / 1000,
            durationSecs = (exo.duration.takeIf { it > 0 } ?: 0L) / 1000,
            isBuffering = exo.playbackState == Media3.STATE_BUFFERING,
        )
    }

    override fun load(url: String, startPositionSecs: Long) {
        exo.setMediaItem(MediaItem.fromUri(url))
        if (startPositionSecs > 0) exo.seekTo(startPositionSecs * 1000)
        exo.prepare()
    }

    override fun play() = exo.play()
    override fun pause() = exo.pause()
    override fun seekTo(positionSecs: Long) = exo.seekTo(positionSecs * 1000)
    override fun release() = exo.release()
}
