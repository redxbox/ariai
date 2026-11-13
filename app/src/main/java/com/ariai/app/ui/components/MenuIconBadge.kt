package com.ariai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ariai.app.ui.theme.AriError
import com.ariai.app.ui.theme.AriMenuBlueEnd
import com.ariai.app.ui.theme.AriMenuBlueStart
import com.ariai.app.ui.theme.AriMenuMintEnd
import com.ariai.app.ui.theme.AriMenuMintStart

enum class MenuIconTone { Mint, Blue, Danger }

private val mintBadgeBrush = Brush.horizontalGradient(listOf(AriMenuMintStart, AriMenuMintEnd))
private val blueBadgeBrush = Brush.horizontalGradient(listOf(AriMenuBlueStart, AriMenuBlueEnd))

/** White menu glyph on a compact, localized accent badge; menu surfaces stay neutral. */
@Composable
fun MenuIconBadge(
    icon: ImageVector,
    tone: MenuIconTone,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 36.dp,
    iconSize: Dp = 19.dp
) {
    val badgeBrush = when (tone) {
        MenuIconTone.Mint -> mintBadgeBrush
        MenuIconTone.Blue -> blueBadgeBrush
        MenuIconTone.Danger -> SolidColor(AriError)
    }
    Box(
        modifier = modifier.size(badgeSize).clip(RoundedCornerShape(11.dp)).background(badgeBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}
