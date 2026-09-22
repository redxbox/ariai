package com.ariai.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuroraBackdrop() {
    Box(Modifier.fillMaxSize().background(Page))
}

@Composable
fun Glass(
    modifier: Modifier = Modifier,
    radius: Int = 22,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)
    Column(
        modifier
            .clip(shape)
            .background(CardBg)
            .border(1.dp, Chip, shape)
            .padding(14.dp)
    ) { content() }
}

@Composable
fun Shutter(
    title: String,
    hint: String = "",
    icon: ImageVector? = null,
    openStart: Boolean = false,
    content: @Composable () -> Unit
) {
    var open by remember { mutableStateOf(openStart) }
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(CardBg.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .animateContentSize(tween(280, easing = FastOutSlowInEasing))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (open) "▾" else "▸", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (hint.isNotBlank()) Text(hint, color = Mute, fontSize = 12.sp)
            }
            if (icon != null) {
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(36.dp).clip(CircleShape).background(AccentSoft), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (open) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Chip))
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) { content() }
        } else {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                repeat(3) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp).height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Chip.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

fun glyphShape(kind: Int): Shape = when (kind % 4) {
    1 -> RoundedCornerShape(8.dp)
    3 -> RoundedCornerShape(20.dp)
    else -> CircleShape
}

@Composable
fun Glyph(
    icon: ImageVector,
    kind: Int,
    modifier: Modifier = Modifier.size(56.dp),
    onClick: () -> Unit
) {
    val diamond = kind % 4 == 2
    val shape = glyphShape(kind)
    Box(
        modifier
            .graphicsLayer { if (diamond) rotationZ = 45f }
            .clip(shape)
            .background(AccentSoft)
            .border(1.dp, Color.White.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null, tint = Accent,
            modifier = Modifier.size(22.dp).graphicsLayer { if (diamond) rotationZ = -45f }
        )
    }
}

@Composable
fun HubCard(title: String, hint: String, icon: ImageVector, kind: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (kind % 2 == 0) 28.dp else 14.dp)
    Column(
        modifier
            .clip(shape)
            .background(CardBg.copy(alpha = 0.75f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Glyph(icon, kind, Modifier.size(48.dp), onClick)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(hint, color = Mute, fontSize = 11.sp)
    }
}
