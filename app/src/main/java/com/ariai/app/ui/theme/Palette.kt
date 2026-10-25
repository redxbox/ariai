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
    // Near-white page surfaces with only a subtle cool cast, matching the clean RikkaHub reference.
    paper = Color(0xFFFAFBFD),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF252A32),
    muted = Color(0xFF737B86),
    line = Color(0xFFE5E9EF),
    tint = Color(0xFFEEF3FA),
    fold = Color(0xFFE1E8F2)
)

val DarkAriPalette = AriPalette(
    paper = Color(0xFF101419),
    card = Color(0xFF191F26),
    ink = Color(0xFFF2F5F8),
    muted = Color(0xFFA2ABB5),
    line = Color(0xFF2B343E),
    tint = Color(0xFF222B34),
    fold = Color(0xFF343E48)
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
