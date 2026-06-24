package com.makd.afinity

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.makd.afinity.cast.CastManager
import com.makd.afinity.data.repository.PreferencesRepository
import com.makd.afinity.data.updater.UpdateScheduler
import com.makd.afinity.data.updater.models.UpdateCheckFrequency
import com.makd.afinity.di.IMAGE_CLIENT
import com.makd.afinity.di.appModules
import com.makd.afinity.shared.viewrr.viewrrModule
import com.makd.afinity.util.logging.CrashFileExporter
import com.makd.afinity.util.logging.RingBufferTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class AfinityApplication : Application(), SingletonImageLoader.Factory {

    private val updateScheduler: UpdateScheduler by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val castManager: CastManager by inject()
    private val imageOkHttpClient: OkHttpClient by inject(IMAGE_CLIENT)

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var ringBufferTree: RingBufferTree? = null
        private set

    @Volatile private var imageCacheEnabled: Boolean = true
    @Volatile private var imageCacheSizeMb: Int = 512

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AfinityApplication)
            workManagerFactory()
            // #101: viewrr data layer. Dev base = emulator->host; token wiring lands with Keycloak.
            modules(appModules + viewrrModule(baseUrl = "http://10.0.2.2:8080") { null })
        }

        applicationScope.launch(Dispatchers.IO) {
            imageCacheEnabled = preferencesRepository.getImageCacheEnabled()
            imageCacheSizeMb = preferencesRepository.getImageCacheSizeMb()
        }

        val tree = RingBufferTree()
        ringBufferTree = tree
        Timber.plant(tree)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Afinity Application started")
        }

        Thread.setDefaultUncaughtExceptionHandler(
            CrashFileExporter(this, ringBufferTree, Thread.getDefaultUncaughtExceptionHandler())
        )

        castManager.initialize(this)

        applicationScope.launch {
            val frequency = preferencesRepository.getUpdateCheckFrequency()
            val checkFrequency = UpdateCheckFrequency.fromHours(frequency)
            updateScheduler.scheduleUpdateChecks(checkFrequency)
            Timber.d("Update scheduler initialized with frequency: ${checkFrequency.displayName}")
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val isCacheEnabled = imageCacheEnabled
        val cacheSizeMb = imageCacheSizeMb

        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { imageOkHttpClient },
                        cacheStrategy = { CacheControlCacheStrategy() },
                    )
                )
                add(SvgDecoder.Factory())
                add(AnimatedImageDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context)
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(if (isCacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(cacheSizeMb * 1024L * 1024L)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
