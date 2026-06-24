package com.makd.afinity.shared.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.HomeRow
import com.makd.afinity.shared.viewrr.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val rows: List<HomeRow>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/** commonMain ViewModel — drives the Apple-TV home from the viewrr data layer (#101). */
class HomeViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            _state.value =
                runCatching { repository.homeRows() }
                    .fold(
                        onSuccess = { HomeUiState.Content(it) },
                        onFailure = { HomeUiState.Error(it.message ?: "Failed to load") },
                    )
        }
    }
}
