package com.makd.afinity.shared.viewrr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MediaRepositoryTest {

    private fun item(id: String, title: String) = MediaItem(id = id, title = title)

    @Test
    fun homeRows_includesTitledRowsForPopulatedEndpoints() = runTest {
        val shows = listOf(item("s1", "Andor"), item("s2", "Silo"))
        val recs = listOf(item("r1", "Severance"))
        val api = FakeViewrrApi(shows = shows, recommendations = recs)
        val repo = ViewrrMediaRepository(api)

        val rows = repo.homeRows()

        val showsRow = rows.firstOrNull { it.title == "Shows" }
        val recsRow = rows.firstOrNull { it.title == "Recommended" }
        assertTrue(showsRow != null, "expected a 'Shows' row")
        assertTrue(recsRow != null, "expected a 'Recommended' row")
        assertEquals(shows, showsRow.items)
        assertEquals(recs, recsRow.items)
    }

    @Test
    fun homeRows_fallsBackToSampleDataWhenAllEndpointsEmpty() = runTest {
        val api = FakeViewrrApi() // all lists default to emptyList()
        val repo = ViewrrMediaRepository(api)

        val rows = repo.homeRows()

        assertEquals(SampleData.homeRows, rows)
    }

    @Test
    fun homeRows_fallsBackToSampleDataWhenApiErrors() = runTest {
        val api = FakeViewrrApi(error = RuntimeException("backend down"))
        val repo = ViewrrMediaRepository(api)

        val rows = repo.homeRows()

        // Each row's runCatching swallows the error, leaving the aggregate empty,
        // so the repository falls back to the sample rows.
        assertTrue(rows.isNotEmpty(), "expected non-empty fallback rows")
        assertEquals(SampleData.homeRows, rows)
    }

    @Test
    fun search_delegatesToApi() = runTest {
        val results = listOf(item("q1", "Dune"))
        val api = FakeViewrrApi(searchResults = results)
        val repo = ViewrrMediaRepository(api)

        assertEquals(results, repo.search("dune"))
    }

    @Test
    fun detail_delegatesToApi() = runTest {
        val detail = MediaItem(id = "d1", title = "Oppenheimer")
        val api = FakeViewrrApi(detail = detail)
        val repo = ViewrrMediaRepository(api)

        assertEquals(detail, repo.detail("d1"))
    }
}
