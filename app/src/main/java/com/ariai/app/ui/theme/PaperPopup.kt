package com.ariai.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** A popup silhouette with a squared logical-end corner for the folded flap. */
fun paperPopupShape(cornerRadius: Dp) = RoundedCornerShape(
    topStart = cornerRadius,
    topEnd = 0.dp,
    bottomEnd = cornerRadius,
    bottomStart = cornerRadius
)

/** Bottom sheets keep their square bottom edge and fold the logical-end top corner. */
fun paperSheetShape(cornerRadius: Dp) = RoundedCornerShape(
    topStart = cornerRadius,
    topEnd = 0.dp
)

/** Adds a visible, theme-aware dog-ear to a popup, with an outline for dialog/menu surfaces. */
@Composable
fun paperPopupModifier(
    shape: Shape,
    foldSize: Dp = 18.dp
): Modifier = Modifier
    .border(BorderStroke(1.dp, AriInk.copy(alpha = 0.18f)), shape)
    .then(paperFoldModifier(foldSize))

/** Draws the fold in sheet-content coordinates, not on the full-screen ModalBottomSheet window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperBottomSheet(
    onDismissRequest: () -> Unit,
    shape: Shape,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = shape,
        containerColor = AriCard,
        tonalElevation = 4.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().then(paperFoldModifier(18.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle()
            }
            content()
        }
    }
}

@Composable
private fun paperFoldModifier(foldSize: Dp): Modifier {
    val foldColor = AriFold
    val creaseColor = AriInk.copy(alpha = 0.34f)
    val foldEdgeColor = AriCard.copy(alpha = 0.9f)

    return Modifier.drawWithContent {
        drawContent()

        // The flap reaches the actual corner; the opposite half stays backed by opaque paper.
        val fold = foldSize.toPx()
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val start = if (isRtl) Offset(fold * 2f, 0f) else Offset(size.width - fold * 2f, 0f)
        val tip = if (isRtl) Offset(0f, fold * 2f) else Offset(size.width, fold * 2f)
        val inner = if (isRtl) Offset(fold * 2f, fold * 2f) else Offset(size.width - fold * 2f, fold * 2f)
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
