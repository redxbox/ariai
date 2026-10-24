package com.ariai.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val DEFAULT_PAPER_FOLD_SIZE = 12.dp

/** Popup silhouette with the physical top-left paper corner folded inward. */
fun paperPopupShape(cornerRadius: Dp, foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE): Shape =
    PaperDogEarShape(cornerRadius, foldSize, roundBottom = true)

/** Bottom sheets keep their square bottom edge and fold the physical top-left corner. */
fun paperSheetShape(cornerRadius: Dp, foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE): Shape =
    PaperDogEarShape(cornerRadius, foldSize, roundBottom = false)

/**
 * Clips popup content to a dog-eared page, then paints an opaque backing and matte folded flap over
 * the clipped corner. The overlay is outside the inner clip, so the underlying window never shows.
 */
@Composable
fun paperPopupModifier(
    shape: Shape,
    foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Modifier {
    val foldColor = AriFold
    val inkColor = AriInk
    return Modifier
        .drawWithContent {
            drawContent()
            drawPaperFold(foldSize.toPx(), foldColor, inkColor)
        }
        .clip(shape)
        .border(BorderStroke(1.dp, AriInk.copy(alpha = 0.18f)), shape)
}

/** Draws the flap in sheet-content coordinates, not on the full-screen ModalBottomSheet window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperBottomSheet(
    onDismissRequest: () -> Unit,
    shape: Shape,
    content: @Composable ColumnScope.() -> Unit
) {
    val paperShape = shape as? PaperDogEarShape
    val sheetShape = paperShape?.surfaceShape() ?: shape
    val foldSize = paperShape?.foldSize ?: DEFAULT_PAPER_FOLD_SIZE
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // The opaque sheet surface remains behind the clipped corner as a matte paper backing.
        shape = sheetShape,
        containerColor = AriCard,
        tonalElevation = 4.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().then(paperFoldModifier(shape, foldSize))
        ) {
            val physicalLeftAlignment = if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = physicalLeftAlignment
            ) {
                // Leave a small physical-left gutter for the folded corner while keeping the handle left-aligned.
                BottomSheetDefaults.DragHandle(modifier = Modifier.absolutePadding(left = 32.dp))
            }
            content()
        }
    }
}

/**
 * A surface shape with the original top-left triangle clipped from its content. The bottom sheet's
 * own opaque rounded surface supplies the backing behind the flap, preventing a see-through notch.
 */
private class PaperDogEarShape(
    private val cornerRadius: Dp,
    val foldSize: Dp,
    private val roundBottom: Boolean
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val shortSide = min(size.width, size.height)
        val fold = with(density) { foldSize.toPx() }
        val cut = (fold * 2f).coerceAtMost(shortSide * 0.45f)
        val requestedRadius = with(density) { cornerRadius.toPx() }
        val radius = requestedRadius
            .coerceAtMost((size.width - cut).coerceAtLeast(0f))
            .coerceAtMost(size.height / 2f)
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width - radius, 0f)
            quadraticBezierTo(size.width, 0f, size.width, radius)
            if (roundBottom) {
                lineTo(size.width, size.height - radius)
                quadraticBezierTo(size.width, size.height, size.width - radius, size.height)
                lineTo(radius, size.height)
                quadraticBezierTo(0f, size.height, 0f, size.height - radius)
            } else {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            lineTo(0f, cut)
            lineTo(cut, 0f)
            close()
        }
        return Outline.Generic(path)
    }

    fun surfaceShape(): Shape = if (roundBottom) {
        RoundedCornerShape(cornerRadius)
    } else {
        RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    }
}

@Composable
private fun paperFoldModifier(shape: Shape, foldSize: Dp): Modifier {
    val foldColor = AriFold
    val inkColor = AriInk
    // Clip sheet content, then draw the opaque folded triangle over the notch. The rounded sheet
    // itself remains as a solid backing, and the drag handle is shifted clear of this physical-left fold.
    return Modifier
        .drawWithContent {
            drawContent()
            drawPaperFold(foldSize.toPx(), foldColor, inkColor)
        }
        .clip(shape)
}

/** Renders an opaque matte flap exactly over the clipped corner, with a restrained dark crease. */
private fun DrawScope.drawPaperFold(requestedFold: Float, foldColor: Color, inkColor: Color) {
    val cut = (requestedFold * 2f).coerceAtMost(min(size.width, size.height) * 0.45f)
    val top = Offset(cut, 0f)
    val left = Offset(0f, cut)

    // This opaque triangle occupies the clipped corner itself, so the underlying window or flat
    // page never shows through the fold.
    val flap = Path().apply {
        moveTo(0f, 0f)
        lineTo(top.x, top.y)
        lineTo(left.x, left.y)
        close()
    }
    drawPath(flap, color = foldColor)

    val edgeShadow = inkColor.copy(alpha = 0.16f)
    drawLine(edgeShadow, top, left, strokeWidth = 0.8.dp.toPx(), cap = StrokeCap.Butt)

    // The crease is inset into the flap face, not drawn across the intact paper beneath it.
    val inset = cut * 0.12f
    drawLine(
        color = inkColor.copy(alpha = 0.30f),
        start = Offset(cut - inset, 0f),
        end = Offset(0f, cut - inset),
        strokeWidth = 0.85.dp.toPx(),
        cap = StrokeCap.Butt
    )
}
