package com.makd.afinity.shared.viewrr

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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
 * Auth handling (#101):
 *  - `expectSuccess = true` + an [HttpResponseValidator] map every non-2xx to a typed
 *    [ViewrrApiException] BEFORE Ktor tries to deserialize the error body as [AuthTokens]
 *    (which produced the cryptic serialization error a 401 login used to surface).
 *  - Authenticated calls run through a [TokenRefreshCoordinator]: a 401 triggers one
 *    refresh (POST /auth/refresh {refreshToken}) and a single retry; refresh failure logs out.
 *  - A sanitized `Logging` plugin logs request/response headers in debug WITHOUT leaking the
 *    Authorization header or credential bodies.
 *
 * @param baseUrl the Hub base URL (e.g. https://api.viewrr.stream / http://localhost:8080).
 * @param tokenProvider supplies the current bearer access token (opaque; Keycloak-ready), or null.
 * @param refreshTokenProvider supplies the persisted refresh token, or null.
 * @param onTokensRefreshed persists the new tokens after a successful refresh.
 * @param onAuthCleared logs the session out when refresh is impossible or fails.
 * @param enableLogging installs the sanitized [Logging] plugin (debug builds).
 * @param engine optional Ktor engine override (tests inject a MockEngine); null uses the platform default.
 */
class ViewrrClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val refreshTokenProvider: () -> String? = { null },
    private val onTokensRefreshed: (AuthTokens) -> Unit = {},
    private val onAuthCleared: () -> Unit = {},
    enableLogging: Boolean = true,
    engine: io.ktor.client.engine.HttpClientEngine? = null,
) : ViewrrApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client: HttpClient = run {
        val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            if (enableLogging) {
                install(Logging) {
                    // HEADERS only: never log request/response bodies (login body carries the
                    // password); redact Authorization so bearer tokens never hit the log.
                    level = LogLevel.HEADERS
                    sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
                }
            }
            HttpResponseValidator {
                // Ktor 3 does not throw on non-2xx by default; with expectSuccess it raises a raw
                // ResponseException. Map it to a typed, friendly exception here.
                handleResponseExceptionWithRequest { cause, _ ->
                    val responseException = cause as? ResponseException
                        ?: return@handleResponseExceptionWithRequest
                    throw ViewrrErrors.fromStatus(responseException.response.status.value)
                }
            }
            defaultRequest {
                url(baseUrl.trimEnd('/') + "/")
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.Application.Json)
            }
        }
        if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
    }

    private val refreshCoordinator = TokenRefreshCoordinator(
        refreshTokenProvider = refreshTokenProvider,
        doRefresh = ::performRefresh,
        onRefreshed = onTokensRefreshed,
        onCleared = onAuthCleared,
    )

    /** Run an authenticated call with a single 401 → refresh → retry cycle. */
    private suspend fun <T> authed(block: suspend () -> T): T = refreshCoordinator.withRetry(block)

    private suspend fun performRefresh(refreshToken: String): AuthTokens =
        client.post("auth/refresh") { setBody(RefreshRequest(refreshToken)) }.body()

    // ---- Auth (not wrapped by the refresh coordinator) ----

    override suspend fun login(username: String, password: String): AuthTokens =
        try {
            client.post("auth/login") { setBody(LoginRequest(username, password)) }.body()
        } catch (cause: Throwable) {
            throw ViewrrErrors.loginError(cause)
        }

    override suspend fun register(username: String, password: String, email: String): AuthTokens =
        try {
            client.post("auth/register") { setBody(RegisterRequest(username, password, email)) }.body()
        } catch (cause: Throwable) {
            throw ViewrrErrors.registerError(cause)
        }

    override suspend fun refresh(): AuthTokens {
        val refreshToken = refreshTokenProvider()
            ?: throw UnauthorizedException("No refresh token; please sign in again.")
        return performRefresh(refreshToken)
    }

    override suspend fun logout() {
        client.post("auth/logout")
    }

    // ---- Authenticated data calls (wrapped: 401 → refresh → retry) ----

    override suspend fun stremioKey(): StremioKey = authed { client.post("me/stremio-key").body() }

    override suspend fun continueWatching(): List<MediaItem> =
        authed { client.get("me/continue-watching").body() }

    override suspend fun recommendations(): List<MediaItem> =
        authed { client.get("me/recommendations").body() }

    override suspend fun recentlyAdded(): List<MediaItem> = authed {
        client.get("media") {
            parameter("sort", "createdAt")
            parameter("order", "desc")
        }.body()
    }

    override suspend fun shows(): List<MediaItem> = authed { client.get("series").body() }

    override suspend fun musicAlbums(): List<MediaItem> = authed { client.get("music/albums").body() }

    override suspend fun top(): List<MediaItem> = authed { client.get("home/top").body() }

    override suspend fun featured(): List<MediaItem> = authed { client.get("home/featured").body() }

    override suspend fun mediaDetail(id: String): MediaItem =
        authed { client.get("media/${id.encodeURLPathPart()}").body() }

    override suspend fun series(showTitle: String): Series =
        authed { client.get("series/${showTitle.encodeURLPathPart()}").body() }

    override suspend fun search(query: String): List<MediaItem> =
        authed { client.get("media/search") { parameter("q", query) }.body() }

    override suspend fun reportWatchEvent(event: WatchEvent) {
        authed { client.post("watch-events") { setBody(event) } }
    }

    override suspend fun watchEventsMe(): List<WatchEvent> =
        authed { client.get("watch-events/me").body() }

    override suspend fun playbackResolve(mediaId: String): PlaybackResolve =
        authed { client.get("playback/${mediaId.encodeURLPathPart()}").body() }

    override suspend fun subtitles(mediaId: String): List<Subtitle> =
        authed { client.get("media/${mediaId.encodeURLPathPart()}/subtitles").body() }
}
