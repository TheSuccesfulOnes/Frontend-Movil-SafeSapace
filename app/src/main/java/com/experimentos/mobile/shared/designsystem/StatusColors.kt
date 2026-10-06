package com.experimentos.mobile.shared.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val SuccessOnLight = Color(0xFF267341)
private val SuccessOnDark = Color(0xFF83D69D)

/** Semantic success color with sufficient contrast on either themed surface. */
val ColorScheme.success: Color
    get() = if (surface.luminance() < 0.5f) SuccessOnDark else SuccessOnLight
