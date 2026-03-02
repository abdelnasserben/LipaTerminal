package com.kori.terminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    secondary = SignalGreen,
    tertiary = SignalGreen,
    background = NightBackground,
    surface = SurfacePrimary,
    surfaceVariant = SurfaceSecondary,
    onPrimary = AppWhite,
    onSecondary = AppWhite,
    onTertiary = AppWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = AppWhite
)

@Composable
fun KoriTerminalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
