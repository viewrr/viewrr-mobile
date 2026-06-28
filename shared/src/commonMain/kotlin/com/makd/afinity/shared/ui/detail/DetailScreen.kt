package com.makd.afinity.shared.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makd.afinity.shared.viewrr.MediaItem
import org.koin.compose.viewmodel.koinViewModel

/** commonMain media detail screen — pure Compose Material3 over the viewrr client. */
@Composable
fun DetailScreen(
    mediaId: String,
    onBack: () -> Unit = {},
    onPlay: (String) -> Unit = {},
    onShow: (String) -> Unit = {},
    viewModel: DetailViewModel = koinViewModel(),
) {
    LaunchedEffect(mediaId) { viewModel.load(mediaId) }
    val state by viewModel.state.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is DetailUiState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is DetailUiState.Error ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            is DetailUiState.Content -> DetailContent(s.item, onPlay, onShow)
        }
    }
}

@Composable
private fun DetailContent(item: MediaItem, onPlay: (String) -> Unit, onShow: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {}
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.cleanTitle ?: item.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            val metadata = buildMetadata(item)
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val episodeLine = buildEpisodeLine(item)
            if (episodeLine != null) {
                Text(
                    text = episodeLine,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Button(onClick = { onPlay(item.id) }) { Text("Play") }
            item.showTitle?.let { show ->
                androidx.compose.material3.OutlinedButton(onClick = { onShow(show) }) {
                    Text("View show")
                }
            }
            item.overview?.let { overview ->
                Text(text = overview, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun buildMetadata(item: MediaItem): String =
    listOfNotNull(
        item.year?.toString(),
        item.contentRating,
        item.durationSecs?.let { formatDuration(it) },
    ).joinToString(" · ")

private fun buildEpisodeLine(item: MediaItem): String? {
    val show = item.showTitle ?: return null
    val season = item.season
    val episode = item.episode
    return if (season != null && episode != null) "$show · S$season E$episode" else show
}

private fun formatDuration(durationSecs: Long): String {
    val totalMinutes = durationSecs / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}
