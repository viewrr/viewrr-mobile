package com.makd.afinity.shared.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.player.PlaybackStatus
import com.makd.afinity.shared.player.Player
import com.makd.afinity.shared.viewrr.PlaybackResolve
import com.makd.afinity.shared.viewrr.ViewrrApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(val resolve: PlaybackResolve) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

/**
 * commonMain player ViewModel — resolves a stream via the viewrr API and drives the
 * platform-agnostic [Player] (#100). Real native playback lands behind the [Player] interface.
 */
class PlayerViewModel(
    private val api: ViewrrApi,
    private val player: Player,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** The platform player instance — passed to VideoSurface to render its output. */
    val boundPlayer: Player get() = player

    /** Live playback status from the underlying [Player]. */
    val playerState: StateFlow<PlaybackStatus> = player.state

    fun start(mediaId: String) {
        _state.value = PlayerUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching { api.playbackResolve(mediaId) }
                    .fold(
                        onSuccess = { resolve ->
                            player.load(resolve.url, resolve.startPositionSecs)
                            player.play()
                            PlayerUiState.Ready(resolve)
                        },
                        onFailure = {
                            // ponytail: no backend /playback/{id} yet — fall back to a dev
                            // sample stream so the player is exercisable. Remove with #101.
                            val resolve = com.makd.afinity.shared.viewrr.SampleData.samplePlayback
                            player.load(resolve.url, resolve.startPositionSecs)
                            player.play()
                            PlayerUiState.Ready(resolve)
                        },
                    )
        }
    }

    fun play() = player.play()

    fun pause() = player.pause()

    fun togglePlayPause() {
        if (player.state.value.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionSecs: Long) = player.seekTo(positionSecs)

    /** Re-read position/duration from the player (UI polls this each second). */
    fun refresh() = player.refreshState()

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
