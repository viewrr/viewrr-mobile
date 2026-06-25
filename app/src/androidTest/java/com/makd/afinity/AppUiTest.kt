package com.makd.afinity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.makd.afinity.shared.viewrr.SessionStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Instrumented UI test — runs on a real Android emulator/device against the live
 * commonMain Compose app (MainActivity -> App()). Verifies the login gate and that the
 * offline-dev path reaches the bottom-nav shell. Run: ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AppUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    // SessionStore is a Koin singleton shared across tests in the process — reset to
    // logged-out so each test starts from the login gate.
    @Before
    fun resetSession() {
        GlobalContext.get().get<SessionStore>().clear()
        rule.waitForIdle()
    }

    @Test
    fun loginScreenIsShownFirst() {
        rule.onNodeWithText("viewrr").assertIsDisplayed()
        rule.onNodeWithText("Sign in").assertIsDisplayed()
    }

    @Test
    fun offlineSkipReachesNavShell() {
        rule.onNodeWithText("Continue offline (dev)").performClick()
        rule.onNodeWithText("Home").assertIsDisplayed()
        rule.onNodeWithText("Library").assertIsDisplayed()
        rule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
