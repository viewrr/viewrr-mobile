# viewrr-mobile — Agent Handoff

You are picking up the **viewrr Android + iOS client**. This doc is your cold-start. This
repo is a **fork of [MakD/AFinity](https://github.com/MakD/AFinity)** (a native Android
Jellyfin client: Kotlin, Jetpack Compose, Material 3, libmpv). Read this, then skim the
AFinity codebase under `app/`.

## What viewrr is
Self-hosted FOSS OTT platform (Jellyfin alternative). Backend: Kotlin/Ktor, repo
[`viewrr/viewrr`](https://github.com/viewrr/viewrr). Distributed Hub/Node internally, but
**the app only talks to the Hub's API** — ignore the Node/distributed internals.

## Your mission
One mobile codebase for **Android + iOS**, by migrating this AFinity fork to **Compose
Multiplatform**, retargeted from the Jellyfin API to **viewrr's own API**, in the
**Apple-TV design language**.

## The three hard problems (in order)
1. **AFinity is Android-only.** Migrate Compose (Android) → **Compose Multiplatform** (shared module + iOS target). This is the biggest unknown.
2. **The player.** AFinity uses **libmpv** (Android). Bring a working player to iOS under CMP — libmpv-ios or AVPlayer behind a shared `Player` interface.
3. **API retarget.** AFinity speaks the **Jellyfin API**. Rip out that data layer and implement a **viewrr API** client (catalog, detail, search, playback URL, auth).

## Issues (milestone [Phase 19: Clients](https://github.com/viewrr/viewrr/milestone/18))
- **#98** fork → clean Android baseline build (do this first, before touching architecture).
- **#99** migrate Compose → Compose Multiplatform (iOS target).
- **#100** iOS player (libmpv-ios or AVPlayer under a shared interface).
- **#101** retarget data layer: Jellyfin API → viewrr API.
- **#102** triage + fix AFinity bugs during the port (split into sub-issues as found).

## Decisions of record (do not relitigate)
- **AFinity fork + CMP** (not native Swiftfin for iOS) — one codebase. See [ADR-0005](https://github.com/viewrr/viewrr/blob/main/docs/adr/0005-client-stack.md).
- **viewrr's own API, NOT Jellyfin API** — hence the retarget.
- **Apple-TV design language** across all clients.

## Design
The visual contract + tokens live in the sibling repo
[`viewrr/viewrr-web`](https://github.com/viewrr/viewrr-web): **DESIGN.md** + `design/tokens.json`
(+ the `design/index.html` mock). Mirror those tokens (color/radius/type/space/motion,
focus = scale 1.06 + ring) into the Compose theme so web and mobile stay in visual parity.

## Auth
- **Humans** via **Keycloak** (OIDC — Google OAuth, passkeys, SSO). Use an OIDC/AppAuth flow.
- **Playback** uses a **per-device stremio-key** (long-lived) in the stream URL path.

## API contract — COORDINATE WITH BACKEND AGENT

**v0 contract is now written:** [`docs/api/client-api.md`](https://github.com/viewrr/viewrr/blob/main/docs/api/client-api.md) in `viewrr/viewrr`. Build against it (✅ exists vs 🔜 gap). File issues for missing pieces.
The viewrr client REST API is **not finalized**. The server today exposes a Stremio addon
(`/stremio/{key}/...`), HLS stream routes, `/auth/*`, and assorted REST. A clean client API
is the **backend agent's** deliverable. Read `viewrr/viewrr`
`server/src/main/kotlin/**/*Routes.kt`; where an endpoint you need is missing, file an
issue against `viewrr/viewrr` for the backend rather than inventing it. Keep the data layer
behind an interface so the swap is cheap.

## Suggested first steps
1. #98: get the **AFinity Android app building** on current toolchain, unchanged. Baseline before any migration.
2. Map the KMP module split (shared: data, domain, UI; platform: player, DI, entrypoints).
3. Then #99 (CMP) → #100 (iOS player) → #101 (API retarget) in that order.

## Pointers
- Glossary: [`viewrr/viewrr` CONTEXT.md](https://github.com/viewrr/viewrr/blob/main/CONTEXT.md). Wiki: Serving / Network pages (stremio-key, locality).
- Upstream you forked from: `MakD/AFinity` — pull useful fixes, but expect heavy divergence after the CMP migration.
