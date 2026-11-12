package com.ariai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AriAccentLight,
    onPrimary = Color(0xFF073E42),
    primaryContainer = Color(0xFF17484D),
    onPrimaryContainer = Color(0xFFD2ECE7),
    secondary = Color(0xFFA6D9C7),
    onSecondary = Color(0xFF123C32),
    secondaryContainer = Color(0xFF244A40),
    onSecondaryContainer = Color(0xFFD7EEE5),
    tertiary = Color(0xFFB5DBA8),
    onTertiary = Color(0xFF233B1D),
    tertiaryContainer = Color(0xFF354D2E),
    onTertiaryContainer = Color(0xFFD0E8C6),
    background = DarkAriPalette.paper,
    surface = DarkAriPalette.card,
    surfaceVariant = DarkAriPalette.tint,
    onBackground = DarkAriPalette.ink,
    onSurface = DarkAriPalette.ink,
    onSurfaceVariant = DarkAriPalette.muted,
    outline = DarkAriPalette.line,
    outlineVariant = DarkAriPalette.fold,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = AriAccent,
    onPrimary = Color.White,
    primaryContainer = AriAccentSoft,
    onPrimaryContainer = AriAccentDark,
    secondary = AriAccentGreen,
    onSecondary = Color.White,
    secondaryContainer = AriAccentGreenSoft,
    onSecondaryContainer = AriAccentGreen,
    tertiary = AriAccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AriAccentGreenSoft,
    onTertiaryContainer = AriAccentGreen,
    background = LightAriPalette.paper,
    surface = LightAriPalette.card,
    surfaceVariant = LightAriPalette.tint,
    onBackground = LightAriPalette.ink,
    onSurface = LightAriPalette.ink,
    onSurfaceVariant = LightAriPalette.muted,
    outline = LightAriPalette.line,
    outlineVariant = LightAriPalette.fold,
    error = AriError
)

// A softer, more continuous corner rhythm for AriAI's main surfaces and controls.
private val AriShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(38.dp)
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
    // Dynamic accents remain user-controlled; AriAI keeps neutral paper and mint-tinted surfaces.
    val colorScheme = baseScheme.copy(
        background = palette.paper,
        surface = palette.card,
        surfaceVariant = palette.tint,
        onBackground = palette.ink,
        onSurface = palette.ink,
        onSurfaceVariant = palette.muted,
        outline = palette.line,
        outlineVariant = palette.fold
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
            shapes = AriShapes,
            content = content
        )
    }
}
