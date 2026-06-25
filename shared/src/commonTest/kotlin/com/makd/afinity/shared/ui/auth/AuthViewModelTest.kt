package com.makd.afinity.shared.ui.auth

import com.makd.afinity.shared.viewrr.AuthTokens
import com.makd.afinity.shared.viewrr.FakeViewrrApi
import com.makd.afinity.shared.viewrr.SessionStore
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_success_setsTokenAndRecordsLogin() = runTest {
        val fake = FakeViewrrApi(loginTokens = AuthTokens(token = "real-token"))
        val session = SessionStore()
        val vm = AuthViewModel(fake, session)

        vm.login("alice", "secret")
        advanceUntilIdle()

        assertTrue(session.isLoggedIn.value)
        assertEquals("real-token", session.token)
        assertEquals("alice" to "secret", fake.lastLogin)
    }

    @Test
    fun login_blankInput_emitsErrorWithoutCallingApi() = runTest {
        val fake = FakeViewrrApi()
        val session = SessionStore()
        val vm = AuthViewModel(fake, session)

        vm.login("", "secret")
        advanceUntilIdle()

        assertIs<LoginUiState.Error>(vm.state.value)
        assertFalse(session.isLoggedIn.value)
        assertEquals(null, fake.lastLogin)
    }

    @Test
    fun login_failure_emitsError() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val session = SessionStore()
        val vm = AuthViewModel(fake, session)

        vm.login("alice", "secret")
        advanceUntilIdle()

        val state = vm.state.value
        assertIs<LoginUiState.Error>(state)
        assertEquals("boom", state.message)
        assertFalse(session.isLoggedIn.value)
    }

    @Test
    fun continueOffline_setsLoggedIn() = runTest {
        val session = SessionStore()
        val vm = AuthViewModel(FakeViewrrApi(), session)

        vm.continueOffline()

        assertTrue(session.isLoggedIn.value)
        assertEquals("offline-dev", session.token)
    }
}
