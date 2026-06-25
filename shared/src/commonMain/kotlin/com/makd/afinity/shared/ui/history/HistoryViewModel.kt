package com.makd.afinity.shared.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.ViewrrApi
import com.makd.afinity.shared.viewrr.WatchEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(val events: List<WatchEvent>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

/** commonMain ViewModel — drives the watch-history screen from the viewrr data layer. */
class HistoryViewModel(private val api: ViewrrApi) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = HistoryUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching { api.watchEventsMe() }
                    .fold(
                        onSuccess = { HistoryUiState.Content(it) },
                        onFailure = { HistoryUiState.Error(it.message ?: "Failed to load") },
                    )
        }
    }
}
