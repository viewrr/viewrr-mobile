package com.makd.afinity.shared.viewrr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ModelsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mediaItem_roundTrips() {
        val original = MediaItem(
            id = "m1",
            title = "Dune: Part Two",
            cleanTitle = "Dune Part Two",
            showTitle = null,
            season = null,
            episode = null,
            year = 2024,
            poster = "https://img/poster.jpg",
            backdrop = "https://img/backdrop.jpg",
            overview = "Sand.",
            durationSecs = 9660L,
            contentRating = "PG-13",
        )

        val decoded = json.decodeFromString<MediaItem>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun playbackResolve_roundTrips() {
        val original = PlaybackResolve(
            url = "https://stream/x.m3u8",
            type = "hls",
            drm = "widevine",
            subtitles = listOf(Subtitle(lang = "en", url = "https://sub/en.vtt", label = "English")),
            startPositionSecs = 120L,
        )

        val decoded = json.decodeFromString<PlaybackResolve>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun watchEvent_roundTrips() {
        val original = WatchEvent(
            mediaId = "m1",
            positionSecs = 42L,
            eventType = WatchEventType.PROGRESS,
            sessionId = "sess-1",
        )

        val decoded = json.decodeFromString<WatchEvent>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun mediaItem_ignoresUnknownFields() {
        val raw = """{"id":"m1","title":"Dune","unexpected":"field","extra":123}"""

        val decoded = json.decodeFromString<MediaItem>(raw)

        assertEquals("m1", decoded.id)
        assertEquals("Dune", decoded.title)
    }
}
