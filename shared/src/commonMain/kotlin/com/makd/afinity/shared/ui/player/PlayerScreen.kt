package com.makd.afinity.shared.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * commonMain player screen — resolves a stream and renders a placeholder surface with
 * controls. The real native video surface (mpv / AVPlayer) replaces the placeholder with #100.
 */
@Composable
fun PlayerScreen(
    mediaId: String,
    onBack: () -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
) {
    LaunchedEffect(mediaId) { viewModel.start(mediaId) }

    val uiState by viewModel.state.collectAsState()
    val playerStatus by viewModel.playerState.collectAsState()

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when (val s = uiState) {
            is PlayerUiState.Loading -> CircularProgressIndicator(color = Color.White)

            is PlayerUiState.Error ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onBack) { Text("Back") }
                }

            is PlayerUiState.Ready -> {
                // Native video surface — media3 PlayerView (Android) / AVPlayerLayer (iOS).
                com.makd.afinity.shared.player.VideoSurface(
                    player = viewModel.boundPlayer,
                    modifier = Modifier.fillMaxSize(),
                )

                // Controls overlay.
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(onClick = onBack) { Text("Back") }

                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Now playing: $mediaId",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = s.resolve.url,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Button(onClick = { viewModel.togglePlayPause() }) {
                        Text(if (playerStatus.isPlaying) "Pause" else "Play")
                    }
                }
            }
        }
    }
}
