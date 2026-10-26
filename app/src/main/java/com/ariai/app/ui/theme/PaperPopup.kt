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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_PAPER_FOLD_SIZE = 12.dp

/** Shared rounded geometry for menus and centered dialogs. */
fun paperPopupShape(
    cornerRadius: Dp,
    @Suppress("UNUSED_PARAMETER") foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Shape = RoundedCornerShape(cornerRadius)

/** Large, soft top corners make sheets read as floating surfaces rather than hard panels. */
fun paperSheetShape(
    cornerRadius: Dp,
    @Suppress("UNUSED_PARAMETER") foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)

/** A hairline outline keeps light dialogs distinct from the lavender page behind them. */
@Composable
fun paperPopupModifier(
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") foldSize: Dp = DEFAULT_PAPER_FOLD_SIZE
): Modifier = Modifier.border(BorderStroke(0.75.dp, AriLine.copy(alpha = 0.9f)), shape)

/**
 * Shared modal sheet with a conventional drag handle, soft scrim, and explicit system-bar/keyboard
 * clearance. The Material sheet supplies its spring-driven slide/dismiss motion; this wrapper keeps
 * every sheet on the same surface, edge, and safe-area treatment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperBottomSheet(
    onDismissRequest: () -> Unit,
    shape: Shape,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = shape,
        modifier = Modifier.border(BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f)), shape),
        containerColor = AriCard,
        contentColor = AriInk,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            content = content
        )
    }
}
