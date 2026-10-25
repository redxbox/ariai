package com.ariai.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_PAPER_FOLD_SIZE = 12.dp

/** Standard rounded surface; [foldSize] remains for source compatibility with older call sites. */
fun paperPopupShape(
    cornerRadius: Dp,
    @Suppress("UNUSED_PARAMETER") foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Shape = RoundedCornerShape(cornerRadius)

/** Rounded top corners without decorative cut-outs, matching the rest of the light interface. */
fun paperSheetShape(
    cornerRadius: Dp,
    @Suppress("UNUSED_PARAMETER") foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)

/** A restrained outline for menus/dialogs; no clipped corner or paper-fold overlay. */
@Composable
fun paperPopupModifier(
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Modifier = Modifier.border(BorderStroke(1.dp, AriLine), shape)

/**
 * Shared modal sheet with a conventional drag handle and explicit system-bar/keyboard clearance.
 * The insets are consumed here so sheet actions remain reachable above Android navigation.
 */
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
        contentColor = AriInk,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            content = content
        )
    }
}
