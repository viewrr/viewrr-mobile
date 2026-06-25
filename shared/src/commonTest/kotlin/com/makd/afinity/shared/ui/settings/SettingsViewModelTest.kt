package com.makd.afinity.shared.ui.settings

import com.makd.afinity.shared.viewrr.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reflectsLoggedInState() = runTest {
        val session = SessionStore()
        session.setToken("tok")
        val vm = SettingsViewModel(session)

        assertTrue(vm.loggedIn.value)
    }

    @Test
    fun logout_clearsSession() = runTest {
        val session = SessionStore()
        session.setToken("tok")
        val vm = SettingsViewModel(session)
        assertTrue(vm.loggedIn.value)

        vm.logout()

        assertFalse(session.isLoggedIn.value)
        assertFalse(vm.loggedIn.value)
    }
}
