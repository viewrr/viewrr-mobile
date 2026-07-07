package com.makd.afinity.shared.viewrr

/**
 * Test double for [ViewrrApi]. Configure return values per field; set [error] to make
 * every call throw. Records the last login() args. Used by commonTest unit tests.
 */
class FakeViewrrApi(
    var error: Throwable? = null,
    var loginTokens: AuthTokens = AuthTokens(token = "test-token"),
    var continueWatching: List<MediaItem> = emptyList(),
    var recommendations: List<MediaItem> = emptyList(),
    var recentlyAdded: List<MediaItem> = emptyList(),
    var shows: List<MediaItem> = emptyList(),
    var musicAlbums: List<MediaItem> = emptyList(),
    var top: List<MediaItem> = emptyList(),
    var featured: List<MediaItem> = emptyList(),
    var searchResults: List<MediaItem> = emptyList(),
    var detail: MediaItem = MediaItem(id = "x", title = "X"),
    var seriesResult: Series = Series(showTitle = "X"),
    var watchEvents: List<WatchEvent> = emptyList(),
    var playback: PlaybackResolve = PlaybackResolve(url = "https://stream/x.m3u8"),
    var stremio: StremioKey = StremioKey(key = "k"),
    var wallet: WalletInfo = WalletInfo(optedIn = false),
) : ViewrrApi {

    var lastLogin: Pair<String, String>? = null
    var lastRegister: Triple<String, String, String>? = null
    var reportedEvents: MutableList<WatchEvent> = mutableListOf()
    var walletOptInCalls: Int = 0
    var walletInfoCalls: Int = 0

    private fun <T> guard(value: T): T = error?.let { throw it } ?: value

    override suspend fun login(username: String, password: String): AuthTokens {
        lastLogin = username to password
        return guard(loginTokens)
    }

    override suspend fun register(username: String, password: String, email: String): AuthTokens {
        lastRegister = Triple(username, password, email)
        return guard(loginTokens)
    }

    override suspend fun refresh(): AuthTokens = guard(loginTokens)
    override suspend fun logout() { guard(Unit) }
    override suspend fun stremioKey(): StremioKey = guard(stremio)

    override suspend fun continueWatching(): List<MediaItem> = guard(continueWatching)
    override suspend fun recommendations(): List<MediaItem> = guard(recommendations)
    override suspend fun recentlyAdded(): List<MediaItem> = guard(recentlyAdded)
    override suspend fun shows(): List<MediaItem> = guard(shows)
    override suspend fun musicAlbums(): List<MediaItem> = guard(musicAlbums)
    override suspend fun top(): List<MediaItem> = guard(top)
    override suspend fun featured(): List<MediaItem> = guard(featured)

    override suspend fun mediaDetail(id: String): MediaItem = guard(detail)
    override suspend fun series(showTitle: String): Series = guard(seriesResult)
    override suspend fun search(query: String): List<MediaItem> = guard(searchResults)

    override suspend fun reportWatchEvent(event: WatchEvent) {
        reportedEvents.add(event)
        guard(Unit)
    }

    override suspend fun watchEventsMe(): List<WatchEvent> = guard(watchEvents)
    override suspend fun playbackResolve(mediaId: String): PlaybackResolve = guard(playback)
    override suspend fun subtitles(mediaId: String): List<Subtitle> = guard(emptyList())

    override suspend fun walletOptIn(): WalletInfo {
        walletOptInCalls++
        wallet = wallet.copy(
            optedIn = true,
            address = wallet.address ?: "0xtest",
        )
        return guard(wallet)
    }

    override suspend fun walletInfo(): WalletInfo {
        walletInfoCalls++
        return guard(wallet)
    }
}
