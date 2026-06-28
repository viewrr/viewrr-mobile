package com.makd.afinity.shared.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.Series
import com.makd.afinity.shared.viewrr.ViewrrApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SeriesUiState {
    data object Loading : SeriesUiState
    data class Content(val series: Series) : SeriesUiState
    data class Error(val message: String) : SeriesUiState
}

/** commonMain ViewModel — drives the series (show) detail screen from the viewrr client. */
class SeriesViewModel(private val api: ViewrrApi) : ViewModel() {

    private val _state = MutableStateFlow<SeriesUiState>(SeriesUiState.Loading)
    val state: StateFlow<SeriesUiState> = _state.asStateFlow()

    fun load(showTitle: String) {
        _state.value = SeriesUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching { api.series(showTitle) }
                    .fold(
                        onSuccess = { SeriesUiState.Content(it) },
                        onFailure = { SeriesUiState.Error(it.message ?: "Failed to load") },
                    )
        }
    }
}
