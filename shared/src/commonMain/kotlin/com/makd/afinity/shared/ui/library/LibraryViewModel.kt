package com.makd.afinity.shared.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.MediaItem
import com.makd.afinity.shared.viewrr.ViewrrApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryTab { SHOWS, MUSIC }

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(val items: List<MediaItem>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

/** commonMain ViewModel — drives the library browse (Shows / Music) from the viewrr data layer. */
class LibraryViewModel(private val api: ViewrrApi) : ViewModel() {

    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(LibraryTab.SHOWS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    init {
        load()
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
        load()
    }

    private fun load() {
        _state.value = LibraryUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching {
                    if (_selectedTab.value == LibraryTab.SHOWS) api.shows() else api.musicAlbums()
                }
                    .fold(
                        onSuccess = { LibraryUiState.Content(it) },
                        onFailure = { LibraryUiState.Error(it.message ?: "Failed to load") },
                    )
        }
    }
}
