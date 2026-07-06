package com.makd.afinity.shared

// ponytail: hardcoded release-safe default (never masks backend failures). The androidLibrary
// KMP module does not generate BuildConfig, so we can't read BuildConfig.DEBUG here yet.
// Upgrade path (#102): once buildConfig is enabled on the shared androidLibrary block (Unit C),
// wire this to `BuildConfig.DEBUG` so local debug builds get sample fallbacks again.
actual val isDebugBuild: Boolean = false
