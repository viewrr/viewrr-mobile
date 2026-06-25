package com.makd.afinity.shared.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makd.afinity.shared.viewrr.WatchEvent
import org.koin.compose.viewmodel.koinViewModel

/** commonMain watch-history screen — lists the current user's reported watch events. */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is HistoryUiState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is HistoryUiState.Error ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            is HistoryUiState.Content ->
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item { HistoryHeader() }
                    items(s.events) { event -> HistoryRow(event) }
                }
        }
    }
}

@Composable
private fun HistoryHeader() {
    Text(
        text = "Watch history",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
    )
}

@Composable
private fun HistoryRow(event: WatchEvent) {
    ListItem(
        headlineContent = { Text(event.mediaId) },
        supportingContent = {
            Text("${event.eventType.name.lowercase()} · ${formatPosition(event.positionSecs)}")
        },
    )
}

/** Formats a position in seconds as "Mm Ss" (e.g. 125 -> "2m 5s"). */
private fun formatPosition(positionSecs: Long): String {
    val minutes = positionSecs / 60
    val seconds = positionSecs % 60
    return "${minutes}m ${seconds}s"
}
