package com.makd.afinity

import android.app.Application
import com.makd.afinity.shared.identity.vault.androidVaultModule
import com.makd.afinity.shared.player.androidPlayerModule
import com.makd.afinity.shared.ui.detail.detailModule
import com.makd.afinity.shared.ui.history.historyModule
import com.makd.afinity.shared.ui.library.libraryModule
import com.makd.afinity.shared.ui.onboarding.onboardingModule
import com.makd.afinity.shared.ui.payments.paymentsModule
import com.makd.afinity.shared.ui.player.playerModule
import com.makd.afinity.shared.ui.search.searchModule
import com.makd.afinity.shared.ui.series.seriesModule
import com.makd.afinity.shared.ui.settings.settingsModule
import com.makd.afinity.shared.viewrr.viewrrModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class AfinityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AfinityApplication)
            modules(
                viewrrModule(baseUrl = "https://api.viewrr.stream"),
                searchModule, detailModule, libraryModule,
                playerModule, settingsModule, historyModule, seriesModule,
                onboardingModule, paymentsModule,
                androidPlayerModule, androidVaultModule,
            )
        }
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
