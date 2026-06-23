package com.makd.afinity.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkerParameters
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Static Koin graph check. Verifies that every constructor-based definition
 * (singleOf / viewModelOf / workerOf) across all app modules can be resolved.
 * Replaces Hilt's compile-time DI safety lost in the Koin migration (#99).
 *
 * extraTypes are the runtime-provided Android types Koin supplies at app start
 * (androidContext, workManagerFactory, SavedStateHandle for ViewModels).
 */
class KoinModulesTest {

    @org.koin.core.annotation.KoinExperimentalAPI
    @Test
    fun verifyKoinGraph() {
        module { includes(appModules) }
            .verify(
                extraTypes =
                    listOf(
                        Context::class,
                        Application::class,
                        WorkerParameters::class,
                        SavedStateHandle::class,
                        // Built inside builder-lambda defs; verify() reflects the ctor anyway.
                        org.jellyfin.sdk.JellyfinOptions.Builder::class,
                        org.jellyfin.sdk.JellyfinOptions::class,
                        java.io.File::class,
                        androidx.media3.datasource.cache.CacheEvictor::class,
                        ByteArray::class,
                        Boolean::class,
                        // Provider<T> cycle-breakers are supplied inline as Provider { get<T>() }
                        // in explicit defs; verify() can't see lambda args, so declare it here.
                        javax.inject.Provider::class,
                    )
            )
    }
}
