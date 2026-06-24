package com.makd.afinity.shared.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.MediaItem
import com.makd.afinity.shared.viewrr.ViewrrApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Content(val results: List<MediaItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

/** commonMain ViewModel — drives viewrr search from the viewrr data layer. */
class SearchViewModel(private val api: ViewrrApi) : ViewModel() {

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _state.value = SearchUiState.Idle
            return
        }
        _state.value = SearchUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching { api.search(q) }
                    .fold(
                        onSuccess = { SearchUiState.Content(it) },
                        onFailure = { SearchUiState.Error(it.message ?: "Search failed") },
                    )
        }
    }
}
