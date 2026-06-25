package com.makd.afinity.shared.ui.settings

import androidx.lifecycle.ViewModel
import com.makd.afinity.shared.viewrr.SessionStore
import kotlinx.coroutines.flow.StateFlow

/** Settings screen state + actions. Reads login state from [SessionStore]; logout flips App() back to login. */
class SettingsViewModel(
    private val session: SessionStore,
) : ViewModel() {

    val loggedIn: StateFlow<Boolean> = session.isLoggedIn

    /** Clears the token; SessionStore flips App() back to the login screen. */
    fun logout() = session.clear()
}
