package com.ariai.app.ui.theme

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * Scroll/focus stability helpers.
 *
 * Two distinct mechanisms are used to make scrollable screens shiver:
 *
 * 1. A focused control (usually an OutlinedTextField, but also switches, chips, and sliders)
 *    asks the scrollable to reveal it on every layout pass (bring-into-view). The user's scroll
 *    gesture and the framework's reveal fight each other, so the content jitters up and down
 *    until focus is dropped. Focus is dropped the moment the *user* drags the content vertically
 *    (see [DragFocusSweeper]). Programmatic scrolls — most importantly the bring-into-view
 *    animation that runs right after a text field gains focus (e.g. when the keyboard opens for a
 *    field in the lower half of the screen) — do NOT clear focus, so the keyboard stays open.
 *
 * 2. A fling that reaches the end of a scrollable inside a fully expanded [androidx.compose.material3.ModalBottomSheet]
 *    handed its leftover velocity to the sheet's AnchoredDraggable. The sheet spring overshoots
 *    and the whole sheet oscillates vertically until the next touch. The connection returned by
 *    [rememberSheetScrollFix] swallows only the leftover upward velocity at the expanded state,
 *    after the inner scrollable had its chance to consume the gesture.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ClearFocusOnScroll(listState: LazyListState) = ClearFocusOnUserDrag()

@Suppress("UNUSED_PARAMETER")
@Composable
fun ClearFocusOnScroll(scrollState: ScrollState) = ClearFocusOnUserDrag()

/**
 * Drops keyboard/focus the moment the *user* physically drags the content vertically.
 *
 * Implementation notes: the bring-into-view animation that reveals a newly focused field is a
 * programmatic scroll, not a touch. The previous implementation reacted to any scroll —
 * including that one — via `snapshotFlow { state.isScrollInProgress }`, which dismissed the
 * keyboard the instant it opened for fields in the lower half of the screen. Listening for the
 * user's own touch stream instead keeps programmatic reveals intact. The listener never consumes
 * events and is shared and ref-counted per root view, so several scrollables on one screen
 * cooperate instead of clobbering each other.
 */
@Composable
private fun ClearFocusOnUserDrag() {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    DisposableEffect(view, focusManager) {
        DragFocusSweeper.register(view, focusManager)
        onDispose { DragFocusSweeper.unregister(view, focusManager) }
    }
}

/**
 * Keeps one touch listener per root view while at least one scrollable asks for it. Every
 * registrant is a [androidx.compose.ui.focus.FocusManager]; on a vertical drag past the touch
 * slop all registered managers clear their focus. Horizontal swipes and small jitters keep focus.
 */
private object DragFocusSweeper {
    private class Entry {
        var count = 0
        var downX = 0f
        var downY = 0f
        var cleared = false
        val managers = mutableListOf<androidx.compose.ui.focus.FocusManager>()
    }

    private val entries = HashMap<View, Entry>()

    @Synchronized
    fun register(view: View, focusManager: androidx.compose.ui.focus.FocusManager) {
        val entry = entries.getOrPut(view) { Entry() }
        if (entry.count == 0) {
            val slop = ViewConfiguration.get(view.context).scaledTouchSlop
            view.setOnTouchListener(View.OnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        entry.downX = event.rawX
                        entry.downY = event.rawY
                        entry.cleared = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!entry.cleared) {
                            val dy = event.rawY - entry.downY
                            val dx = event.rawX - entry.downX
                            if (kotlin.math.abs(dy) > slop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                                entry.managers.forEach { manager -> runCatching { manager.clearFocus() } }
                                entry.cleared = true
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        entry.downX = 0f
                        entry.downY = 0f
                        entry.cleared = false
                    }
                }
                false // never consume: Compose receives every event unchanged
            })
        }
        entry.count += 1
        if (focusManager !in entry.managers) entry.managers += focusManager
    }

    @Synchronized
    fun unregister(view: View, focusManager: androidx.compose.ui.focus.FocusManager) {
        val entry = entries[view] ?: return
        entry.count -= 1
        entry.managers.remove(focusManager)
        if (entry.count <= 0) {
            view.setOnTouchListener(null)
            entries.remove(view)
        }
    }
}

/**
 * Returns a nested scroll connection to place between a ModalBottomSheet and its scrollable
 * content. While the sheet is fully expanded it consumes leftover upward scroll and fling
 * velocity so the sheet's anchor never receives the kick that makes it oscillate. Drag gestures
 * and swipe-to-dismiss (positive y) keep working, and a partially expanded sheet can still be
 * opened further by flinging upwards.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
