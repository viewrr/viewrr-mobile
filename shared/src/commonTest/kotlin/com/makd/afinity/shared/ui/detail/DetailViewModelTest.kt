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
    fun load_failure_emitsError() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val vm = DetailViewModel(fake)

        vm.load("abc")
        advanceUntilIdle()

        val state = vm.state.value
        assertIs<DetailUiState.Error>(state)
        assertEquals("boom", state.message)
    }
}
