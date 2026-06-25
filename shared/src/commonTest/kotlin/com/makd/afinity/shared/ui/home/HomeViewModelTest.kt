package com.makd.afinity.shared.ui.home

import com.makd.afinity.shared.viewrr.FakeViewrrApi
import com.makd.afinity.shared.viewrr.HomeRow
import com.makd.afinity.shared.viewrr.MediaItem
import com.makd.afinity.shared.viewrr.MediaRepository
import com.makd.afinity.shared.viewrr.PlaybackResolve
import com.makd.afinity.shared.viewrr.ViewrrMediaRepository
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

class HomeViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun success_emitsContentFromRepository() = runTest {
        val fake = FakeViewrrApi(
            continueWatching = listOf(MediaItem(id = "cw1", title = "Resume Me")),
        )
        val vm = HomeViewModel(ViewrrMediaRepository(fake))

        advanceUntilIdle()

        val state = vm.state.value
        assertIs<HomeUiState.Content>(state)
        assertTrue(state.rows.any { it.title == "Continue Watching" })
        val cw = state.rows.first { it.title == "Continue Watching" }
        assertEquals("Resume Me", cw.items.single().title)
    }

    @Test
    fun failure_emitsError() = runTest {
        // The real repository swallows per-call errors, so use a repository that throws.
        val throwingRepo = object : MediaRepository {
            override suspend fun homeRows(): List<HomeRow> = throw RuntimeException("boom")
            override suspend fun search(query: String): List<MediaItem> = emptyList()
            override suspend fun detail(id: String): MediaItem = MediaItem(id = "x", title = "X")
            override suspend fun resolvePlayback(id: String): PlaybackResolve = PlaybackResolve(url = "x")
        }
        val vm = HomeViewModel(throwingRepo)

        advanceUntilIdle()

        val state = vm.state.value
        assertIs<HomeUiState.Error>(state)
        assertEquals("boom", state.message)
    }
}
