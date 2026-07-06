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
    fun homeRows_returnsEmptyWhenAllEndpointsEmptyAndFlagOff() = runTest {
        val api = FakeViewrrApi() // all lists default to emptyList()
        val repo = ViewrrMediaRepository(api, debug = false)

        val rows = repo.homeRows()

        // #102: in release (flag off) a broken/empty backend must surface as empty rows,
        // not fake sample content that masks the failure as "working".
        assertEquals(emptyList(), rows)
    }

    @Test
    fun homeRows_returnsEmptyWhenApiErrorsAndFlagOff() = runTest {
        val api = FakeViewrrApi(error = RuntimeException("backend down"))
        val repo = ViewrrMediaRepository(api, debug = false)

        val rows = repo.homeRows()

        // Each row's runCatching swallows the error, leaving the aggregate empty; with the
        // debug flag off the repository does NOT substitute sample rows.
        assertEquals(emptyList(), rows)
    }

    @Test
    fun homeRows_fallsBackToSampleDataWhenAllEndpointsEmptyAndFlagOn() = runTest {
        val api = FakeViewrrApi() // all lists default to emptyList()
        val repo = ViewrrMediaRepository(api, debug = true)

        val rows = repo.homeRows()

        // Debug-only convenience so the home renders before the backend lands (#102).
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
