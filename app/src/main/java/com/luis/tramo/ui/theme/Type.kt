package com.luis.tramo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Tabular (monospaced) figures. Merge into any counting/stat text so digits keep a fixed width and
 * numbers don't shift while the timer ticks or stats update.
 */
val TabularFigures = TextStyle(fontFeatureSettings = "tnum")
