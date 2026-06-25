package com.makd.afinity.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.makd.afinity.shared.ui.auth.LoginScreen
import com.makd.afinity.shared.ui.detail.DetailScreen
import com.makd.afinity.shared.ui.history.HistoryScreen
import com.makd.afinity.shared.ui.home.HomeScreen
import com.makd.afinity.shared.ui.library.LibraryScreen
import com.makd.afinity.shared.ui.player.PlayerScreen
import com.makd.afinity.shared.ui.search.SearchScreen
import com.makd.afinity.shared.ui.settings.SettingsScreen
import com.makd.afinity.shared.ui.theme.ViewrrTheme
import com.makd.afinity.shared.viewrr.SessionStore
import org.koin.compose.koinInject

// ponytail: glyph icons avoid the heavy material-icons-extended dependency in commonMain.
private enum class Tab(val label: String, val glyph: String) {
    HOME("Home", "⌂"),
    SEARCH("Search", "⌕"),
    LIBRARY("Library", "☰"),
    HISTORY("History", "⏱"),
    SETTINGS("Settings", "⚙"),
}

/** Shared CMP app shell — login gate, bottom-nav, detail + player overlays. */
@Composable
fun App() {
    ViewrrTheme {
        val session = koinInject<SessionStore>()
        val loggedIn by session.isLoggedIn.collectAsState()
        if (!loggedIn) {
            LoginScreen()
            return@ViewrrTheme
        }

        var current by remember { mutableStateOf(Tab.HOME) }
        var detailId by remember { mutableStateOf<String?>(null) }
        var playerId by remember { mutableStateOf<String?>(null) }

        // Overlays take priority: player over detail over the tab shell.
        playerId?.let { id ->
            PlayerScreen(mediaId = id, onBack = { playerId = null })
            return@ViewrrTheme
        }
        detailId?.let { id ->
            DetailScreen(
                mediaId = id,
                onBack = { detailId = null },
                onPlay = { playerId = it },
            )
            return@ViewrrTheme
        }

        val openDetail: (String) -> Unit = { detailId = it }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab,
                            onClick = { current = tab },
                            icon = { Text(tab.glyph) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (current) {
                    Tab.HOME -> HomeScreen(onItemClick = openDetail)
                    Tab.SEARCH -> SearchScreen(onItemClick = openDetail)
                    Tab.LIBRARY -> LibraryScreen(onItemClick = openDetail)
                    Tab.HISTORY -> HistoryScreen()
                    Tab.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
