package com.makd.afinity.shared.viewrr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current auth token + login state. Singleton in Koin; [ViewrrClient]'s
 * tokenProvider reads [token] on each request so login takes effect immediately.
 *
 * ponytail: in-memory only — token is lost on app restart. Add persistence
 * (multiplatform-settings / DataStore) when Keycloak lands (#112-115).
 */
class SessionStore {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    var token: String? = null
        private set

    fun setToken(value: String?) {
        token = value
        _isLoggedIn.value = !value.isNullOrBlank()
    }

    fun clear() = setToken(null)
}
