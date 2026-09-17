package com.experimentos.mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.experimentos.mobile.shared.designsystem.WellbeingTheme
import com.experimentos.mobile.shared.presentation.AppLanguage
import com.experimentos.mobile.shared.presentation.AppStrings
import com.experimentos.mobile.shared.presentation.LocalAppStrings
import com.experimentos.mobile.shared.navigation.AppNavHost
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay

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
                    var showLaunchOverlay by rememberSaveable { mutableStateOf(true) }
                    Box(Modifier.fillMaxSize()) {
                        AppNavHost(
                            container = container,
                            session = session,
                        )
                        if (showLaunchOverlay) {
                            SafeSpaceLaunchOverlay(
                                onFinished = { showLaunchOverlay = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A short branded transition that keeps the first frame intentional and calm. */
@Composable
private fun SafeSpaceLaunchOverlay(onFinished: () -> Unit) {
    val strings = LocalAppStrings.current
    val colors = MaterialTheme.colorScheme
    var isVisible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "launch-alpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "launch-scale",
    )
    val pulseTransition = rememberInfiniteTransition(label = "launch-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "launch-pulse-scale",
    )

    LaunchedEffect(Unit) {
        delay(780)
        isVisible = false
        delay(320)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.background, colors.surfaceVariant),
                ),
            )
            .graphicsLayer(alpha = alpha),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 48.dp, y = (-24).dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(colors.secondary.copy(alpha = 0.14f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-42).dp, y = 28.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.12f)),
        )

        Column(
            modifier = Modifier.graphicsLayer(
                scaleX = scale * pulse,
                scaleY = scale * pulse,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(184.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.safespace_logo),
                    contentDescription = strings.t("Logo de SafeSpace"),
                    modifier = Modifier.size(142.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = strings.t("SafeSpace"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.t("Bienestar laboral, en un espacio seguro."),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(92.dp)
                    .height(4.dp),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.16f),
            )
        }
    }
}
