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
    // Soft lavender paper and near-white cards, sampled from the supplied RikkaHub references.
    paper = Color(0xFFF7F6FC),
    card = Color(0xFFFCFBFF),
    ink = Color(0xFF26262D),
    muted = Color(0xFF747681),
    line = Color(0xFFE7E5EE),
    tint = Color(0xFFEBEAF3),
    fold = Color(0xFFDAD9E4)
)

val DarkAriPalette = AriPalette(
    paper = Color(0xFF111319),
    card = Color(0xFF1A1D25),
    ink = Color(0xFFF2F1F7),
    muted = Color(0xFFA4A3AE),
    line = Color(0xFF343642),
    tint = Color(0xFF272A34),
    fold = Color(0xFF3B3D49)
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
