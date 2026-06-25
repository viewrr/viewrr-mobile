package com.makd.afinity.shared.ui.library

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

class LibraryViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initLoadsShows() = runTest {
        val fake = FakeViewrrApi(
            shows = listOf(MediaItem(id = "s1", title = "Andor")),
        )
        val vm = LibraryViewModel(fake)

        advanceUntilIdle()

        assertEquals(LibraryTab.SHOWS, vm.selectedTab.value)
        val state = vm.state.value
        assertIs<LibraryUiState.Content>(state)
        assertEquals("Andor", state.items.single().title)
    }

    @Test
    fun selectMusic_loadsMusicAlbums() = runTest {
        val fake = FakeViewrrApi(
            shows = listOf(MediaItem(id = "s1", title = "Andor")),
            musicAlbums = listOf(MediaItem(id = "m1", title = "Random Access Memories")),
        )
        val vm = LibraryViewModel(fake)
        advanceUntilIdle()

        vm.selectTab(LibraryTab.MUSIC)
        advanceUntilIdle()

        assertEquals(LibraryTab.MUSIC, vm.selectedTab.value)
        val state = vm.state.value
        assertIs<LibraryUiState.Content>(state)
        assertEquals("Random Access Memories", state.items.single().title)
    }

    @Test
    fun failure_emitsError() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val vm = LibraryViewModel(fake)

        advanceUntilIdle()

        val state = vm.state.value
        assertIs<LibraryUiState.Error>(state)
        assertEquals("boom", state.message)
    }
}
