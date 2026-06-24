package com.makd.afinity.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.makd.afinity.shared.ui.home.HomeScreen
import com.makd.afinity.shared.ui.library.LibraryScreen
import com.makd.afinity.shared.ui.search.SearchScreen

// ponytail: glyph icons avoid the heavy material-icons-extended dependency in commonMain.
private enum class Tab(val label: String, val glyph: String) {
    HOME("Home", "⌂"),
    SEARCH("Search", "⌕"),
    LIBRARY("Library", "☰"),
}

/** Shared CMP app shell — bottom-nav over the viewrr-backed commonMain screens. */
@Composable
fun App() {
    MaterialTheme {
        var current by remember { mutableStateOf(Tab.HOME) }
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
                    Tab.HOME -> HomeScreen()
                    Tab.SEARCH -> SearchScreen()
                    Tab.LIBRARY -> LibraryScreen()
                }
            }
        }
    }
}
