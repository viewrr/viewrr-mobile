---
project: viewrr-mobile
phase: execute
mode: algorithm
effort: E4
updated: 2026-06-24
---

# viewrr-mobile — Compose Multiplatform Migration

## Problem
AFinity is an Android-only Jellyfin client (Kotlin, Compose, Hilt, libmpv, Retrofit).
viewrr needs ONE mobile codebase for **Android + iOS**, talking viewrr's own API, in
Apple-TV design language. The fork must become Compose Multiplatform, shed Android-only
deps that block iOS, and replace the Jellyfin data layer with a viewrr API client.

## Vision
A single shared CMP module drives both Android and iOS from one Kotlin codebase; the player
sits behind a platform interface; data comes from viewrr's REST API. Adding a feature touches
`commonMain` once, ships to both stores.

## Out of Scope
- Node / distributed-hub internals (app only talks the Hub API).
- Porting Jellyfin/Retrofit data code to KMP — it is being **replaced**, not migrated (#101).
- TV / desktop targets (mobile Android + iOS only for now).

## Constraints
- Toolchain (do not downgrade): JDK 26 host, Gradle 9.4.1, AGP 9.2.1, Kotlin 2.3.21,
  compileSdk 36 / minSdk 35.
- Compose Multiplatform plugin 1.11.1; Compose compiler ships with Kotlin (auto-match).
- DI = **Koin 4.1.0** (Hilt has no KMP/iOS support — decided 2026-06-24).
- Module layout = **`:shared` (commonMain/androidMain/iosMain) + `:app` (Android entry) +
  `iosApp` (Xcode)** — decided 2026-06-24.
- Data layer stays behind an interface so the viewrr-vs-Jellyfin swap is cheap.
- Android SDK lives at `/opt/homebrew/share/android-commandlinetools` (`local.properties`, gitignored).

## Goal
Migrate the AFinity Android Compose app to Compose Multiplatform with a working iOS target:
Hilt→Koin, portable code in `commonMain`, Android leakage behind `expect/actual`, player and
data layer behind shared interfaces — then retarget the data layer to viewrr's API.

## Criteria
- [x] ISC-1: `:app:assembleDebug` builds green on the current toolchain (baseline #98).
- [x] ISC-2: `:shared` KMP module exists with android + ios targets; android build stays green.
- [x] ISC-3: `:shared:compileKotlinIosSimulatorArm64` succeeds (iOS target compiles).
- [ ] ISC-4: Zero `dagger.hilt` / `javax.inject` imports remain in shared code (Koin swap).
- [x] ISC-5: App wires DI via Koin; `:app:assembleDebug` green post-swap (+ Koin verify() test passes).
- [ ] ISC-6: Domain models + use cases live in `commonMain`.
- [ ] ISC-7: ViewModels live in `commonMain` (lifecycle-viewmodel KMP).
- [ ] ISC-8: UI composables + navigation live in `commonMain`.
- [ ] ISC-9: Each `android.content.Context` use (66 files) is removed or behind `expect/actual`.
- [ ] ISC-10: `Player` interface in `commonMain`; mpv impl in androidMain (iOS impl = #100).
- [x] ISC-11: iosApp Xcode project launches the shared commonMain CMP home on the iPhone 16 simulator (verified 2026-06-25). Full app parity = Stage 2 remainder.
- [x] ISC-12: viewrr data layer = commonMain `ViewrrApi` interface + `ViewrrClient` (Ktor) impl; compiles android+iOS. (wiring into app/VMs = remainder of #101.)
- [x] ISC-13: Anti: shared/commonMain has zero jellyfin/retrofit imports (viewrr-native, replaced not ported).
- [ ] ISC-14: Anti: `:app` keeps building green after every stage (no broken-baseline commits).

## Test Strategy
isc | type | check | tool
ISC-1/2/3/5/14 | build | gradle task exit 0 | Bash ./gradlew
ISC-4/13 | static | grep import count == 0 | Grep
ISC-6/7/8 | static | files present under commonMain | Glob
ISC-9 | static | grep android.content.Context in commonMain == 0 | Grep
ISC-10/12 | static | interface in commonMain, impl in platform set | Grep
ISC-11 | live | simulator launch screenshot | Xcode/Interceptor

## Features
name | satisfies | depends_on | parallelizable
Stage0-scaffold | ISC-2,3 | ISC-1 | no
Stage1-koin | ISC-4,5 | Stage0 | no
Stage2-commonMain | ISC-6,7,8 | Stage1 | partly
Stage3-expectactual | ISC-9,10 | Stage2 | partly
Stage4-iosApp+api | ISC-11,12 | Stage3 | no (#100/#101)

## Decisions
- 2026-06-24: SDK was missing; installed cmdline-tools + platform-36 + build-tools 36 via brew. #98 green.
- 2026-06-24: DI = Koin 4.1.0 (user-chosen). Hilt KMP-blocking, ~283 import sites.
- 2026-06-24: Layout = shared + app + iosApp (user-chosen, official JetBrains CMP).
- 2026-06-24: Data layer replaced not ported — #101 rewrites KMP-native (Ktor), skip Jellyfin/Retrofit port.
- 2026-06-24: iosApp Xcode project deferred to Stage 4 (needs Xcode; iOS-compile proven via shared framework target meanwhile).
- 2026-06-25: REORDER — Stage 2 (commonMain UI move) gated on #101. New order: #101 viewrr KMP
  models/data-layer in commonMain → Stage 2 UI/VM move → Stage 3 expect/actual → Stage 4 iOS.
  Forge/Anvil/codex unavailable this env (no API keys); Stage 1 done by hand.

## Changelog
- 2026-06-25 conjecture: Stage 2 (move domain/VM/UI → commonMain) follows Stage 1 directly.
  refuted-by: coupling scan — domain models are Jellyfin SDK DTOs (40 sdk imports) + Room
  @Entity classes, depended on pervasively (repos → VMs → UI). jellyfin-sdk is not KMP and
  Room android-entities can't live in commonMain; per Anti ISC-13 these are replaced, not ported.
  learned: **Stage 2 is gated on #101.** The viewrr-native KMP model layer must exist in
  commonMain before UI/VMs can move. Reordered plan: #101-models-first, then Stage 2 UI move,
  then Stage 3 expect/actual for residual Context. #101 needs viewrr v0 API contract
  (docs/api/client-api.md in viewrr/viewrr) — partially available; backend not finalized.

## Verification
ISC-1: ./gradlew :app:assembleDebug — BUILD SUCCESSFUL in 4m32s, APKs in app/build/outputs/apk/debug.

ISC-2: :shared module builds; :app:assembleDebug green with shared dep (BUILD SUCCESSFUL 4m4s).
ISC-3: ./gradlew :shared:compileKotlinIosSimulatorArm64 — BUILD SUCCESSFUL.

ISC-5: ./gradlew :app:assembleDebug + :app:testDebugUnitTest KoinModulesTest — BUILD SUCCESSFUL, 1 test 0 failures. Hilt removed (0 dagger imports), Koin 4.1 wired, graph verified.

ISC-12: shared/commonMain/.../viewrr/{Models,ViewrrApi,ViewrrClient,ViewrrModule}.kt — ./gradlew :shared:compileKotlinIosSimulatorArm64 :app:assembleDebug BUILD SUCCESSFUL (3m50s). Ktor 3.2 KMP, engine per-platform (okhttp/darwin).
ISC-13: grep jellyfin/retrofit in shared/commonMain = 0.

Slice (#101/#99): MediaRepository + HomeViewModel + HomeScreen in shared/commonMain over ViewrrApi — ./gradlew :shared:compileKotlinIosSimulatorArm64 :app:assembleDebug BUILD SUCCESSFUL (14s). Real CMP vertical slice (data->VM->UI) on android+iOS; lifecycle.ViewModel+koinViewModel resolve in commonMain. Partial ISC-6/7/8 (template; full UI migration remains).

Cross-platform live test (2026-06-25): android emulator (AVD viewrr) + iOS simulator (iPhone 16) both launch MainActivity/iosApp into the same commonMain HomeScreen, rendering Apple-TV rows (Continue Watching/Recommended/Recently Added/Shows) from MediaRepository. iOS PlistSanityCheck fixed via CADisableMinimumFrameDurationOnPhone. No crash; apps stay resident. Partial ISC-11 (iosApp launches shared CMP UI on simulator).
