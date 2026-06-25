package com.makd.afinity.shared.ui.history

import com.makd.afinity.shared.viewrr.FakeViewrrApi
import com.makd.afinity.shared.viewrr.WatchEvent
import com.makd.afinity.shared.viewrr.WatchEventType
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

class HistoryViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initLoad_success_emitsContent() = runTest {
        val fake = FakeViewrrApi(
            watchEvents = listOf(
                WatchEvent(
                    mediaId = "m1",
                    positionSecs = 42,
                    eventType = WatchEventType.PROGRESS,
                    sessionId = "s1",
                ),
            ),
        )
        val vm = HistoryViewModel(fake)

        advanceUntilIdle()

        val state = vm.state.value
        assertIs<HistoryUiState.Content>(state)
        assertEquals("m1", state.events.single().mediaId)
    }

    @Test
    fun initLoad_failure_emitsError() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val vm = HistoryViewModel(fake)

        advanceUntilIdle()

        val state = vm.state.value
        assertIs<HistoryUiState.Error>(state)
        assertEquals("boom", state.message)
    }
}
