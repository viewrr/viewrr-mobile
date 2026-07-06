package com.makd.afinity.shared.ui.detail

import com.makd.afinity.shared.viewrr.FakeViewrrApi
import com.makd.afinity.shared.viewrr.MediaItem
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

class DetailViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_emitsContent() = runTest {
        val fake = FakeViewrrApi(
            detail = MediaItem(id = "abc", title = "Oppenheimer"),
        )
        val vm = DetailViewModel(fake)

        vm.load("abc")
        advanceUntilIdle()

        val state = vm.state.value
        assertIs<DetailUiState.Content>(state)
        assertEquals("Oppenheimer", state.item.title)
    }

    @Test
    fun load_failure_emitsErrorWhenFlagOff() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val vm = DetailViewModel(fake, debug = false)

        vm.load("abc")
        advanceUntilIdle()

        // #102: in release a failed detail load must surface as an error, not a fake sample item.
        val state = vm.state.value
        assertIs<DetailUiState.Error>(state)
        assertEquals("boom", state.message)
    }

    @Test
    fun load_failure_fallsBackToSampleDetailWhenFlagOn() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val vm = DetailViewModel(fake, debug = true)

        vm.load("abc")
        advanceUntilIdle()

        // Debug-only fallback (#102): render a sample item carrying the requested id.
        val state = vm.state.value
        assertIs<DetailUiState.Content>(state)
        assertEquals("abc", state.item.id)
    }
}
