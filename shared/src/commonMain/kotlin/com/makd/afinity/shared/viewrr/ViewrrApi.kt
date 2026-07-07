package com.makd.afinity.shared.viewrr

/**
 * The viewrr client data layer (commonMain). Implemented by [ViewrrClient] over Ktor.
 * This is the swap-point interface the migration keeps behind (replaces the Jellyfin
 * data layer). Endpoints marked 🔜 in the v0 contract are declared here and will 404
 * until the backend lands them (Phase 20) — clients should handle failures.
 */
interface ViewrrApi {
    // Auth
    suspend fun login(username: String, password: String): AuthTokens
    suspend fun register(username: String, password: String, email: String): AuthTokens
    suspend fun refresh(): AuthTokens
    suspend fun logout()
    suspend fun stremioKey(): StremioKey

    // Home rows
    suspend fun continueWatching(): List<MediaItem>
    suspend fun recommendations(): List<MediaItem>
    suspend fun recentlyAdded(): List<MediaItem>
    suspend fun shows(): List<MediaItem>
    suspend fun musicAlbums(): List<MediaItem>
    suspend fun top(): List<MediaItem> // 🔜 /home/top
    suspend fun featured(): List<MediaItem> // 🔜 /home/featured

    // Detail
    suspend fun mediaDetail(id: String): MediaItem // 🔜 GET /media/{id}
    suspend fun series(showTitle: String): Series

    // Search
    suspend fun search(query: String): List<MediaItem>

    // Watch progress
    suspend fun reportWatchEvent(event: WatchEvent)
    suspend fun watchEventsMe(): List<WatchEvent>

    // Playback
    suspend fun playbackResolve(mediaId: String): PlaybackResolve // 🔜 GET /playback/{mediaId}
    suspend fun subtitles(mediaId: String): List<Subtitle>

    // Payments — opt-in wallet (p2p-0020 / #3). Read-only: derive + display, no money movement.
    // Contract PINNED (mesh-hub implementing): docs/api/client-api.md `/api/pay/wallet/*`.
    suspend fun walletOptIn(): WalletInfo // POST /api/pay/wallet/opt-in — idempotent; provisions if absent
    suspend fun walletInfo(): WalletInfo // GET /api/pay/wallet — check .optedIn; false until opted in
}
