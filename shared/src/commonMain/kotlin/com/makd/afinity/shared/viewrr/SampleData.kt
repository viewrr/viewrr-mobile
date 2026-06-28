package com.makd.afinity.shared.viewrr

// ponytail: dev-only placeholder so the home renders before a backend is reachable.
// Remove (or gate behind a debug flag) once the viewrr Hub is wired. Tracked with #101.
internal object SampleData {
    private fun item(id: String, title: String, year: Int) =
        MediaItem(id = id, title = title, cleanTitle = title, year = year)

    val homeRows: List<HomeRow> = listOf(
        HomeRow(
            "Continue Watching",
            listOf(item("1", "Dune: Part Two", 2024), item("2", "Oppenheimer", 2023), item("3", "Blade Runner 2049", 2017)),
        ),
        HomeRow(
            "Recommended",
            listOf(item("4", "Severance", 2022), item("5", "Foundation", 2021), item("6", "Silo", 2023), item("7", "The Expanse", 2015)),
        ),
        HomeRow(
            "Recently Added",
            listOf(item("8", "Arrival", 2016), item("9", "Interstellar", 2014), item("10", "The Martian", 2015), item("11", "Gravity", 2013)),
        ),
        HomeRow(
            "Shows",
            listOf(item("12", "Andor", 2022), item("13", "The Last of Us", 2023), item("14", "House of the Dragon", 2022)),
        ),
    )

    // ponytail: dev detail fallback so the screen renders (incl. Play) before /media/{id} exists.
    fun sampleDetail(id: String) = MediaItem(
        id = id,
        title = "Sample Title",
        cleanTitle = "Sample Title",
        year = 2024,
        durationSecs = 600,
        contentRating = "PG-13",
        overview = "Dev sample — no backend wired yet. Press Play to test the player against a public HLS stream.",
    )

    // ponytail: dev-only public HLS so the player works before /playback/{id} exists. Remove with #101.
    val samplePlayback = PlaybackResolve(
        url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        type = "hls",
    )
}
