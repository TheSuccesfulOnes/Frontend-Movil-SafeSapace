package com.experimentos.mobile.shared.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusColorsTest {
    @Test
    fun successHasSufficientContrastOnLightAndDarkSurfaces() {
        val schemes = listOf(
            lightColorScheme(surface = Color.White),
            darkColorScheme(surface = Color(0xFF202B28)),
        )
        schemes.forEach { scheme ->
            val foreground = scheme.success.luminance()
            val background = scheme.surface.luminance()
            val contrast = (maxOf(foreground, background) + 0.05f) /
                (minOf(foreground, background) + 0.05f)
            assertTrue("Success indicator should have at least 4.5:1 contrast", contrast >= 4.5f)
            assertTrue("Success indicator should be green", scheme.success.green > scheme.success.red)
        }
    }
}
