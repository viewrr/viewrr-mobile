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
- [ ] ISC-5: App wires DI via Koin; `:app:assembleDebug` green post-swap.
- [ ] ISC-6: Domain models + use cases live in `commonMain`.
- [ ] ISC-7: ViewModels live in `commonMain` (lifecycle-viewmodel KMP).
- [ ] ISC-8: UI composables + navigation live in `commonMain`.
- [ ] ISC-9: Each `android.content.Context` use (66 files) is removed or behind `expect/actual`.
- [ ] ISC-10: `Player` interface in `commonMain`; mpv impl in androidMain (iOS impl = #100).
- [ ] ISC-11: `iosApp` Xcode project launches the shared CMP UI on simulator.
- [ ] ISC-12: Data layer is a `commonMain` interface; viewrr Ktor client implements it (#101).
- [ ] ISC-13: Anti: no Jellyfin/Retrofit code ported into `commonMain` (replaced, not migrated).
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

## Changelog

## Verification
ISC-1: ./gradlew :app:assembleDebug — BUILD SUCCESSFUL in 4m32s, APKs in app/build/outputs/apk/debug.

ISC-2: :shared module builds; :app:assembleDebug green with shared dep (BUILD SUCCESSFUL 4m4s).
ISC-3: ./gradlew :shared:compileKotlinIosSimulatorArm64 — BUILD SUCCESSFUL.
