package com.ariai.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Opaque paper-like decoration for popup surfaces: a light outline and a small folded corner.
 * This deliberately avoids translucency and blur so popup content stays easy to read.
 */
@Composable
fun paperPopupModifier(
    shape: Shape,
    foldColor: Color,
    foldSize: Dp = 14.dp
): Modifier {
    val borderColor = AriLine
    val creaseColor = AriInk.copy(alpha = 0.16f)
    val foldEdgeColor = AriCard.copy(alpha = 0.72f)

    return Modifier
        .border(BorderStroke(1.dp, borderColor), shape)
        .drawWithContent {
            drawContent()

            val fold = foldSize.toPx()
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val start = if (isRtl) Offset(fold * 2f, 0f) else Offset(size.width - fold * 2f, 0f)
            val tip = if (isRtl) Offset(fold, fold) else Offset(size.width - fold, fold)
            val inner = if (isRtl) Offset(fold * 2f, fold) else Offset(size.width - fold * 2f, fold)
            val flap = Path().apply {
                moveTo(start.x, start.y)
                lineTo(tip.x, tip.y)
                lineTo(inner.x, inner.y)
                close()
            }

            drawPath(flap, color = foldColor)
            drawLine(
                color = creaseColor,
                start = start,
                end = tip,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Butt
            )
            drawLine(
                color = foldEdgeColor,
                start = tip,
                end = inner,
                strokeWidth = 0.75.dp.toPx(),
                cap = StrokeCap.Butt
            )
        }
}
