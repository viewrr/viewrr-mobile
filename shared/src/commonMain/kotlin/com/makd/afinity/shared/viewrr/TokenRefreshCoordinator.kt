package com.makd.afinity.shared.viewrr

/**
 * Wraps authenticated calls with a single 401 → refresh → retry cycle. Kept free of Ktor
 * types so the retry semantics unit-test without a live HttpClient: [ViewrrClient] supplies
 * [refreshTokenProvider] (reads the persisted refresh token), [doRefresh] (POST /auth/refresh),
 * [onRefreshed] (persist new tokens) and [onCleared] (logout on refresh failure).
 *
 * We deliberately do NOT use Ktor's `Auth`/`bearer` plugin here: `ktor-client-auth` is not a
 * declared dependency (and build.gradle is owned by another change), so a response-driven retry
 * is the composable fit with the existing per-request bearer header.
 */
class TokenRefreshCoordinator(
    private val refreshTokenProvider: () -> String?,
    private val doRefresh: suspend (refreshToken: String) -> AuthTokens,
    private val onRefreshed: (AuthTokens) -> Unit,
    private val onCleared: () -> Unit,
) {
    /**
     * Run [block]; if it throws [UnauthorizedException] once, refresh the token and retry a
     * single time. If there is no refresh token, or the refresh itself fails, clear the session
     * (logout) and propagate. A second 401 propagates without another refresh.
     */
    suspend fun <T> withRetry(block: suspend () -> T): T {
        return try {
            block()
        } catch (unauthorized: UnauthorizedException) {
            val refreshToken = refreshTokenProvider()
            if (refreshToken.isNullOrBlank()) {
                onCleared()
                throw unauthorized
            }
            val newTokens = try {
                doRefresh(refreshToken)
            } catch (refreshFailure: Throwable) {
                onCleared()
                throw refreshFailure
            }
            onRefreshed(newTokens)
            block()
        }
    }
}
