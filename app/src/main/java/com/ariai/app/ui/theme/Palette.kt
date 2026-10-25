package com.ariai.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Neutral surfaces and text. Light and dark sets are switched by [AriAiTheme]. */
data class AriPalette(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val tint: Color,
    val fold: Color
)

val LightAriPalette = AriPalette(
    paper = Color(0xFFF3F1FB),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF20233A),
    muted = Color(0xFF747A91),
    line = Color(0xFFE4E1F0),
    tint = Color(0xFFECEAFF),
    fold = Color(0xFFDAD7EF)
)

val DarkAriPalette = AriPalette(
    paper = Color(0xFF0F1320),
    card = Color(0xFF181E2D),
    ink = Color(0xFFF2F3FC),
    muted = Color(0xFFA0A7BC),
    line = Color(0xFF2C3448),
    tint = Color(0xFF232A3D),
    fold = Color(0xFF343C52)
)

val LocalAriPalette = staticCompositionLocalOf { LightAriPalette }

/** Theme-aware neutrals. Read inside composables, so both themes work everywhere. */
val AriPaper: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.paper
val AriCard: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.card
val AriInk: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.ink
val AriMuted: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.muted
val AriLine: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.line
val AriFold: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.fold
val AriTint: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.tint
