package com.makd.afinity.shared.viewrr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * viewrr client API models — v0 contract (docs/api/client-api.md in viewrr/viewrr).
 * NOT Jellyfin shapes. Nullable fields come from optional TMDb enrichment.
 */
@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val cleanTitle: String? = null,
    val showTitle: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val overview: String? = null,
    val durationSecs: Long? = null,
    val contentRating: String? = null,
)

@Serializable
data class Series(
    val showTitle: String,
    val title: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val overview: String? = null,
    val seasons: List<Season> = emptyList(),
)

@Serializable
data class Season(
    val season: Int,
    val episodes: List<MediaItem> = emptyList(),
)

enum class WatchEventType {
    @SerialName("start") START,
    @SerialName("progress") PROGRESS,
    @SerialName("pause") PAUSE,
    @SerialName("stop") STOP,
}

@Serializable
data class WatchEvent(
    val mediaId: String,
    val positionSecs: Long,
    val eventType: WatchEventType,
    val sessionId: String,
)

@Serializable
data class Subtitle(
    val lang: String,
    val url: String,
    val label: String? = null,
)

/** Clean playback resolve (GET /playback/{mediaId}) — the one endpoint clients should call. */
@Serializable
data class PlaybackResolve(
    val url: String,
    val type: String = "hls",
    val drm: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
    val startPositionSecs: Long = 0,
)

@Serializable
data class StremioKey(val key: String)

@Serializable
data class AuthTokens(
    @SerialName("accessToken") val token: String,
    val refreshToken: String? = null,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

/** POST /auth/register — email is REQUIRED by the backend (400 without it). */
@Serializable
data class RegisterRequest(val username: String, val password: String, val email: String)

/** POST /auth/refresh — carries the persisted refresh token. */
@Serializable
data class RefreshRequest(val refreshToken: String)

/**
 * Read-only wallet surface for the opt-in payments feature (p2p-0020 / #3). Contract PINNED
 * against mesh-hub's `/api/pay/wallet` routes:
 *  - `POST /api/pay/wallet/opt-in` -> `{"address":"0x...","optedIn":true}` (idempotent).
 *  - `GET /api/pay/wallet` opted in -> `{"address":"0x...","balanceBaseUnits":"1500000",
 *    "asset":"USDC","decimals":6,"optedIn":true}`; not opted in -> `{"optedIn":false}`.
 * [balanceBaseUnits] is the raw on-chain integer as a STRING (avoids float precision loss),
 * scaled by [decimals] (USDC = 6, i.e. 1 USDC = "1000000"). [address]/[balanceBaseUnits]/
 * [asset]/[decimals] are absent when [optedIn] is false. No signing / send fields here by
 * design — this slice is derive+display only (legal #9 open).
 */
@Serializable
data class WalletInfo(
    val optedIn: Boolean,
    val address: String? = null,
    val balanceBaseUnits: String? = null,
    val asset: String? = null,
    val decimals: Int? = null,
)
