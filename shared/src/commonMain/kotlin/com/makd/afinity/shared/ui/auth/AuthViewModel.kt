package com.makd.afinity.shared.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.viewrr.SessionStore
import com.makd.afinity.shared.viewrr.ViewrrApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/** Legacy username/password login against POST /auth/login (#101; Keycloak swap later). */
class AuthViewModel(
    private val api: ViewrrApi,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** ponytail: dev bypass for offline UI work (no Hub). Drop when Keycloak lands. */
    fun continueOffline() = session.setToken("offline-dev")

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("Enter username and password")
            return
        }
        _state.value = LoginUiState.Loading
        viewModelScope.launch {
            runCatching { api.login(username.trim(), password) }
                .onSuccess { session.setToken(it.token) } // SessionStore flips App() to the nav shell
                .onFailure { _state.value = LoginUiState.Error(it.message ?: "Login failed") }
        }
    }
}
