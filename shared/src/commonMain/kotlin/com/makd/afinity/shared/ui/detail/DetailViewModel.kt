package com.makd.afinity.shared.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.MediaItem
import com.makd.afinity.shared.viewrr.ViewrrApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val item: MediaItem) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

/** commonMain ViewModel — drives the media detail screen from the viewrr client (#101). */
class DetailViewModel(private val api: ViewrrApi) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun load(id: String) {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching { api.mediaDetail(id) }
                    .fold(
                        onSuccess = { DetailUiState.Content(it) },
                        onFailure = { DetailUiState.Error(it.message ?: "Failed to load") },
                    )
        }
    }
}
