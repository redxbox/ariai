package com.ariai.app.ui.theme

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.focus.LocalFocusManager
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue

/**
 * Scroll/focus stability helpers.
 *
 * Two distinct mechanisms used to make scrollable screens shiver:
 *
 * 1. A focused control (usually an OutlinedTextField, but also switches, chips, and sliders)
 *    asks the scrollable to reveal it on every layout pass (bring-into-view). The user's scroll
 *    gesture and the framework's reveal fight each other, so the content jitters up and down
 *    until focus is dropped. Dropping focus as soon as a scroll starts stops the fight.
 *
 * 2. A fling that reaches the end of a scrollable inside a fully expanded [androidx.compose.material3.ModalBottomSheet]
 *    handed its leftover velocity to the sheet's AnchoredDraggable. The sheet spring overshoots
 *    and the whole sheet oscillates vertically until the next touch. The connection returned by
 *    [rememberSheetScrollFix] swallows only the leftover upward velocity at the expanded state,
 *    after the inner scrollable had its chance to consume the gesture.
 */
@Composable
fun ClearFocusOnScroll(listState: LazyListState) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) focusManager.clearFocus() }
    }
}

@Composable
fun ClearFocusOnScroll(scrollState: ScrollState) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) focusManager.clearFocus() }
    }
}

/**
 * Returns a nested scroll connection to place between a ModalBottomSheet and its scrollable
 * content. While the sheet is fully expanded it consumes leftover upward scroll and fling
 * velocity so the sheet's anchor never receives the kick that makes it oscillate. Drag gestures
 * and swipe-to-dismiss (positive y) keep working, and a partially expanded sheet can still be
 * opened further by flinging upwards.
 */
@Composable
fun rememberSheetScrollFix(sheetState: SheetState): NestedScrollConnection =
    remember(sheetState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset =
                if (available.y < 0f && sheetState.currentValue == SheetValue.Expanded) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (available.y < 0f && sheetState.currentValue == SheetValue.Expanded) {
                    Velocity(0f, available.y)
                } else {
                    Velocity.Zero
                }
        }
    }
