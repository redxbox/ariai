package com.ariai.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

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
    // Neutral light surfaces, text and dividers sampled from the Mi Mover reference.
    paper = Color(0xFFFBFBFB),
    card = Color(0xFFFFFFFF),
    ink = AriMenuInk,
    muted = Color(0xFF757575),
    line = Color(0xFFDFE1E0),
    // Keep selected surfaces quiet: this is a 10% blend of the sampled mint over sampled paper.
    tint = AriMenuMintStart.copy(alpha = 0.10f).compositeOver(Color(0xFFFBFBFB)),
    fold = Color(0xFFDFE1E0)
)

val DarkAriPalette = AriPalette(
    paper = Color(0xFF11191B),
    card = Color(0xFF192426),
    ink = Color(0xFFF1F5F3),
    muted = Color(0xFFA2ADAB),
    line = Color(0xFF303D3E),
    tint = Color(0xFF203437),
    fold = Color(0xFF324548)
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
