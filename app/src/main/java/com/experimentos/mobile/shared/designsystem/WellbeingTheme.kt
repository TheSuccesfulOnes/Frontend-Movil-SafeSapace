package com.experimentos.mobile.shared.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared visual tokens aligned with the web admin panel. */
val AdminDark = Color(0xFF243432)
val AdminInk = Color(0xFF16211F)
val AdminMuted = Color(0xFF73807B)
val AdminPaper = Color(0xFFF5F6F1)
val AdminPanel = Color(0xFFFFFFFF)
val AdminLine = Color(0xFFE1E5DE)
val AdminBlue = Color(0xFFB9D8E5)
val AdminCoral = Color(0xFFEE8E79)
val AdminGold = Color(0xFFE9C36C)
val AdminSage = Color(0xFFA7C4A5)

private val LightColors = lightColorScheme(
    primary = AdminDark,
    onPrimary = Color.White,
    primaryContainer = AdminSage,
    onPrimaryContainer = AdminDark,
    secondary = AdminCoral,
    onSecondary = AdminDark,
    secondaryContainer = AdminBlue,
    onSecondaryContainer = AdminDark,
    tertiary = AdminGold,
    background = AdminPaper,
    onBackground = AdminInk,
    surface = AdminPanel,
    onSurface = AdminInk,
    surfaceVariant = Color(0xFFEEF1EC),
    onSurfaceVariant = Color(0xFF5F6D66),
    outline = Color(0xFFC9D0C8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA7D0B0),
    onPrimary = AdminDark,
    primaryContainer = Color(0xFF3D5745),
    onPrimaryContainer = Color(0xFFE5F1E5),
    secondary = Color(0xFFFFB09D),
    onSecondary = AdminDark,
    secondaryContainer = Color(0xFF3A4C50),
    onSecondaryContainer = Color(0xFFD9EEF5),
    tertiary = Color(0xFFF5D98D),
    background = Color(0xFF18211F),
    onBackground = Color(0xFFE6ECE8),
    surface = Color(0xFF202B28),
    onSurface = Color(0xFFE6ECE8),
    surfaceVariant = Color(0xFF2B3935),
    onSurfaceVariant = Color(0xFFB7C4BD),
    outline = Color(0xFF61716A),
)

private val WellbeingShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private val WellbeingTypography = Typography(
    bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        lineHeight = 24.sp,
    ),
    bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(
        fontFamily = FontFamily.SansSerif,
        lineHeight = 21.sp,
    ),
    titleLarge = androidx.compose.material3.Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
    ),
)

@Composable
fun WellbeingTheme(theme: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (theme.equals("DARK", ignoreCase = true)) DarkColors else LightColors,
        shapes = WellbeingShapes,
        typography = WellbeingTypography,
        content = content,
    )
}
