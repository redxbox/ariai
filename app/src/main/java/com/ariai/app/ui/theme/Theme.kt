package com.ariai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

// Calm blue/green controls sit over bright neutral surfaces in the light theme and charcoal in dark mode.
private val DarkColorScheme = darkColorScheme(
    primary = AriAccentLight,
    onPrimary = Color(0xFF17233C),
    secondary = Color(0xFF9FD4C9),
    tertiary = AriAccentGreen,
    background = DarkAriPalette.paper,
    surface = DarkAriPalette.paper,
    surfaceVariant = DarkAriPalette.tint,
    onBackground = DarkAriPalette.ink,
    onSurface = DarkAriPalette.ink,
    outline = DarkAriPalette.line
)

private val LightColorScheme = lightColorScheme(
    primary = AriAccent,
    onPrimary = Color.White,
    secondary = AriAccentGreen,
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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val palette = if (darkTheme) DarkAriPalette else LightAriPalette
    val dynamicScheme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor
    val baseScheme = when {
        dynamicScheme && darkTheme -> dynamicDarkColorScheme(context)
        dynamicScheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    // Keep AriAI's quiet paper surfaces while taking only Material You's accent colors.
    val colorScheme = baseScheme.copy(
        background = palette.paper,
        surface = palette.paper,
        surfaceVariant = palette.tint,
        onBackground = palette.ink,
        onSurface = palette.ink,
        outline = palette.line
    )
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

    CompositionLocalProvider(LocalAriPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
