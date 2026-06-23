package com.makd.afinity.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import androidx.work.WorkManager
import com.makd.afinity.BuildConfig
import com.makd.afinity.core.AppConstants
import com.makd.afinity.data.database.AfinityDatabase
import com.makd.afinity.data.database.DatabaseMigrations
import com.makd.afinity.data.manager.MediaChangeManager
import com.makd.afinity.data.manager.MediaRefreshBus
import com.makd.afinity.data.manager.PlaybackStateManager
import com.makd.afinity.data.manager.SessionManager
import com.makd.afinity.data.network.AudiobookshelfApiService
import com.makd.afinity.data.network.AudnexusApiService
import com.makd.afinity.data.network.JellyseerrApiService
import com.makd.afinity.data.network.MdbListApiService
import com.makd.afinity.data.network.OmdbApiService
import com.makd.afinity.data.network.TmdbApiService
import com.makd.afinity.data.repository.AppDataRepository
import com.makd.afinity.data.repository.AudiobookshelfRepository
import com.makd.afinity.data.repository.DatabaseRepository
import com.makd.afinity.data.repository.JellyfinRepository
import com.makd.afinity.data.repository.JellyseerrRepository
import com.makd.afinity.data.repository.PreferencesRepository
import com.makd.afinity.data.repository.SecurePreferencesRepository
import com.makd.afinity.data.repository.admin.AdminRepository
import com.makd.afinity.data.repository.admin.JellyfinAdminRepository
import com.makd.afinity.data.repository.audiobookshelf.AbsDownloadRepository
import com.makd.afinity.data.repository.audiobookshelf.AbsDownloadRepositoryImpl
import com.makd.afinity.data.repository.audiobookshelf.AudiobookshelfRepositoryImpl
import com.makd.afinity.data.repository.auth.AuthRepository
import com.makd.afinity.data.repository.auth.JellyfinAuthRepository
import com.makd.afinity.data.repository.download.DownloadRepository
import com.makd.afinity.data.repository.download.JellyfinDownloadRepository
import com.makd.afinity.data.repository.impl.DatabaseRepositoryImpl
import com.makd.afinity.data.repository.impl.JellyfinRepositoryImpl
import com.makd.afinity.data.repository.impl.PreferencesRepositoryImpl
import com.makd.afinity.data.repository.impl.SecurePreferencesRepositoryImpl
import com.makd.afinity.data.repository.jellyseerr.JellyseerrRepositoryImpl
import com.makd.afinity.data.repository.livetv.JellyfinLiveTvRepository
import com.makd.afinity.data.repository.livetv.LiveTvRepository
import com.makd.afinity.data.repository.media.JellyfinMediaRepository
import com.makd.afinity.data.repository.media.MediaRepository
import com.makd.afinity.data.repository.playback.JellyfinPlaybackRepository
import com.makd.afinity.data.repository.playback.PlaybackRepository
import com.makd.afinity.data.repository.segments.JellyfinSegmentsRepository
import com.makd.afinity.data.repository.segments.SegmentsRepository
import com.makd.afinity.data.repository.server.JellyfinServerRepository
import com.makd.afinity.data.repository.server.ServerAddressResolver
import com.makd.afinity.data.repository.server.ServerRepository
import com.makd.afinity.data.repository.syncplay.JellyfinSyncPlayRepository
import com.makd.afinity.data.repository.syncplay.SyncPlayRepository
import com.makd.afinity.data.repository.userdata.JellyfinUserDataRepository
import com.makd.afinity.data.repository.userdata.UserDataRepository
import com.makd.afinity.data.repository.watchlist.WatchlistRepository
import com.makd.afinity.data.repository.watchlist.WatchlistRepositoryImpl
import com.makd.afinity.data.sync.UserDataSyncScheduler
import com.makd.afinity.data.updater.GitHubApiService
import com.makd.afinity.ui.settings.servers.ServerManagementViewModel
import com.makd.afinity.data.workers.AbsMediaDownloadWorker
import com.makd.afinity.data.workers.ImageDownloadWorker
import com.makd.afinity.data.workers.MediaDownloadWorker
import com.makd.afinity.data.workers.SubtitleDownloadWorker
import com.makd.afinity.data.workers.TrickplayDownloadWorker
import com.makd.afinity.util.NetworkConnectivityMonitor
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.android.androidDevice
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import javax.inject.Provider
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber

