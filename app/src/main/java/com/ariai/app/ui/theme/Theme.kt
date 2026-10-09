package com.ariai.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Monochrome: white and black, with one small accent used per screen.
private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFFBDBDBD),
    tertiary = AriAccent,
    background = DarkAriPalette.paper,
    surface = DarkAriPalette.paper,
    surfaceVariant = DarkAriPalette.tint,
    onBackground = DarkAriPalette.ink,
    onSurface = DarkAriPalette.ink,
    outline = DarkAriPalette.line
)

private val LightColorScheme = lightColorScheme(
    primary = LightAriPalette.ink,
    onPrimary = Color.White,
    secondary = Color(0xFF555555),
    tertiary = AriAccent,
    background = LightAriPalette.paper,
    surface = LightAriPalette.paper,
    surfaceVariant = LightAriPalette.tint,
    onBackground = LightAriPalette.ink,
    onSurface = LightAriPalette.ink,
    outline = LightAriPalette.line
)

@Composable
fun AriAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour is off: the palette must stay black and white.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAriPalette provides if (darkTheme) DarkAriPalette else LightAriPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
