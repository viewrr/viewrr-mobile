package com.makd.afinity.shared.ui.payments

import com.makd.afinity.shared.payments.PaymentsPreferences
import com.makd.afinity.shared.viewrr.FakeViewrrApi
import com.makd.afinity.shared.viewrr.WalletInfo
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PaymentsViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun paymentsOff_neverCallsWalletApi() = runTest {
        val fake = FakeViewrrApi()
        val vm = PaymentsViewModel(fake, PaymentsPreferences(MapSettings()))

        advanceUntilIdle()

        assertEquals(WalletUiState.Hidden, vm.walletState.value)
        assertEquals(0, fake.walletOptInCalls)
        assertEquals(0, fake.walletInfoCalls)
    }

    @Test
    fun enabling_optsInOnceThenDisplaysBalance() = runTest {
        val fake = FakeViewrrApi(
            wallet = WalletInfo(optedIn = false),
        )
        val vm = PaymentsViewModel(fake, PaymentsPreferences(MapSettings()))
        advanceUntilIdle()

        vm.setPaymentsEnabled(true)
        advanceUntilIdle()

        val state = vm.walletState.value
        assertIs<WalletUiState.Loaded>(state)
        assertEquals("0xtest", state.address)
        assertEquals(1, fake.walletOptInCalls)
    }

    @Test
    fun disablingAfterEnabled_hidesWithoutFurtherCalls() = runTest {
        val fake = FakeViewrrApi(wallet = WalletInfo(optedIn = false))
        val vm = PaymentsViewModel(fake, PaymentsPreferences(MapSettings()))
        vm.setPaymentsEnabled(true)
        advanceUntilIdle()
        val callsAfterEnable = fake.walletInfoCalls

        vm.setPaymentsEnabled(false)
        advanceUntilIdle()

        assertEquals(WalletUiState.Hidden, vm.walletState.value)
        assertEquals(callsAfterEnable, fake.walletInfoCalls)
    }
}
