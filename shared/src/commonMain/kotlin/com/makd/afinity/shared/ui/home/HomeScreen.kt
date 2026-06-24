package com.makd.afinity.shared.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makd.afinity.shared.viewrr.HomeRow
import com.makd.afinity.shared.viewrr.MediaItem
import org.koin.compose.viewmodel.koinViewModel

/** First commonMain screen — Apple-TV style home, end-to-end over the viewrr client. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is HomeUiState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is HomeUiState.Error ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            is HomeUiState.Content ->
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(s.rows) { row -> HomeRowView(row) }
                }
        }
    }
}

@Composable
private fun HomeRowView(row: HomeRow) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(row.items) { item -> PosterCard(item) }
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem) {
    Column(Modifier.width(120.dp)) {
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                // Poster image wiring (Coil3) lands when the UI layer moves to commonMain.
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        text = item.title.take(2),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }
        Text(
            text = item.cleanTitle ?: item.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
