package com.makd.afinity.shared.ui.search

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

class SearchViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun success_emitsContentWithResults() = runTest {
        val fake = FakeViewrrApi(
            searchResults = listOf(MediaItem(id = "1", title = "Dune")),
        )
        val vm = SearchViewModel(fake)

        vm.onQueryChange("dune")
        advanceUntilIdle()

        val state = vm.state.value
        assertIs<SearchUiState.Content>(state)
        assertEquals("Dune", state.results.single().title)
        assertEquals("dune", vm.query.value)
    }

    @Test
    fun blankQuery_emitsIdle() = runTest {
        val vm = SearchViewModel(FakeViewrrApi())

        vm.onQueryChange("   ")
        advanceUntilIdle()

        assertIs<SearchUiState.Idle>(vm.state.value)
    }

    @Test
    fun failure_emitsError() = runTest {
        val fake = FakeViewrrApi(error = RuntimeException("boom"))
        val vm = SearchViewModel(fake)

        vm.onQueryChange("dune")
        advanceUntilIdle()

        val state = vm.state.value
        assertIs<SearchUiState.Error>(state)
        assertEquals("boom", state.message)
    }
}
