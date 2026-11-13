package com.ariai.app.ui.theme

import androidx.compose.ui.graphics.Color

// Deep teal is AriAI's primary action color; mint and coral are reserved for soft states.
val AriAccent = Color(0xFF0E6470)
val AriAccentLight = Color(0xFF9DD8D0)
val AriAccentDark = Color(0xFF094B53)
val AriAccentSoft = Color(0xFFDDF2EE)
val AriAccentGreen = Color(0xFF26725C)
val AriAccentGreenSoft = Color(0xFFDDF2E9)

// Menu accent stops sampled from the supplied Mi Mover screenshot (1220 x 2712 JPEG).
// Each is the median RGB from an unobstructed 90 x 60 px patch near the top of its card:
// mint left/right at (140, 1940)/(460, 1940), blue left/right at (710, 1940)/(1020, 1940).
val AriMenuMintStart = Color(0xFF54DEBD)
val AriMenuMintEnd = Color(0xFF59E0BF)
val AriMenuBlueStart = Color(0xFF63B3FE)
val AriMenuBlueEnd = Color(0xFF64BCFD)
// Sampled dark label/foreground color from the same reference, used on the bright menu accents.
val AriMenuInk = Color(0xFF333333)

val AriError = Color(0xFFAD4A42)
val AriErrorSoft = Color(0xFFFBE6E2)

// Soft brand colours for providers and tags (kept light, like the reference).
val AriOpenAI = Color(0xFF2E9E84)
val AriGemini = Color(0xFF5B82C9)
val AriAnthropic = Color(0xFFC9785A)
val AriOllama = Color(0xFF4A4A55)
val AriCustom = Color(0xFF8C6FB5)
val AriTagAmberBg = Color(0xFFF4E2B8)
val AriTagAmberFg = Color(0xFF785D2C)
