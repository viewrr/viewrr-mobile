package com.makd.afinity.shared.ui.onboarding

import com.makd.afinity.shared.identity.ViewrrIdentity
import com.makd.afinity.shared.identity.vault.IdentityVault
import com.makd.afinity.shared.identity.vault.InMemoryVaultStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Rung 3 onboarding flow-machine tests (#142). Runs on the JVM host via :shared:testAndroidHostTest
 * against the real rung 1 crypto core + rung 2 vault (over [InMemoryVaultStorage]) — no mocks, so the
 * mnemonic generation, confirmation, KDF/AEAD store, and unlock round-trip are all genuinely exercised.
 */
class OnboardingViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setup() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(vault: IdentityVault) =
        // cryptoDispatcher = the test dispatcher so advanceUntilIdle drives the store coroutine.
        OnboardingViewModel(vault, cryptoDispatcher = dispatcher)

    @Test
    fun starts_by_generating_a_twelve_word_mnemonic_held_in_memory() {
        val vm = newViewModel(IdentityVault(InMemoryVaultStorage()))

        val step = vm.state.value.step
        assertIs<OnboardingStep.DisplayMnemonic>(step)
        assertEquals(12, step.words.size, "a fresh 12-word BIP39 mnemonic must be generated")
        assertTrue(
            ViewrrIdentity.validateMnemonic(step.words.joinToString(" ")),
            "the generated mnemonic must be a valid BIP39 phrase",
        )
    }

    @Test
    fun confirm_challenges_three_distinct_in_range_positions() {
        val vm = newViewModel(IdentityVault(InMemoryVaultStorage()))

        vm.confirmWritten()

        val step = vm.state.value.step
        assertIs<OnboardingStep.Confirm>(step)
        assertEquals(3, step.positions.size)
        assertEquals(3, step.positions.toSet().size, "challenged positions must be distinct")
        assertTrue(step.positions.all { it in 1..12 }, "positions must be 1-based and in range")
    }

    @Test
    fun confirm_accepts_the_correct_words_and_advances_to_set_password() {
        val vm = newViewModel(IdentityVault(InMemoryVaultStorage()))
        val words = (vm.state.value.step as OnboardingStep.DisplayMnemonic).words

        vm.confirmWritten()
        val positions = (vm.state.value.step as OnboardingStep.Confirm).positions
        vm.submitConfirmation(positions.map { words[it - 1] })

        assertIs<OnboardingStep.SetPassword>(vm.state.value.step)
        assertNull(vm.state.value.error)
    }

    @Test
    fun confirm_rejects_wrong_words_and_stays_on_confirm() {
        val vm = newViewModel(IdentityVault(InMemoryVaultStorage()))
        vm.confirmWritten()
        val positions = (vm.state.value.step as OnboardingStep.Confirm).positions

        vm.submitConfirmation(positions.map { "wrong" })

        val step = vm.state.value.step
        assertIs<OnboardingStep.Confirm>(step)
        assertEquals(positions, step.positions, "a wrong answer must not change the challenge")
        assertNotNull(vm.state.value.error, "a wrong answer must surface an error")
    }

    @Test
    fun set_password_rejects_short_password() = runTest {
        val vm = advanceToSetPassword(IdentityVault(InMemoryVaultStorage()))

        vm.setPassword("short", "short")
        advanceUntilIdle()

        assertIs<OnboardingStep.SetPassword>(vm.state.value.step)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun set_password_rejects_mismatched_confirmation() = runTest {
        val vm = advanceToSetPassword(IdentityVault(InMemoryVaultStorage()))

        vm.setPassword("battery-staple", "battery-stapleX")
        advanceUntilIdle()

        assertIs<OnboardingStep.SetPassword>(vm.state.value.step)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun set_password_stores_to_vault_and_unlock_round_trips_with_matching_handle() = runTest {
        val storage = InMemoryVaultStorage()
        val vault = IdentityVault(storage)
        val vm = advanceToSetPassword(vault)
        val password = "correct horse battery staple"

        vm.setPassword(password, password)
        advanceUntilIdle()

        val done = vm.state.value.step
        assertIs<OnboardingStep.Done>(done)
        assertTrue(vault.exists(), "a vault blob must be persisted")

        val unlocked = vault.unlock(password)
        assertNotNull(unlocked, "unlock with the master password must recover the identity")
        assertEquals(unlocked.handle, done.handle, "surfaced handle must match the stored identity")
        assertEquals(
            ViewrrIdentity.deriveHandle(unlocked.publicKey),
            done.handle,
            "surfaced handle must equal deriveHandle(publicKey)",
        )
    }

    @Test
    fun wrong_master_password_does_not_unlock_the_created_vault() = runTest {
        val vault = IdentityVault(InMemoryVaultStorage())
        val vm = advanceToSetPassword(vault)

        vm.setPassword("the-real-password", "the-real-password")
        advanceUntilIdle()

        assertNull(vault.unlock("some-other-password"))
    }

    /** Drives a fresh ViewModel through display -> confirm(correct) so it sits on SetPassword. */
    private fun advanceToSetPassword(vault: IdentityVault): OnboardingViewModel {
        val vm = newViewModel(vault)
        val words = (vm.state.value.step as OnboardingStep.DisplayMnemonic).words
        vm.confirmWritten()
        val positions = (vm.state.value.step as OnboardingStep.Confirm).positions
        vm.submitConfirmation(positions.map { words[it - 1] })
        return vm
    }
}
