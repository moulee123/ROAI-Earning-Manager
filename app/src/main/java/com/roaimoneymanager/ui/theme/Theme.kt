package com.roaimoneymanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = Color(0xFF0B291A),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkGreenPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF3B4A41)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = CardWhite,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = GreenPrimary,
    background = BackgroundLight,
    surface = CardWhite,
    surfaceVariant = Color(0xFFEAEAEA),
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = GraySubtitle,
    outline = GrayBorder
)

@Composable
fun ROAIMoneyManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
