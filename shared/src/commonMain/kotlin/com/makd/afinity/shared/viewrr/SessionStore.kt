package com.makd.afinity.shared.viewrr

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current auth token + login state, persisted via multiplatform-settings
 * (SharedPreferences on Android, NSUserDefaults on iOS) so login survives app restart.
 * [ViewrrClient]'s tokenProvider reads [token] on each request.
 */
class SessionStore(private val settings: Settings) {

    var token: String? = settings.getStringOrNull(KEY_TOKEN)
        private set

    private val _isLoggedIn = MutableStateFlow(!token.isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun setToken(value: String?) {
        token = value
        if (value.isNullOrBlank()) settings.remove(KEY_TOKEN) else settings.putString(KEY_TOKEN, value)
        _isLoggedIn.value = !value.isNullOrBlank()
    }

    fun clear() = setToken(null)

    private companion object {
        const val KEY_TOKEN = "viewrr.auth.token"
    }
}
