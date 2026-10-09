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
    val tint: Color
)

val LightAriPalette = AriPalette(
    paper = Color(0xFFF4F5FA),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF111111),
    muted = Color(0x8A111111),
    line = Color(0x14000000),
    tint = Color(0xFFF3F3F3)
)

val DarkAriPalette = AriPalette(
    paper = Color(0xFF000000),
    card = Color(0xFF161618),
    ink = Color(0xFFF2F2F2),
    muted = Color(0x8AF2F2F2),
    line = Color(0x1FFFFFFF),
    tint = Color(0xFF242426)
)

val LocalAriPalette = staticCompositionLocalOf { LightAriPalette }

/** Theme-aware neutrals. Read inside composables, so both themes work everywhere. */
val AriPaper: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.paper
val AriCard: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.card
val AriInk: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.ink
val AriMuted: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.muted
val AriLine: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.line
val AriTint: Color @Composable @ReadOnlyComposable get() = LocalAriPalette.current.tint
