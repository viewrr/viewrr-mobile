package com.makd.afinity.shared.viewrr

import com.makd.afinity.shared.isDebugBuild

/**
 * Domain repository over [ViewrrApi] — the viewrr-native replacement for AFinity's
 * Jellyfin MediaRepository. commonMain; no platform deps. Aggregates the Apple-TV home rows.
 */
interface MediaRepository {
    suspend fun homeRows(): List<HomeRow>
    suspend fun search(query: String): List<MediaItem>
    suspend fun detail(id: String): MediaItem
    suspend fun resolvePlayback(id: String): PlaybackResolve
}

data class HomeRow(val title: String, val items: List<MediaItem>)

class ViewrrMediaRepository(
    private val api: ViewrrApi,
    private val debug: Boolean = isDebugBuild,
) : MediaRepository {

    override suspend fun homeRows(): List<HomeRow> {
        // Per v0 contract; 🔜 rows (top/featured) may fail until backend lands — skip on error.
        val rows = mutableListOf<HomeRow>()
        runCatching { api.continueWatching() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Continue Watching", it) }
        runCatching { api.featured() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Featured", it) }
        runCatching { api.recommendations() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Recommended", it) }
        runCatching { api.top() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Top 10", it) }
        runCatching { api.recentlyAdded() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Recently Added", it) }
        runCatching { api.shows() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Shows", it) }
        runCatching { api.musicAlbums() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { rows += HomeRow("Music", it) }
        // #102: only mask empty backend rows with sample data in debug builds. In release,
        // return the real (possibly empty) rows so the UI surfaces an empty/error state
        // instead of fake "working" content.
        return rows.ifEmpty { if (debug) SampleData.homeRows else emptyList() }
    }

    override suspend fun search(query: String): List<MediaItem> = api.search(query)

    override suspend fun detail(id: String): MediaItem = api.mediaDetail(id)

    override suspend fun resolvePlayback(id: String): PlaybackResolve = api.playbackResolve(id)
}
