package com.makd.afinity.shared.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.makd.afinity.shared.viewrr.MediaItem
import org.koin.compose.viewmodel.koinViewModel

/** commonMain search screen — query field over a poster grid of viewrr results. */
@Composable
fun SearchScreen(onItemClick: (String) -> Unit = {}, viewModel: SearchViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            Box(Modifier.fillMaxSize()) {
                when (val s = state) {
                    is SearchUiState.Idle ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Search viewrr", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    is SearchUiState.Loading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    is SearchUiState.Error ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                        }
                    is SearchUiState.Content ->
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(120.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(s.results) { item ->
                                com.makd.afinity.shared.ui.components.MediaCard(
                                    item, onItemClick, androidx.compose.ui.Modifier.fillMaxWidth(),
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem) {
    Column(Modifier.fillMaxWidth()) {
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