// ── Koin qualifiers (replace Hilt @Qualifier annotation classes) ─────────────
val IMAGE_CLIENT = named("ImageClient")
private val GITHUB_CLIENT = named("GitHubClient")
private val DOWNLOAD_CLIENT = named("DownloadClient")
private val JELLYSEERR_CLIENT = named("JellyseerrClient")
private val AUDIOBOOKSHELF_CLIENT = named("AudiobookshelfClient")
private val AUDIOBOOKSHELF_RETROFIT = named("AudiobookshelfRetrofit")
private val TMDB_CLIENT = named("TmdbClient")
private val MDBLIST_CLIENT = named("MdbListClient")
private val OMDB_CLIENT = named("OmdbClient")
private val AUDNEXUS_CLIENT = named("AudnexusClient")
val APP_PREFERENCES = named("AppPreferences")
val USER_PREFERENCES = named("UserPreferences")
val SERVER_PREFERENCES = named("ServerPreferences")

// ── DataStore delegates (from PreferencesModule / RepositoryModule) ──────────
private val Context.appPreferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "app_preferences")
private val Context.userPreferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "user_preferences")
private val Context.serverPreferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "server_preferences")
private val Context.afinityDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "afinity_preferences")

private val jellyseerrJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
private val audiobookshelfJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
private val absTokenRefreshLock = Any()

// ── Network helpers (moved from NetworkModule) ───────────────────────────────
private class SeerrCookieJar(private val securePrefs: SecurePreferencesRepository) : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    private fun getSessionKey(host: String): String {
        val activeSession = securePrefs.getCachedJellyseerrCookie() ?: "anonymous"
        return "${host}_${activeSession.hashCode()}"
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = getSessionKey(url.host)
        val bucket = store.getOrPut(key) { mutableListOf() }
        synchronized(bucket) {
            for (c in cookies) {
                bucket.removeIf { it.name == c.name }
                if (c.value.isNotEmpty()) bucket.add(c)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val key = getSessionKey(url.host)
        val bucket = store[key] ?: return emptyList()
        synchronized(bucket) {
            return bucket.filter { !it.secure || url.scheme == "https" }
        }
    }

    fun preloadSessionCookie(url: HttpUrl, rawSetCookie: String?) {
        rawSetCookie ?: return
        Cookie.parse(url, rawSetCookie)?.let { saveFromResponse(url, listOf(it)) }
    }

    fun hasXsrfToken(host: String): Boolean {
        val key = getSessionKey(host)
        val bucket = store[key] ?: return false
        synchronized(bucket) {
            return bucket.any { it.name == "XSRF-TOKEN" }
        }
    }

    fun getXsrfToken(host: String): String? {
        val key = getSessionKey(host)
        val bucket = store[key] ?: return null
        synchronized(bucket) {
            return bucket.find { it.name == "XSRF-TOKEN" }?.value
        }
    }

    fun clear(host: String? = null) {
        if (host != null) store.remove(getSessionKey(host)) else store.clear()
    }
}

private fun normalizeJellyseerrUrl(raw: String?): String {
    if (raw.isNullOrBlank()) throw IOException("Seerr server URL not configured")
    var base = raw.trim()
    if (!base.startsWith("http://") && !base.startsWith("https://")) base = "http://$base"
    if (!base.endsWith("/")) base += "/"
    return base
}

private fun normalizeAudiobookshelfUrl(raw: String?): String {
    if (raw.isNullOrBlank()) throw IOException("Audiobookshelf server URL not configured")
    var base = raw.trim()
    if (!base.startsWith("http://") && !base.startsWith("https://")) base = "http://$base"
    if (!base.endsWith("/")) base += "/"
    return base
}

private fun buildAbsUrl(originalRequest: Request, baseUrl: String): HttpUrl? {
    return baseUrl
        .toHttpUrlOrNull()
        ?.newBuilder()
        ?.addPathSegments(originalRequest.url.encodedPath.removePrefix("/"))
        ?.apply {
            for (i in 0 until originalRequest.url.querySize) {
                addQueryParameter(
                    originalRequest.url.queryParameterName(i),
                    originalRequest.url.queryParameterValue(i),
                )
            }
        }
        ?.build()
}

private fun attemptAbsTokenRefresh(
    baseUrl: String,
    refreshToken: String,
    baseClient: OkHttpClient,
): Pair<String, String?>? {
    return try {
        val refreshUrl = "${baseUrl}auth/refresh"
        val refreshClient =
            baseClient
                .newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        val request =
            Request.Builder()
                .url(refreshUrl)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .addHeader("x-refresh-token", refreshToken)
                .addHeader("Content-Type", "application/json")
                .build()
        val response = refreshClient.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body.string()
            val json = Json { ignoreUnknownKeys = true }
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val userObj = jsonObj["user"]?.jsonObject
            val newAccessToken =
                userObj?.get("accessToken")?.jsonPrimitive?.content
                    ?: jsonObj["accessToken"]?.jsonPrimitive?.content
            val newRefreshToken =
                userObj?.get("refreshToken")?.jsonPrimitive?.content
                    ?: jsonObj["refreshToken"]?.jsonPrimitive?.content
            if (newAccessToken != null) Pair(newAccessToken, newRefreshToken) else null
        } else {
            response.close()
            null
        }
    } catch (e: Exception) {
        Timber.w(e, "ABS token refresh error")
        null
    }
}

