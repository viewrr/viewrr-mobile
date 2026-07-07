package com.makd.afinity.shared.payments

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local opt-in flag for the payments feature (p2p-0020 / #3), persisted via
 * multiplatform-settings (SharedPreferences on Android, NSUserDefaults on iOS) so the
 * choice survives restart — mirrors [com.makd.afinity.shared.viewrr.SessionStore].
 *
 * Default is OFF: account creation and normal app use never touch a wallet unless the
 * user explicitly flips this. This is a display preference only — it never itself holds
 * key material.
 */
class PaymentsPreferences(private val settings: Settings) {

    private val _enabled = MutableStateFlow(settings.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        settings.putBoolean(KEY_ENABLED, value)
        _enabled.value = value
    }

    private companion object {
        const val KEY_ENABLED = "viewrr.payments.enabled"
    }
}
