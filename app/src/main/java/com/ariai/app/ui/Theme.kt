package com.ariai.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AriPalette(
    val page: Color,
    val card: Color,
    val ink: Color,
    val mute: Color,
    val accent: Color,
    val accentSoft: Color,
    val chip: Color,
    val dangerBg: Color,
    val dangerInk: Color,
    val link: Color,
    val section: Color
)

private val LightPal = AriPalette(
    page = Color(0xFFF7F6FB),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF161221),
    mute = Color(0xFF6F6A80),
    accent = Color(0xFF5B4FE8),
    accentSoft = Color(0xFFEEEDFB),
    chip = Color(0xFFF0EEF6),
    dangerBg = Color(0xFFFFE8EA),
    dangerInk = Color(0xFFB42318),
    link = Color(0xFF5B4FE8),
    section = Color(0xFF5B4FE8)
)

private val DarkPal = AriPalette(
    page = Color(0xFF0F0D16),
    card = Color(0xFF1A1724),
    ink = Color(0xFFF4F1FF),
    mute = Color(0xFFA39BB8),
    accent = Color(0xFF8B84F8),
    accentSoft = Color(0xFF2A2640),
    chip = Color(0xFF232033),
    dangerBg = Color(0xFF3B1520),
    dangerInk = Color(0xFFFF8FAB),
    link = Color(0xFFB4A8FF),
    section = Color(0xFFB4A8FF)
)

val LocalAri = staticCompositionLocalOf { LightPal }

val Page: Color @Composable get() = LocalAri.current.page
val CardBg: Color @Composable get() = LocalAri.current.card
val Ink: Color @Composable get() = LocalAri.current.ink
val Mute: Color @Composable get() = LocalAri.current.mute
val Accent: Color @Composable get() = LocalAri.current.accent
val AccentSoft: Color @Composable get() = LocalAri.current.accentSoft
val Chip: Color @Composable get() = LocalAri.current.chip
val DangerBg: Color @Composable get() = LocalAri.current.dangerBg
val DangerInk: Color @Composable get() = LocalAri.current.dangerInk
val Link: Color @Composable get() = LocalAri.current.link
val Section: Color @Composable get() = LocalAri.current.section

@Composable
fun AriAiTheme(mode: String = "System", content: @Composable () -> Unit) {
    val dark = when (mode) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    val pal = if (dark) DarkPal else LightPal
    CompositionLocalProvider(LocalAri provides pal) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(
                primary = pal.accent, background = pal.page, surface = pal.card,
                onBackground = pal.ink, onSurface = pal.ink
            ) else lightColorScheme(
                primary = pal.accent, background = pal.page, surface = pal.card,
                onBackground = pal.ink, onSurface = pal.ink
            ),
            content = content
        )
    }
}
