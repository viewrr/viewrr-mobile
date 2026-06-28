package com.makd.afinity.shared.ui.player

import com.makd.afinity.shared.player.StubPlayer
import com.makd.afinity.shared.viewrr.FakeViewrrApi
import com.makd.afinity.shared.viewrr.PlaybackResolve
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
import kotlin.test.assertTrue

class PlayerViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun start_success_emitsReadyAndPlaysPlayer() = runTest {
        val fake = FakeViewrrApi(
            playback = PlaybackResolve(url = "https://stream/x.m3u8", startPositionSecs = 30),
        )
        val player = StubPlayer()
        val vm = PlayerViewModel(fake, player)

        vm.start("abc")
        advanceUntilIdle()

        val state = vm.state.value
        assertIs<PlayerUiState.Ready>(state)
        assertEquals("https://stream/x.m3u8", state.resolve.url)
        assertTrue(player.state.value.isPlaying)
        assertEquals(30, player.state.value.positionSecs)
    }

    @Test
    fun start_failure_fallsBackToSampleStream() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val player = StubPlayer()
        val vm = PlayerViewModel(fake, player)

        vm.start("abc")
        advanceUntilIdle()

        // Dev fallback (#101): on resolve failure, play the sample stream instead of erroring.
        val state = vm.state.value
        assertIs<PlayerUiState.Ready>(state)
        assertTrue(player.state.value.isPlaying)
    }
}
