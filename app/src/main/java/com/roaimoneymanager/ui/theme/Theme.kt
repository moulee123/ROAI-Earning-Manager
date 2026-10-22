package com.roaimoneymanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VioletAccent,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = CyanAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = CardWhite,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = VioletAccent,
    background = BackgroundLight,
    surface = CardWhite,
    surfaceVariant = Color(0xFFE8EEF5),
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
