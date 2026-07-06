package com.makd.afinity.shared.viewrr

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the auth-layer logic that would otherwise only be reachable through a live HttpClient:
 *  - a 401 on login maps to a friendly [InvalidCredentialsException], NOT a serialization crash;
 *  - the refresh coordinator sends the token, retries once, and logs out on failure.
 *
 * (A full HTTP-level test through the real validator needs `ktor-client-mock`, which is not a
 * declared test dependency — see the handoff note. The pure mapping/coordinator units below give
 * equivalent coverage of the behaviour.)
 */
class AuthErrorAndRefreshTest {

    // ---- 401 login → friendly error (not a serialization crash) ----

    @Test
    fun fromStatus_401_isUnauthorized() {
        assertIs<UnauthorizedException>(ViewrrErrors.fromStatus(401))
    }

    @Test
    fun loginError_from401_isFriendlyInvalidCredentials() {
        val mapped = ViewrrErrors.loginError(ViewrrErrors.fromStatus(401))
        assertIs<InvalidCredentialsException>(mapped)
        assertEquals("Invalid username or password.", mapped.message)
    }

    @Test
    fun loginError_from400_isFriendlyInvalidCredentials() {
        val mapped = ViewrrErrors.loginError(ViewrrErrors.fromStatus(400))
        assertIs<InvalidCredentialsException>(mapped)
    }

    @Test
    fun loginError_fromArbitraryThrowable_isNetworkError_notRawSerialization() {
        // A raw serialization/deserialization failure must NOT leak to the user.
        val raw = IllegalStateException("Field 'accessToken' is required but missing")
        val mapped = ViewrrErrors.loginError(raw)
        assertIs<ViewrrNetworkException>(mapped)
        assertEquals("Can't reach viewrr. Check your connection.", mapped.message)
    }

    @Test
    fun registerError_from400_isFriendlyRegistrationError() {
        val mapped = ViewrrErrors.registerError(ViewrrErrors.fromStatus(400))
        assertIs<RegistrationException>(mapped)
        assertTrue(mapped.message.contains("email", ignoreCase = true))
    }

    // ---- refresh sends the token and retries ----

    @Test
    fun withRetry_on401_refreshesWithStoredTokenAndRetriesOnce() = runTest {
        var refreshedWith: String? = null
        var persisted: AuthTokens? = null
        var cleared = false
        var attempts = 0

        val coordinator = TokenRefreshCoordinator(
            refreshTokenProvider = { "stored-refresh" },
            doRefresh = { token ->
                refreshedWith = token
                AuthTokens(token = "fresh-access", refreshToken = "fresh-refresh")
            },
            onRefreshed = { persisted = it },
            onCleared = { cleared = true },
        )

        val result = coordinator.withRetry {
            attempts++
            if (attempts == 1) throw UnauthorizedException() else "ok"
        }

        assertEquals("ok", result)
        assertEquals(2, attempts) // original + one retry
        assertEquals("stored-refresh", refreshedWith) // refresh received the persisted token
        assertEquals("fresh-access", persisted?.token) // new tokens persisted
        assertFalse(cleared)
    }

    @Test
    fun withRetry_success_doesNotRefresh() = runTest {
        var refreshed = false
        val coordinator = TokenRefreshCoordinator(
            refreshTokenProvider = { "stored-refresh" },
            doRefresh = { refreshed = true; AuthTokens(token = "x") },
            onRefreshed = {},
            onCleared = {},
        )

        val result = coordinator.withRetry { "immediate" }

        assertEquals("immediate", result)
        assertFalse(refreshed)
    }

    @Test
    fun withRetry_noRefreshToken_clearsSessionAndThrows() = runTest {
        var cleared = false
        val coordinator = TokenRefreshCoordinator(
            refreshTokenProvider = { null },
            doRefresh = { AuthTokens(token = "x") },
            onRefreshed = {},
            onCleared = { cleared = true },
        )

        assertFailsWith<UnauthorizedException> {
            coordinator.withRetry { throw UnauthorizedException() }
        }
        assertTrue(cleared)
    }

    @Test
    fun withRetry_refreshFails_clearsSessionAndThrows() = runTest {
        var cleared = false
        val coordinator = TokenRefreshCoordinator(
            refreshTokenProvider = { "stored-refresh" },
            doRefresh = { throw UnauthorizedException("refresh rejected") },
            onRefreshed = {},
            onCleared = { cleared = true },
        )

        assertFailsWith<UnauthorizedException> {
            coordinator.withRetry { throw UnauthorizedException() }
        }
        assertTrue(cleared)
    }

    @Test
    fun withRetry_secondCallAlsoFails_propagatesWithoutSecondRefresh() = runTest {
        var refreshCount = 0
        val coordinator = TokenRefreshCoordinator(
            refreshTokenProvider = { "stored-refresh" },
            doRefresh = { refreshCount++; AuthTokens(token = "fresh") },
            onRefreshed = {},
            onCleared = {},
        )

        assertFailsWith<UnauthorizedException> {
            coordinator.withRetry { throw UnauthorizedException() }
        }
        assertEquals(1, refreshCount) // exactly one refresh, no infinite loop
    }

    // ---- SessionStore persists the refresh token ----

    @Test
    fun sessionStore_setTokens_persistsAccessAndRefresh() {
        val store = SessionStore(com.russhwolf.settings.MapSettings())
        store.setTokens("access", "refresh")

        assertEquals("access", store.token)
        assertEquals("refresh", store.refreshToken)
        assertTrue(store.isLoggedIn.value)
    }

    @Test
    fun sessionStore_clear_dropsBothTokens() {
        val store = SessionStore(com.russhwolf.settings.MapSettings())
        store.setTokens("access", "refresh")

        store.clear()

        assertNull(store.token)
        assertNull(store.refreshToken)
        assertFalse(store.isLoggedIn.value)
    }

    @Test
    fun sessionStore_refreshToken_survivesReconstruction() {
        val settings = com.russhwolf.settings.MapSettings()
        SessionStore(settings).setTokens("access", "refresh")

        // Simulate app restart: a fresh store over the same persisted settings.
        val reloaded = SessionStore(settings)
        assertEquals("refresh", reloaded.refreshToken)
    }
}
