package com.makd.afinity.shared.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Snapshot of player playback state, surfaced to the UI via [Player.state]. */
data class PlaybackStatus(
    val isPlaying: Boolean = false,
    val positionSecs: Long = 0,
    val durationSecs: Long = 0,
    val isBuffering: Boolean = false,
)

/**
 * Platform-agnostic video player contract (commonMain). Real native backends —
 * mpv on androidMain, AVPlayer on iosMain — provide concrete implementations later (#100).
 * Until then [StubPlayer] keeps commonMain compiling and the player UI functional.
 */
interface Player {
    val state: StateFlow<PlaybackStatus>

    fun load(url: String, startPositionSecs: Long = 0)
    fun play()
    fun pause()
    fun seekTo(positionSecs: Long)
    fun release()
}

/**
 * No-op [Player] for commonMain. Performs no real playback — it only mutates the
 * exposed [PlaybackStatus] so the UI behaves correctly before the native players land (#100).
 */
class StubPlayer : Player {

    private val _state = MutableStateFlow(PlaybackStatus())
    override val state: StateFlow<PlaybackStatus> = _state.asStateFlow()

    override fun load(url: String, startPositionSecs: Long) {
        _state.update {
            it.copy(positionSecs = startPositionSecs, isPlaying = false, isBuffering = false)
        }
    }

    override fun play() {
        _state.update { it.copy(isPlaying = true, isBuffering = false) }
    }

    override fun pause() {
        _state.update { it.copy(isPlaying = false) }
    }

    override fun seekTo(positionSecs: Long) {
        _state.update { it.copy(positionSecs = positionSecs) }
    }

    override fun release() {
        _state.update { PlaybackStatus() }
    }
}
