package com.ariai.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.remote.ToolExecutionPhase
import com.ariai.app.data.remote.ToolExecutionUpdate
import com.ariai.app.ui.theme.*
import com.ariai.app.util.UiLang
import com.ariai.app.util.tx
import java.text.NumberFormat
import java.util.Locale

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted

@Composable
internal fun ToolExecutionTimeline(updates: List<ToolExecutionUpdate>) {
    val finished = updates.filter { it.phase != ToolExecutionPhase.RUNNING }
    if (finished.isEmpty()) return
    val visible = finished.takeLast(3)
    Column(
        modifier = Modifier.padding(start = 8.dp).testTag("chat-tool-timeline"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (finished.size > visible.size) {
            Text(
                text = "+${finished.size - visible.size}",
                color = Muted.copy(alpha = 0.72f),
                fontSize = 10.sp
            )
        }
        visible.forEach { update ->
            val label = tx(update.label)
            val statusText = when (update.phase) {
                ToolExecutionPhase.COMPLETED -> if (update.elapsedMs >= 1_000L) {
                    "$label · ${formatGenerationDuration(update.elapsedMs / 1_000L)}"
                } else label
                ToolExecutionPhase.FAILED -> "$label · ${tx("Failed")}"
                ToolExecutionPhase.CANCELLED -> "$label · ${tx("Stopped")}"
                ToolExecutionPhase.RUNNING -> label
            }
            val icon = when (update.phase) {
                ToolExecutionPhase.COMPLETED -> Icons.Default.Check
                ToolExecutionPhase.FAILED -> Icons.Default.ErrorOutline
                ToolExecutionPhase.CANCELLED -> Icons.Default.Stop
                ToolExecutionPhase.RUNNING -> Icons.Default.MoreHoriz
            }
            Row(
                modifier = Modifier.testTag("chat-tool-execution-step"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Muted.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                Text(
                    statusText,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun GenerationStatus(label: String, elapsedSeconds: Long, active: Boolean) {
    val duration = formatGenerationDuration(elapsedSeconds)
    val statusText = if (elapsedSeconds > 0L) "$label · $duration" else label
    Surface(
        shape = RoundedCornerShape(50),
        color = AriTint,
        modifier = Modifier.padding(start = 4.dp).widthIn(max = 360.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (active) {
                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 1.8.dp, color = Accent)
            } else {
                Icon(Icons.Default.Check, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
            }
            Text(
                statusText,
                modifier = Modifier.testTag(if (active) "chat-generation-status-active" else "chat-generation-status-complete"),
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Live status card while a reply is being generated: an animated "thinking /
 * searching" header with the elapsed time, plus every tool step executed so far,
 * all inside one frame.
 */
@Composable
internal fun GenerationWorkCard(
    label: String,
    elapsedSeconds: Long,
    updates: List<ToolExecutionUpdate>,
    modifier: Modifier = Modifier
) {
    val steps = updates.takeLast(8)
    val hiddenCount = updates.size - steps.size
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AriCard,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.85f)),
        modifier = modifier
            .padding(start = 4.dp)
            .widthIn(max = 380.dp)
            .animateContentSize(tween(200, easing = FastOutSlowInEasing))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulsingWorkDot()
                val statusText = if (elapsedSeconds > 0L) {
                    "$label · ${formatGenerationDuration(elapsedSeconds)}"
                } else {
                    label
                }
                Text(
                    statusText,
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat-generation-status-active")
                )
            }
            if (steps.isNotEmpty()) {
                Column(
                    modifier = Modifier.testTag("chat-tool-timeline"),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (hiddenCount > 0) {
                        Text("+$hiddenCount", color = Muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    steps.forEach { update -> GenerationWorkStep(update) }
                }
            }
        }
    }
}

@Composable
private fun GenerationWorkStep(update: ToolExecutionUpdate) {
    val label = tx(update.label)
    val statusText = when (update.phase) {
        ToolExecutionPhase.COMPLETED -> if (update.elapsedMs >= 1_000L) {
            "$label · ${formatGenerationDuration(update.elapsedMs / 1_000L)}"
        } else {
            label
        }
        ToolExecutionPhase.FAILED -> "$label · ${tx("Failed")}"
        ToolExecutionPhase.CANCELLED -> "$label · ${tx("Stopped")}"
        ToolExecutionPhase.RUNNING -> label
    }
    Row(
        modifier = Modifier.testTag("chat-tool-execution-step"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        when (update.phase) {
            ToolExecutionPhase.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = Accent
            )
            ToolExecutionPhase.COMPLETED -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Accent.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            ToolExecutionPhase.FAILED -> Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(13.dp)
            )
            ToolExecutionPhase.CANCELLED -> Icon(
                Icons.Default.Stop,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            statusText,
            color = if (update.phase == ToolExecutionPhase.RUNNING) Ink else Muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Small breathing dot that signals an active generation without a heavy spinner. */
@Composable
private fun PulsingWorkDot() {
    val transition = rememberInfiniteTransition(label = "generation-work-dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "generation-work-dot-alpha"
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(Accent.copy(alpha = alpha))
    )
}

private fun formatGenerationDuration(totalSeconds: Long): String {
    val numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag(UiLang.code))
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes == 0L) {
        "${numbers.format(totalSeconds)} ${tx("sec")}"
    } else if (seconds == 0L) {
        "${numbers.format(minutes)} ${tx("min")}"
    } else {
        "${numbers.format(minutes)} ${tx("min")} ${numbers.format(seconds)} ${tx("sec")}"
    }
}
