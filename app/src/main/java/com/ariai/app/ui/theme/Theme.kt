package com.ariai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFB7A6FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF2A1B7A),
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = androidx.compose.ui.graphics.Color(0xFF121216),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1B22),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E9),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E9)
)

// Light palette: soft lavender-gray canvas, white raised surfaces, violet accent.
private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6C4DFF),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = androidx.compose.ui.graphics.Color(0xFFF7F6FB),
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEFEDF6),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    outline = androidx.compose.ui.graphics.Color(0x1F000000)
)

@Composable
fun AriAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
