package com.makd.afinity.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.data.repository.AppDataRepository
import com.makd.afinity.data.websocket.JellyfinWebSocketManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val appDataRepository: AppDataRepository,
    private val webSocketManager: JellyfinWebSocketManager,
) : ViewModel() {

    val webSocketState = webSocketManager.connectionState

    val uiState: StateFlow<MainUiState> =
        combine(appDataRepository.userName, appDataRepository.userProfileImageUrl) { name, imageUrl
                ->
                MainUiState(userName = name, userProfileImageUrl = imageUrl)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MainUiState(),
            )
}
