package com.makd.afinity.shared

// Kotlin/Native reports whether the binary was compiled in debug mode — true for debug
// framework builds, false for release. Self-contained, no gradle/BuildConfig needed (#102).
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
actual val isDebugBuild: Boolean = kotlin.native.Platform.isDebugBinary
