package com.makd.afinity.shared.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makd.afinity.shared.identity.ViewrrIdentity
import com.makd.afinity.shared.identity.vault.IdentityVault
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** The onboarding flow's linear state machine: generate -> display -> confirm -> set password -> done. */
sealed interface OnboardingStep {
    /** The freshly generated 12-word mnemonic, shown once for the user to write down. */
    data class DisplayMnemonic(val words: List<String>) : OnboardingStep

    /** Challenge the user to re-enter the words at these 1-based [positions] to prove they saved it. */
    data class Confirm(val positions: List<Int>) : OnboardingStep

    /** Collect the local master password that encrypts the seed at rest. */
    data object SetPassword : OnboardingStep

    /** Vault now holds the encrypted identity; [handle] is the derived display handle. */
    data class Done(val handle: String) : OnboardingStep
}

data class OnboardingUiState(
    val step: OnboardingStep,
    val error: String? = null,
    val busy: Boolean = false,
)

/**
 * Drives self-custody identity onboarding (#142 rung 3): generate a BIP39 mnemonic, show it once,
 * confirm the user backed it up, then encrypt the seed behind a master password via [IdentityVault].
 *
 * The mnemonic is held in memory ONLY — never logged and never persisted except through the vault
 * at the final [setPassword] step (which stores the AEAD-encrypted seed, not the mnemonic). Reuses
 * the rung 1 crypto core and rung 2 vault untouched.
 */
class OnboardingViewModel(
    private val vault: IdentityVault,
    private val random: Random = Random.Default,
    // PBKDF2 (210k iterations) is real CPU work — kept off the main thread. Injected for host tests.
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    // In-memory secret. Wiped best-effort once the vault holds the encrypted seed.
    private var mnemonic: String = ViewrrIdentity.generateMnemonic()
    private val words: List<String> get() = mnemonic.split(" ")

    private val _state = MutableStateFlow(OnboardingUiState(OnboardingStep.DisplayMnemonic(words)))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** DisplayMnemonic -> Confirm: pick [CONFIRM_WORD_COUNT] distinct 1-based positions to challenge. */
    fun confirmWritten() {
        _state.value = OnboardingUiState(OnboardingStep.Confirm(pickPositions()))
    }

    /** Confirm -> DisplayMnemonic: let the user re-read the words before retrying. */
    fun backToMnemonic() {
        _state.value = OnboardingUiState(OnboardingStep.DisplayMnemonic(words))
    }

    /**
     * Validates the supplied [answers] against the mnemonic words at the challenged positions.
     * Correct -> advance to [OnboardingStep.SetPassword]; wrong -> stay on Confirm with an error.
     */
    fun submitConfirmation(answers: List<String>) {
        val step = _state.value.step as? OnboardingStep.Confirm ?: return
        val correct = step.positions.size == answers.size &&
            step.positions.zip(answers).all { (position, answer) ->
                answer.trim().equals(words[position - 1], ignoreCase = true)
            }
        _state.value = if (correct) {
            OnboardingUiState(OnboardingStep.SetPassword)
        } else {
            OnboardingUiState(step, error = "Those words don't match your backup. Try again.")
        }
    }

    /**
     * Validates the master password, then encrypts the seed and persists it via the vault.
     * On success the derived handle is surfaced in [OnboardingStep.Done].
     */
    fun setPassword(password: String, confirm: String) {
        val step = _state.value.step
        if (step !is OnboardingStep.SetPassword) return

        passwordError(password, confirm)?.let {
            _state.value = OnboardingUiState(step, error = it)
            return
        }

        _state.value = OnboardingUiState(step, busy = true)
        viewModelScope.launch {
            val handle = withContext(cryptoDispatcher) {
                // seed = first 32 bytes of the BIP39-512 seed (the frozen reduction the vault stores).
                val seed = ViewrrIdentity.mnemonicToSeed(mnemonic).copyOf(32)
                vault.store(seed, password)
                ViewrrIdentity.keyPairFromSeed(seed).handle
            }
            mnemonic = "" // best-effort wipe: the vault is now the sole holder of the secret.
            _state.value = OnboardingUiState(OnboardingStep.Done(handle))
        }
    }

    private fun passwordError(password: String, confirm: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH ->
            "Master password must be at least $MIN_PASSWORD_LENGTH characters."
        password != confirm -> "Passwords don't match."
        else -> null
    }

    /** [CONFIRM_WORD_COUNT] distinct 1-based positions across the mnemonic, in ascending order. */
    private fun pickPositions(): List<Int> =
        (1..words.size).shuffled(random).take(minOf(CONFIRM_WORD_COUNT, words.size)).sorted()

    companion object {
        const val CONFIRM_WORD_COUNT = 3
        const val MIN_PASSWORD_LENGTH = 8
    }
}