private fun buildBaseOkHttpClient(
    context: Context,
    networkMonitor: NetworkConnectivityMonitor,
): OkHttpClient {
    val dispatcher =
        Dispatcher(
                Executors.newCachedThreadPool { runnable ->
                    Thread(runnable, "Jellyfin-OkHttp").apply { isDaemon = false }
                }
            )
            .apply {
                maxRequests = 45
                maxRequestsPerHost = 10
            }
    val connectionPool =
        ConnectionPool(maxIdleConnections = 10, keepAliveDuration = 30, timeUnit = TimeUnit.SECONDS)
    CoroutineScope(Dispatchers.Default).launch {
        merge(networkMonitor.networkSwitchEvents, networkMonitor.networkDropEvents).collect {
            connectionPool.evictAll()
        }
    }
    val builder =
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .cache(Cache(directory = File(context.cacheDir, "http_cache"), maxSize = 50L * 1024L * 1024L))
            .dns { hostname -> Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 } }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
    if (BuildConfig.DEBUG) {
        val loggingInterceptor =
            HttpLoggingInterceptor { message ->
                    val sanitized =
                        message.replace(
                            Regex("(?i)(api_key|token|accessToken)=[^&\\s]+"),
                            "$1=[REDACTED]",
                        )
                    if (sanitized.contains("ERROR") || sanitized.contains("FAILED")) {
                        Timber.tag("Jellyfin-HTTP").d(sanitized)
                    }
                }
                .apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("X-MediaBrowser-Token")
                    redactHeader("x-refresh-token")
                }
        builder.addInterceptor(loggingInterceptor)
    }
    return builder.build()
}

// ── Modules ──────────────────────────────────────────────────────────────────

