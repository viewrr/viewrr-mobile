package com.makd.afinity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.makd.afinity.data.manager.OfflineModeManager
import com.makd.afinity.data.repository.PreferencesRepository
import com.makd.afinity.data.updater.UpdateManager
import com.makd.afinity.data.updater.UpdateScheduler
import com.makd.afinity.data.updater.models.UpdateCheckFrequency
import com.makd.afinity.data.updater.notification.UpdateNotificationManager
import com.makd.afinity.navigation.MainNavigation
import com.makd.afinity.ui.components.AfinitySplashScreen
import com.makd.afinity.ui.login.LoginScreen
import com.makd.afinity.ui.theme.AFinityTheme
import com.makd.afinity.ui.theme.ThemeMode
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : ComponentActivity() {
    private val preferencesRepository: PreferencesRepository by inject()
    private val updateManager: UpdateManager by inject()
    private val offlineModeManager: OfflineModeManager by inject()
    private val updateScheduler: UpdateScheduler by inject()
    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.authenticationState.value == AuthenticationState.Loading
        }

        setContent {
            @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
            val windowSize = calculateWindowSizeClass(this)
            val themeMode by
                preferencesRepository.getThemeModeFlow().collectAsState(initial = "SYSTEM")
            val dynamicColors by
                preferencesRepository.getDynamicColorsFlow().collectAsState(initial = true)

            val appFont by
                preferencesRepository.getAppFontFlow().collectAsState(initial = "DEFAULT")
            AFinityTheme(themeMode = themeMode, dynamicColor = dynamicColors, appFont = appFont) {
                val windowInsetsController =
                    WindowCompat.getInsetsController(window, window.decorView)
                val mode = ThemeMode.fromString(themeMode)
                val systemInDarkTheme = isSystemInDarkTheme()

                val isLightTheme =
                    when (mode) {
                        ThemeMode.SYSTEM -> !systemInDarkTheme
                        ThemeMode.LIGHT -> true
                        ThemeMode.DARK,
                        ThemeMode.AMOLED -> false
                    }

                windowInsetsController.isAppearanceLightStatusBars = isLightTheme
                windowInsetsController.isAppearanceLightNavigationBars = isLightTheme

                // Migration (#99/#101): render the new commonMain CMP app shell (bottom-nav:
                // Home/Search/Library) over the viewrr client. Old Jellyfin MainContent stays
                // in the file for reference/rollback.
                com.makd.afinity.shared.ui.App()
            }
        }
        lifecycleScope.launch {
            val frequency = preferencesRepository.getUpdateCheckFrequency()
            if (frequency == UpdateCheckFrequency.ON_APP_OPEN.hours) {
                updateManager.checkForUpdates()
            }
        }

        if (intent.getBooleanExtra(UpdateNotificationManager.EXTRA_AUTO_DOWNLOAD_UPDATE, false)) {
            lifecycleScope.launch { updateManager.triggerAutoDownload() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(UpdateNotificationManager.EXTRA_AUTO_DOWNLOAD_UPDATE, false)) {
            lifecycleScope.launch { updateManager.triggerAutoDownload() }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel(),
    updateManager: UpdateManager,
    offlineModeManager: OfflineModeManager,
    widthSizeClass: WindowWidthSizeClass,
) {
    val authState by viewModel.authenticationState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = authState,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "AuthTransition",
    ) { state ->
        when (state) {
            AuthenticationState.Loading -> {
                AfinitySplashScreen(
                    statusText = stringResource(R.string.splash_status_authenticating),
                    modifier = modifier,
                )
            }

            AuthenticationState.Authenticated -> {
                MainNavigation(
                    modifier = modifier,
                    updateManager = updateManager,
                    offlineModeManager = offlineModeManager,
                    widthSizeClass = widthSizeClass,
                )
            }

            AuthenticationState.NotAuthenticated -> {
                LoginScreen(
                    onLoginSuccess = {},
                    modifier = modifier,
                    widthSizeClass = widthSizeClass,
                )
            }
        }
    }
}

sealed class AuthenticationState {
    object Loading : AuthenticationState()

    object Authenticated : AuthenticationState()

    object NotAuthenticated : AuthenticationState()
}
