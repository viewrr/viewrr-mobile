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
                // Persist BOTH tokens so refresh works; SessionStore flips App() to the nav shell.
                .onSuccess { session.setTokens(it.token, it.refreshToken) }
                .onFailure { _state.value = LoginUiState.Error(it.message ?: "Login failed") }
        }
    }

    /** Sign up against POST /auth/register. email is REQUIRED by the backend (400 without it). */
    fun register(username: String, password: String, email: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("Enter username and password")
            return
        }
        if (email.isBlank()) {
            _state.value = LoginUiState.Error("Enter an email address")
            return
        }
        _state.value = LoginUiState.Loading
        viewModelScope.launch {
            runCatching { api.register(username.trim(), password, email.trim()) }
                .onSuccess { session.setTokens(it.token, it.refreshToken) }
                .onFailure { _state.value = LoginUiState.Error(it.message ?: "Registration failed") }
        }
    }
}
