package com.makd.afinity.shared

/**
 * Whether this is a debug/dev build. When true, UI layers may fall back to
 * [com.makd.afinity.shared.viewrr.SampleData] so screens render before the backend lands.
 *
 * In release this MUST be false so real backend failures surface as empty/error states
 * instead of fake "working" content that masks a broken integration (#102).
 *
 * Self-contained by design — no BuildConfig / gradle dependency, so it works regardless of
 * cross-module wiring.
 */
expect val isDebugBuild: Boolean
