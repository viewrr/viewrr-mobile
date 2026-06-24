package com.makd.afinity.shared.ui.library

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

/** commonMain library browse screen — Shows / Music tabs over the viewrr client. */
@Composable
fun LibraryScreen(viewModel: LibraryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == LibraryTab.SHOWS,
                    onClick = { viewModel.selectTab(LibraryTab.SHOWS) },
                    text = { Text("Shows") },
                )
                Tab(
                    selected = selectedTab == LibraryTab.MUSIC,
                    onClick = { viewModel.selectTab(LibraryTab.MUSIC) },
                    text = { Text("Music") },
                )
            }
            when (val s = state) {
                is LibraryUiState.Loading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is LibraryUiState.Error ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                is LibraryUiState.Content ->
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.items) { item -> PosterCard(item) }
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
