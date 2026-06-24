package com.makd.afinity.shared.viewrr

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Ktor implementation of [ViewrrApi]. Engine resolves per platform (OkHttp on Android,
 * Darwin on iOS) from the dependency on the classpath — no expect/actual needed.
 *
 * @param baseUrl the Hub base URL (e.g. https://hub.viewrr / http://localhost:8080).
 * @param tokenProvider supplies the current bearer token (opaque; Keycloak-ready), or null.
 */
class ViewrrClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
) : ViewrrApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun login(username: String, password: String): AuthTokens =
        client.post("auth/login") { setBody(LoginRequest(username, password)) }.body()

    override suspend fun refresh(): AuthTokens = client.post("auth/refresh").body()

    override suspend fun logout() {
        client.post("auth/logout")
    }

    override suspend fun stremioKey(): StremioKey = client.post("me/stremio-key").body()

    override suspend fun continueWatching(): List<MediaItem> =
        client.get("me/continue-watching").body()

    override suspend fun recommendations(): List<MediaItem> =
        client.get("me/recommendations").body()

    override suspend fun recentlyAdded(): List<MediaItem> =
        client.get("media") {
            parameter("sort", "createdAt")
            parameter("order", "desc")
        }.body()

    override suspend fun shows(): List<MediaItem> = client.get("series").body()

    override suspend fun musicAlbums(): List<MediaItem> = client.get("music/albums").body()

    override suspend fun top(): List<MediaItem> = client.get("home/top").body()

    override suspend fun featured(): List<MediaItem> = client.get("home/featured").body()

    override suspend fun mediaDetail(id: String): MediaItem =
        client.get("media/${id.encodeURLPathPart()}").body()

    override suspend fun series(showTitle: String): Series =
        client.get("series/${showTitle.encodeURLPathPart()}").body()

    override suspend fun search(query: String): List<MediaItem> =
        client.get("media/search") { parameter("q", query) }.body()

    override suspend fun reportWatchEvent(event: WatchEvent) {
        client.post("watch-events") { setBody(event) }
    }

    override suspend fun watchEventsMe(): List<WatchEvent> = client.get("watch-events/me").body()

    override suspend fun playbackResolve(mediaId: String): PlaybackResolve =
        client.get("playback/${mediaId.encodeURLPathPart()}").body()

    override suspend fun subtitles(mediaId: String): List<Subtitle> =
        client.get("media/${mediaId.encodeURLPathPart()}/subtitles").body()
}