@UnstableApi
val networkModule = module {
    single { ClientInfo(name = AppConstants.APP_NAME, version = AppConstants.VERSION_NAME) }
    single<DeviceInfo> { androidDevice(androidContext()) }
    single { buildBaseOkHttpClient(androidContext(), get()) }

    single(IMAGE_CLIENT) {
        val sessionManager = get<SessionManager>()
        get<OkHttpClient>()
            .newBuilder()
            .dispatcher(Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 16
            })
            .addInterceptor { chain ->
                val request = chain.request()
                val session = sessionManager.currentSession.value
                val token = session?.user?.accessToken
                val serverUrl = session?.serverUrl
                if (token != null && serverUrl != null) {
                    val serverHost = serverUrl.toHttpUrlOrNull()?.host
                    if (serverHost != null && request.url.host == serverHost) {
                        return@addInterceptor chain.proceed(
                            request.newBuilder()
                                .addHeader("Authorization", "MediaBrowser Token=$token")
                                .build()
                        )
                    }
                }
                chain.proceed(request)
            }
            .build()
    }

    single(GITHUB_CLIENT) {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    single(DOWNLOAD_CLIENT) {
        val context = androidContext()
        val networkMonitor = get<NetworkConnectivityMonitor>()
        val dispatcher =
            Dispatcher(
                    Executors.newCachedThreadPool { runnable ->
                        Thread(runnable, "Download-OkHttp").apply { isDaemon = false }
                    }
                )
                .apply {
                    maxRequests = 5
                    maxRequestsPerHost = 2
                }
        val connectionPool =
            ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 5, timeUnit = TimeUnit.MINUTES)
        CoroutineScope(Dispatchers.Default).launch {
            merge(networkMonitor.networkSwitchEvents, networkMonitor.networkDropEvents).collect {
                connectionPool.evictAll()
            }
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    single { OkHttpFactory(base = get()) }
    single {
        createJellyfin {
            this.context = androidContext()
            this.clientInfo = get()
            this.deviceInfo = get()
            this.apiClientFactory = get<OkHttpFactory>()
            this.socketConnectionFactory = get<OkHttpFactory>()
        }
    }
    single<ApiClient> { get<Jellyfin>().createApi() }

    single(JELLYSEERR_CLIENT) {
        val baseOkHttpClient = get<OkHttpClient>()
        val securePreferencesRepository = get<SecurePreferencesRepository>()
        val seerrCookieJar = SeerrCookieJar(securePreferencesRepository)
        val csrfSeedClient =
            baseOkHttpClient.newBuilder()
                .cookieJar(seerrCookieJar)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .build()
        baseOkHttpClient.newBuilder()
            .cookieJar(seerrCookieJar)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val savedUrl = securePreferencesRepository.getCachedJellyseerrServerUrl()
                val currentBaseUrl =
                    try {
                        if (!savedUrl.isNullOrBlank()) normalizeJellyseerrUrl(savedUrl) else null
                    } catch (e: Exception) {
                        null
                    }
                        ?: throw IOException(
                            "Jellyseerr server URL not configured. Please configure the server URL first."
                        )
                val baseHttpUrl =
                    currentBaseUrl.toHttpUrlOrNull()
                        ?: throw IOException("Failed to parse Jellyseerr URL")
                seerrCookieJar.preloadSessionCookie(
                    baseHttpUrl,
                    securePreferencesRepository.getCachedJellyseerrCookie(),
                )
                val newUrl =
                    baseHttpUrl.newBuilder()
                        .addPathSegments(originalRequest.url.encodedPath.removePrefix("/"))
                        .apply {
                            for (i in 0 until originalRequest.url.querySize) {
                                addQueryParameter(
                                    originalRequest.url.queryParameterName(i),
                                    originalRequest.url.queryParameterValue(i),
                                )
                            }
                        }
                        .build()
                val isMutating = originalRequest.method in listOf("POST", "PUT", "DELETE", "PATCH")
                if (isMutating && !seerrCookieJar.hasXsrfToken(baseHttpUrl.host)) {
                    for (url in listOf(currentBaseUrl)) {
                        try {
                            csrfSeedClient.newCall(Request.Builder().url(url).get().build())
                                .execute()
                                .close()
                            if (seerrCookieJar.hasXsrfToken(baseHttpUrl.host)) {
                                if (url != currentBaseUrl)
                                    securePreferencesRepository.updateCachedJellyseerrServerUrl(
                                        url.trimEnd('/')
                                    )
                                break
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Jellyseerr: CSRF seed failed for $url")
                        }
                    }
                }
                val newRequest =
                    originalRequest.newBuilder()
                        .url(newUrl)
                        .apply {
                            addHeader("Content-Type", "application/json")
                            seerrCookieJar.getXsrfToken(baseHttpUrl.host)?.let {
                                addHeader("XSRF-TOKEN", it)
                            }
                        }
                        .build()
                val response = chain.proceed(newRequest)
                if (response.code == 403) seerrCookieJar.clear(baseHttpUrl.host)
                response
            }
            .build()
    }

    single { provideJellyseerrRetrofit(get(JELLYSEERR_CLIENT)) }
    single<JellyseerrApiService> { get<Retrofit>().create(JellyseerrApiService::class.java) }

    single(AUDIOBOOKSHELF_CLIENT) {
        val baseOkHttpClient = get<OkHttpClient>()
        val securePreferencesRepository = get<SecurePreferencesRepository>()
        baseOkHttpClient.newBuilder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val savedUrl = securePreferencesRepository.getCachedAudiobookshelfServerUrl()
                val currentBaseUrl =
                    try {
                        if (!savedUrl.isNullOrBlank()) normalizeAudiobookshelfUrl(savedUrl) else null
                    } catch (e: Exception) {
                        null
                    }
                        ?: throw IOException(
                            "Audiobookshelf server URL not configured. Please configure the server URL first."
                        )
                val newUrl =
                    buildAbsUrl(originalRequest, currentBaseUrl)
                        ?: throw IOException("Failed to build Audiobookshelf URL")
                val token = securePreferencesRepository.getCachedAudiobookshelfToken()
                val newRequest =
                    originalRequest.newBuilder()
                        .url(newUrl)
                        .apply {
                            token?.let { addHeader("Authorization", "Bearer $it") }
                            addHeader("Content-Type", "application/json")
                            addHeader("x-return-tokens", "true")
                        }
                        .build()
                val response = chain.proceed(newRequest)
                if (response.code == 401 && !newUrl.encodedPath.contains("auth")) {
                    response.close()
                    synchronized(absTokenRefreshLock) {
                        val currentToken = securePreferencesRepository.getCachedAudiobookshelfToken()
                        if (currentToken != null && currentToken != token) {
                            return@addInterceptor chain.proceed(
                                originalRequest.newBuilder()
                                    .url(newUrl)
                                    .header("Authorization", "Bearer $currentToken")
                                    .header("Content-Type", "application/json")
                                    .header("x-return-tokens", "true")
                                    .build()
                            )
                        }
                        val refreshToken =
                            securePreferencesRepository.getCachedAudiobookshelfRefreshToken()
                        if (refreshToken != null) {
                            val refreshResult =
                                attemptAbsTokenRefresh(currentBaseUrl, refreshToken, baseOkHttpClient)
                            if (refreshResult != null) {
                                securePreferencesRepository.updateCachedAudiobookshelfTokens(
                                    refreshResult.first,
                                    refreshResult.second,
                                )
                                return@addInterceptor chain.proceed(
                                    originalRequest.newBuilder()
                                        .url(newUrl)
                                        .header("Authorization", "Bearer ${refreshResult.first}")
                                        .header("Content-Type", "application/json")
                                        .header("x-return-tokens", "true")
                                        .build()
                                )
                            } else {
                                securePreferencesRepository.updateCachedAudiobookshelfTokens(
                                    token ?: "",
                                    null,
                                )
                                securePreferencesRepository.onAbsAuthInvalidated?.invoke()
                            }
                        }
                    }
                    securePreferencesRepository.onAbsAuthInvalidated?.invoke()
                    return@addInterceptor Response.Builder()
                        .request(originalRequest)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized - token refresh failed")
                        .body("".toResponseBody(null))
                        .build()
                }
                response
            }
            .build()
    }

    single(AUDIOBOOKSHELF_RETROFIT) {
        Retrofit.Builder()
            .baseUrl("http://placeholder.audiobookshelf/")
            .client(get(AUDIOBOOKSHELF_CLIENT))
            .addConverterFactory(audiobookshelfJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<AudiobookshelfApiService> {
        get<Retrofit>(AUDIOBOOKSHELF_RETROFIT).create(AudiobookshelfApiService::class.java)
    }

    single(TMDB_CLIENT) {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .client(get<OkHttpClient>().newBuilder().connectTimeout(5, TimeUnit.SECONDS).build())
            .addConverterFactory(jellyseerrJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<TmdbApiService> { get<Retrofit>(TMDB_CLIENT).create(TmdbApiService::class.java) }

    single(MDBLIST_CLIENT) {
        Retrofit.Builder()
            .baseUrl("https://api.mdblist.com/")
            .client(get())
            .addConverterFactory(jellyseerrJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<MdbListApiService> { get<Retrofit>(MDBLIST_CLIENT).create(MdbListApiService::class.java) }

    single(OMDB_CLIENT) {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<OmdbApiService> { get<Retrofit>(OMDB_CLIENT).create(OmdbApiService::class.java) }

    single(AUDNEXUS_CLIENT) {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        Retrofit.Builder()
            .baseUrl("https://api.audnex.us/")
            .client(
                get<OkHttpClient>().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .callTimeout(15, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<AudnexusApiService> { get<Retrofit>(AUDNEXUS_CLIENT).create(AudnexusApiService::class.java) }

    single { GitHubApiService(get(GITHUB_CLIENT)) }
}

// Download workers need the tuned DOWNLOAD_CLIENT OkHttpClient (unlimited callTimeout),
// so they can't use plain workerOf (no named-qualifier support). get() resolves Context +
// WorkerParameters from the factory params, then graph deps by type, okHttpClient by qualifier.
@UnstableApi
val downloadWorkerModule = module {
    worker { ImageDownloadWorker(get(), get(), get(), get(), get(), get(DOWNLOAD_CLIENT)) }
    worker {
        MediaDownloadWorker(get(), get(), get(), get(), get(), get(), get(), get(), get(DOWNLOAD_CLIENT))
    }
    worker { SubtitleDownloadWorker(get(), get(), get(), get(), get(), get(DOWNLOAD_CLIENT)) }
    worker { TrickplayDownloadWorker(get(), get(), get(), get(), get(), get(DOWNLOAD_CLIENT)) }
    worker {
        AbsMediaDownloadWorker(get(), get(), get(), get(), get(), get(), get(), get(), get(DOWNLOAD_CLIENT))
    }
}

// ViewModels with Provider<T> cycle-breakers can't use viewModelOf (no Provider support).
val cycleViewModelModule = module {
    viewModel {
        ServerManagementViewModel(
            androidContext(), get(), get(), get(), get(), get(), get(), get(),
            Provider { get<JellyseerrRepository>() },
            Provider { get<AudiobookshelfRepository>() },
            Provider { get<JellyfinRepository>() },
        )
    }
}

private fun provideJellyseerrRetrofit(okHttpClient: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl("http://placeholder.jellyseerr/")
        .client(okHttpClient)
        .addConverterFactory(jellyseerrJson.asConverterFactory("application/json".toMediaType()))
        .build()

val databaseModule = module {
    single {
        Room.databaseBuilder(
                androidContext().applicationContext,
                AfinityDatabase::class.java,
                "afinity_database",
            )
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()
    }
    single { get<AfinityDatabase>().serverDao() }
    single { get<AfinityDatabase>().serverAddressDao() }
    single { get<AfinityDatabase>().userDao() }
    single { get<AfinityDatabase>().movieDao() }
    single { get<AfinityDatabase>().showDao() }
    single { get<AfinityDatabase>().seasonDao() }
    single { get<AfinityDatabase>().episodeDao() }
    single { get<AfinityDatabase>().sourceDao() }
    single { get<AfinityDatabase>().mediaStreamDao() }
    single { get<AfinityDatabase>().userDataDao() }
    single { get<AfinityDatabase>().serverDatabaseDao() }
    single { get<AfinityDatabase>().libraryCacheDao() }
    single { get<AfinityDatabase>().boxSetCacheDao() }
    single { get<AfinityDatabase>().itemMetadataCacheDao() }
    single { get<AfinityDatabase>().jellyfinStatsDao() }
    single { get<AfinityDatabase>().genreCacheDao() }
    single { get<AfinityDatabase>().studioCacheDao() }
    single { get<AfinityDatabase>().topPeopleDao() }
    single { get<AfinityDatabase>().personSectionDao() }
    single { get<AfinityDatabase>().movieSectionDao() }
    single { get<AfinityDatabase>().jellyseerrDao() }
    single { get<AfinityDatabase>().audiobookshelfDao() }
    single { get<AfinityDatabase>().absDownloadDao() }
    single { get<AfinityDatabase>().audibleRatingDao() }
}

val preferencesModule = module {
    single(APP_PREFERENCES) { androidContext().appPreferencesDataStore }
    single(USER_PREFERENCES) { androidContext().userPreferencesDataStore }
    single(SERVER_PREFERENCES) { androidContext().serverPreferencesDataStore }
    single<DataStore<Preferences>> { androidContext().afinityDataStore }
    single { WorkManager.getInstance(androidContext()) }
}

@UnstableApi
val playerModule = module {
    singleOf(::PlaybackStateManager)
    single<DatabaseProvider> { StandaloneDatabaseProvider(androidContext()) }
    single {
        SimpleCache(
            File(androidContext().cacheDir, "exo_media_cache"),
            LeastRecentlyUsedCacheEvictor(1024L * 1024L * 1024L),
            get<DatabaseProvider>(),
        )
    }
}

// Repository @Binds → Koin bind(); standalone @Inject managers → singleOf
val repositoryModule = module {
    singleOf(::JellyfinAuthRepository) { bind<AuthRepository>() }
    // Provider<T> breaks SessionManager <-> ServerRepository cycle; inline Provider defers resolution.
    single<ServerRepository> {
        JellyfinServerRepository(
            get(), get(), Provider { get<SessionManager>() }, get(), get(),
            Provider { get<ServerAddressResolver>() },
        )
    }
    singleOf(::JellyfinMediaRepository) { bind<MediaRepository>() }
    singleOf(::JellyfinUserDataRepository) { bind<UserDataRepository>() }
    singleOf(::JellyfinPlaybackRepository) { bind<PlaybackRepository>() }
    singleOf(::JellyfinRepositoryImpl) { bind<JellyfinRepository>() }
    // Provider<SessionManager> breaks DatabaseRepository <-> SessionManager cycle.
    single<DatabaseRepository> {
        DatabaseRepositoryImpl(
            Provider { get<SessionManager>() },
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get(APP_PREFERENCES)) }
    singleOf(::JellyfinDownloadRepository) { bind<DownloadRepository>() }
    singleOf(::JellyfinSyncPlayRepository) { bind<SyncPlayRepository>() }
    singleOf(::SecurePreferencesRepositoryImpl) { bind<SecurePreferencesRepository>() }
    singleOf(::JellyfinAdminRepository) { bind<AdminRepository>() }
    singleOf(::JellyfinLiveTvRepository) { bind<LiveTvRepository>() }
    singleOf(::JellyfinSegmentsRepository) { bind<SegmentsRepository>() }
    singleOf(::JellyseerrRepositoryImpl) { bind<JellyseerrRepository>() }
    singleOf(::AudiobookshelfRepositoryImpl) { bind<AudiobookshelfRepository>() }
    singleOf(::AbsDownloadRepositoryImpl) { bind<AbsDownloadRepository>() }
    single<WatchlistRepository> { WatchlistRepositoryImpl(get(), get()) }
    singleOf(::AppDataRepository)
}

val appModules: List<org.koin.core.module.Module> =
    listOf(
        networkModule,
        databaseModule,
        preferencesModule,
        playerModule,
        repositoryModule,
        managerModule,
        viewModelModule,
        workerModule,
        downloadWorkerModule,
        cycleViewModelModule,
    )
