package com.experimentos.mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.toArgb
import com.experimentos.mobile.shared.designsystem.WellbeingTheme
import com.experimentos.mobile.shared.presentation.AppLanguage
import com.experimentos.mobile.shared.presentation.AppStrings
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.navigation.AppNavHost
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the platform-compatible splash screen before the activity is created.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val container = (application as WellbeingApplication).appContainer
        setContent {
            val sessionStore = container.sessionStore
            val session by sessionStore.session.collectAsStateWithLifecycle(initialValue = null)
            val theme by container.appearanceStore.theme.collectAsStateWithLifecycle()
            val language by container.appearanceStore.language.collectAsStateWithLifecycle()
            LaunchedEffect(session?.accountKey) {
                container.appearanceStore.activateAccount(session?.accountKey)
            }
            val appLanguage = if (language == AppLanguage.ENGLISH.code) {
                AppLanguage.ENGLISH
            } else {
                AppLanguage.SPANISH
            }

            CompositionLocalProvider(LocalAppStrings provides AppStrings(appLanguage)) {
                WellbeingTheme(theme = theme) {
                    val colorScheme = MaterialTheme.colorScheme
                    val isDarkTheme = theme.equals("DARK", ignoreCase = true)
                    SideEffect {
                        window.statusBarColor = colorScheme.surface.toArgb()
                        window.navigationBarColor = colorScheme.background.toArgb()
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !isDarkTheme
                            isAppearanceLightNavigationBars = !isDarkTheme
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
                    }
                    AppNavHost(
                        container = container,
                        session = session,
                    )
                }
            }
        }
    }
}
