package com.makd.afinity.shared.viewrr

/**
 * Typed viewrr API failures. In Ktor 3 a non-2xx response does NOT throw by default and
 * `expectSuccess = true` only surfaces a raw [io.ktor.client.plugins.ResponseException]; if
 * we let that reach the UI, a 401 login tries to deserialize the error body as [AuthTokens]
 * and the user sees a cryptic serialization error. [ViewrrClient]'s response validator maps
 * every HTTP error to one of these instead, so callers get a friendly [message].
 */
sealed class ViewrrApiException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)

/** 401 — token missing/expired on an authenticated call (triggers refresh+retry). */
class UnauthorizedException(
    message: String = "Your session has expired. Please sign in again.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** 403 — authenticated but not allowed. */
class ForbiddenException(
    message: String = "You don't have access to that.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** 400 — the server rejected the request shape (e.g. register without an email). */
class BadRequestException(
    message: String = "That request was invalid.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** 5xx — the server failed. */
class ServerException(
    val status: Int,
    message: String = "The server had a problem. Please try again.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** Any other non-2xx status. */
class ViewrrHttpException(
    val status: Int,
    message: String = "Request failed ($status).",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** Transport-level failure (no HTTP status): DNS, connection refused, timeout, etc. */
class ViewrrNetworkException(
    message: String = "Can't reach viewrr. Check your connection.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** Login-specific friendly failure for bad credentials (401/400 on /auth/login). */
class InvalidCredentialsException(
    message: String = "Invalid username or password.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/** Register-specific friendly failure (400 on /auth/register — usually a taken username or bad email). */
class RegistrationException(
    message: String = "Couldn't create your account. Check your details — email is required.",
    cause: Throwable? = null,
) : ViewrrApiException(message, cause)

/**
 * Pure mapping helpers — no Ktor types, so they unit-test without a live HttpClient or a
 * MockEngine dependency. [ViewrrClient]'s response validator feeds HTTP statuses through
 * [fromStatus]; the auth calls translate the result into a caller-friendly message.
 */
object ViewrrErrors {

    /** Map a raw HTTP status to a typed exception. */
    fun fromStatus(status: Int, serverMessage: String? = null): ViewrrApiException = when (status) {
        401 -> UnauthorizedException()
        403 -> ForbiddenException()
        400 -> BadRequestException(serverMessage ?: "That request was invalid.")
        in 500..599 -> ServerException(status)
        else -> ViewrrHttpException(status, serverMessage ?: "Request failed ($status).")
    }

    /** Translate a failure from POST /auth/login into a friendly, user-facing exception. */
    fun loginError(cause: Throwable): ViewrrApiException = when (cause) {
        is UnauthorizedException, is BadRequestException -> InvalidCredentialsException(cause = cause)
        is ViewrrApiException -> cause
        else -> ViewrrNetworkException(cause = cause)
    }

    /** Translate a failure from POST /auth/register into a friendly, user-facing exception. */
    fun registerError(cause: Throwable): ViewrrApiException = when (cause) {
        is BadRequestException -> RegistrationException(cause = cause)
        is UnauthorizedException -> RegistrationException(cause = cause)
        is ViewrrApiException -> cause
        else -> ViewrrNetworkException(cause = cause)
    }
}
