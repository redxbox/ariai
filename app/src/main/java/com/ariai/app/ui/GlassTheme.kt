package com.ariai.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AriAi glass visual system.
 *
 * The visual language is deliberately restrained: near-white ice glass,
 * charcoal typography/icons, hairline borders and soft elevation.
 */
object AriGlass {
    val Background = Color(0xFFFAFAFC)
    val Surface = Color(0xD9FFFFFF)
    val SurfaceStrong = Color(0xF2FFFFFF)
    val Stroke = Color(0x66FFFFFF)
    val StrokeDark = Color(0x14000000)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF6B6B72)
    val Accent = Color(0xFF171717)
    val Accent2 = Color(0xFF3A3A40)
    val AccentSoft = Color(0x10000000)
    val Highlight = Color(0xBFFFFFFF)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    radius: Int = 22,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)
    val surface = if (emphasized) AriGlass.SurfaceStrong else AriGlass.Surface

    var m = modifier
        .shadow(
            elevation = if (emphasized) 10.dp else 6.dp,
            shape = shape,
            clip = false
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (emphasized) .96f else .88f),
                    surface
                )
            )
        )
        .border(BorderStroke(1.dp, AriGlass.StrokeDark), shape)
        .border(BorderStroke(1.dp, AriGlass.Stroke), shape)
        .padding(16.dp)

    if (onClick != null) m = m.clickable(onClick = onClick)

    Column(m) { content() }
}

@Composable
fun GlassModeCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassSurface(modifier, radius = 20, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(AriGlass.AccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = AriGlass.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AriGlass.Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = AriGlass.Muted, fontSize = 11.sp)
            }
            Text("›", color = AriGlass.Muted, fontSize = 20.sp)
        }
    }
}
