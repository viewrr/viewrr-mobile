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

    /** Long-lived token used to mint a fresh access token via POST /auth/refresh. */
    var refreshToken: String? = settings.getStringOrNull(KEY_REFRESH)
        private set

    private val _isLoggedIn = MutableStateFlow(!token.isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** Persist both tokens from an [AuthTokens] response (login/register/refresh). */
    fun setTokens(access: String?, refresh: String?) {
        token = access
        refreshToken = refresh
        if (access.isNullOrBlank()) settings.remove(KEY_TOKEN) else settings.putString(KEY_TOKEN, access)
        if (refresh.isNullOrBlank()) settings.remove(KEY_REFRESH) else settings.putString(KEY_REFRESH, refresh)
        _isLoggedIn.value = !access.isNullOrBlank()
    }

    /** Set only the access token (offline dev bypass); drops any stored refresh token. */
    fun setToken(value: String?) = setTokens(value, null)

    fun clear() = setTokens(null, null)

    private companion object {
        const val KEY_TOKEN = "viewrr.auth.token"
        const val KEY_REFRESH = "viewrr.auth.refresh"
    }
}
