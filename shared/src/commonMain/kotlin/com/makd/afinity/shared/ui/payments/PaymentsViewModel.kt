package com.makd.afinity.shared.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.payments.PaymentsPreferences
import com.makd.afinity.shared.viewrr.ViewrrApi
import com.makd.afinity.shared.viewrr.WalletInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WalletUiState {
    /** Payments are off — the default. No wallet call is ever made in this state. */
    data object Hidden : WalletUiState
    data object Loading : WalletUiState
    data class Loaded(val address: String, val usdcBalance: String) : WalletUiState
    data class Error(val message: String) : WalletUiState
}

/**
 * Drives the payments opt-in toggle + read-only wallet display (p2p-0020 / #3).
 *
 * Hard gate: [walletState] only ever calls the Hub when [paymentsEnabled] is true. Flipping
 * the toggle off immediately hides the wallet section and makes no further calls — the app
 * stays fully functional with payments off (default). This screen is display-only: no
 * send / open-channel / close actions exist here (legal #9 open).
 */
class PaymentsViewModel(
    private val api: ViewrrApi,
    private val prefs: PaymentsPreferences,
) : ViewModel() {

    val paymentsEnabled: StateFlow<Boolean> = prefs.enabled

    private val _walletState = MutableStateFlow<WalletUiState>(WalletUiState.Hidden)
    val walletState: StateFlow<WalletUiState> = _walletState.asStateFlow()

    init {
        if (prefs.enabled.value) refreshWallet()
    }

    /** Flips the opt-in flag. Enabling provisions/fetches the wallet; disabling only hides it — no call. */
    fun setPaymentsEnabled(enabled: Boolean) {
        prefs.setEnabled(enabled)
        if (enabled) refreshWallet() else _walletState.value = WalletUiState.Hidden
    }

    /** Retry after an [WalletUiState.Error]; no-op while payments are off. */
    fun retry() {
        if (prefs.enabled.value) refreshWallet()
    }

    private fun refreshWallet() {
        _walletState.value = WalletUiState.Loading
        viewModelScope.launch {
            _walletState.value = try {
                // GET first (no write); only POST opt-in the one time the Hub reports optedIn=false.
                var info = api.walletInfo()
                if (!info.optedIn) {
                    api.walletOptIn()
                    info = api.walletInfo()
                }
                info.toUiState()
            } catch (e: Exception) {
                WalletUiState.Error(e.message ?: "Couldn't load your wallet.")
            }
        }
    }

    private fun WalletInfo.toUiState(): WalletUiState {
        val walletAddress = address
        if (!optedIn || walletAddress == null) {
            return WalletUiState.Error("Couldn't enable payments.")
        }
        return WalletUiState.Loaded(
            address = walletAddress,
            usdcBalance = formatBaseUnits(balanceBaseUnits ?: "0", decimals ?: DEFAULT_DECIMALS),
        )
    }

    /** Formats a raw base-units integer string (e.g. "1500000") to a 2dp display amount ("1.50"). */
    private fun formatBaseUnits(raw: String, decimals: Int): String {
        val units = raw.toLongOrNull() ?: return "0.00"
        if (decimals <= 0) return units.toString()
        val divisor = pow10(decimals)
        val whole = units / divisor
        val fraction = (units % divisor).toString().padStart(decimals, '0').take(2).padEnd(2, '0')
        return "$whole.$fraction"
    }

    private fun pow10(exponent: Int): Long {
        var result = 1L
        repeat(exponent) { result *= 10 }
        return result
    }

    private companion object {
        const val DEFAULT_DECIMALS = 6
    }
}
