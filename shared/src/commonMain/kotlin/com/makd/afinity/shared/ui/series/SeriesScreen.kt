package com.makd.afinity.shared.ui.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.makd.afinity.shared.ui.components.MediaCard
import com.makd.afinity.shared.viewrr.Season
import com.makd.afinity.shared.viewrr.Series
import org.koin.compose.viewmodel.koinViewModel

/** commonMain series (show) detail screen — pure Compose Material3 over the viewrr client. */
@Composable
fun SeriesScreen(
    showTitle: String,
    onBack: () -> Unit = {},
    onEpisodeClick: (String) -> Unit = {},
    viewModel: SeriesViewModel = koinViewModel(),
) {
    LaunchedEffect(showTitle) { viewModel.load(showTitle) }
    val state by viewModel.state.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is SeriesUiState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is SeriesUiState.Error ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onBack) { Text("Back") }
                    }
                }
            is SeriesUiState.Content -> SeriesContent(s.series, onEpisodeClick)
        }
    }
}

@Composable
private fun SeriesContent(series: Series, onEpisodeClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { SeriesHeader(series) }
        items(series.seasons) { season -> SeasonRow(season, onEpisodeClick) }
    }
}

@Composable
private fun SeriesHeader(series: Series) {
    Column {
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
                text = series.title ?: series.showTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            series.year?.let { year ->
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            series.overview?.let { overview ->
                Text(text = overview, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SeasonRow(season: Season, onEpisodeClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Season ${season.season}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(season.episodes) { episode -> MediaCard(episode, onEpisodeClick) }
        }
    }
}
