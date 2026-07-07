package com.luis.tramo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TramoPine,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = TramoPineContainer,
    onPrimaryContainer = TramoOnPineContainer,
    secondary = Color(0xFF4E6A6D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E4E6),
    onSecondaryContainer = Color(0xFF0B1F21),
    tertiary = Color(0xFF46685F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8E9DB),
    onTertiaryContainer = Color(0xFF052017),
    background = TramoBackgroundLight,
    onBackground = TramoInk,
    surface = TramoBackgroundLight,
    onSurface = TramoInk,
    surfaceVariant = Color(0xFFDBE4E3),
    onSurfaceVariant = Color(0xFF3F4A4B),
    outline = Color(0xFF6F7A79),
    outlineVariant = Color(0xFFBFC8C7),
    error = TramoError,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DBD7),
    onErrorContainer = Color(0xFF3B0906)
)

private val DarkColors = darkColorScheme(
    primary = TramoPineDark,
    onPrimary = Color(0xFF00363B),
    primaryContainer = TramoPineContainerDark,
    onPrimaryContainer = TramoPineContainer,
    secondary = Color(0xFFB3CCCE),
    onSecondary = Color(0xFF1E3437),
    secondaryContainer = Color(0xFF354B4E),
    onSecondaryContainer = Color(0xFFD1E4E6),
    tertiary = Color(0xFFACCFC0),
    onTertiary = Color(0xFF183730),
    tertiaryContainer = Color(0xFF2F4E45),
    onTertiaryContainer = Color(0xFFC8E9DB),
    background = TramoBackgroundDark,
    onBackground = Color(0xFFE1E6E5),
    surface = TramoBackgroundDark,
    onSurface = Color(0xFFE1E6E5),
    surfaceVariant = Color(0xFF3F4A4B),
    onSurfaceVariant = Color(0xFFBEC8C7),
    outline = Color(0xFF899392),
    outlineVariant = Color(0xFF3F4A4B),
    error = Color(0xFFE69B93),
    onError = Color(0xFF5F1610),
    errorContainer = Color(0xFF8A2F28),
    onErrorContainer = Color(0xFFF7DBD7)
)

/** Tramo's extra theme values that don't fit a Material [androidx.compose.material3.ColorScheme] slot. */
@Immutable
data class TramoExtendedColors(val progress: Color)

private val LocalTramoColors = staticCompositionLocalOf { TramoExtendedColors(progress = TramoProgress) }

/** Accessor for Tramo-specific theme values, e.g. `TramoTheme.progress`. */
object TramoTheme {
    val progress: Color
        @Composable @ReadOnlyComposable get() = LocalTramoColors.current.progress
}

@Composable
fun TramoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Fixed brand palette — dynamic color (Material You) is intentionally disabled.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalTramoColors provides TramoExtendedColors(progress = TramoProgress)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
