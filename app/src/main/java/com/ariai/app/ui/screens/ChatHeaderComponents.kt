package com.ariai.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.Provider
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx

private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted

@Composable
internal fun ChatHeaderTitle(
    chatTitle: String,
    activeAgent: Agent?,
    onTitleClick: () -> Unit,
    onAgentClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("chat-title-header").padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onAgentClick,
            modifier = Modifier.size(48.dp)
                .testTag("chat-agent-button")
                .semantics(mergeDescendants = true) {
                    contentDescription = tx("Agents & prompts")
                },
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = Ink,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (activeAgent == null) {
                    ProviderBrandMark(provider = null, size = 30.dp)
                } else {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(AriAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeAgent.avatar, fontSize = 17.sp)
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            chatTitle,
            modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { onTitleClick() }
                .semantics { stateDescription = tx("Rename chat") }
                .testTag("chat-title-text")
                .padding(vertical = 5.dp),
            color = Ink,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
    }
}

@Composable
internal fun ChatModelSelector(
    provider: Provider?,
    modelName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelectedModel = provider != null && modelName.isNotBlank() && modelName != tx("Choose model")
    val label = if (hasSelectedModel) modelName else tx("Choose model")
    Surface(
        onClick = onClick,
        modifier = modifier.widthIn(max = 244.dp).heightIn(min = 40.dp)
            .animateContentSize(tween(190, easing = FastOutSlowInEasing))
            .semantics(mergeDescendants = true) {
                contentDescription = tx("Choose model")
                stateDescription = "${provider?.name ?: tx("No provider")} · $modelName"
            },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        contentColor = Ink,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.widthIn(max = 210.dp),
                color = if (hasSelectedModel) Ink else Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun ContextUsageIndicator(
    tokens: Int,
    contextWindow: Int,
    approximate: Boolean,
    onClick: () -> Unit
) {
    val fraction = if (contextWindow > 0) (tokens.toFloat() / contextWindow).coerceIn(0f, 1f) else 0f
    val percentage = (fraction * 100).toInt()
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = tx("Context usage")
                stateDescription = (if (approximate) "${tx("Approx.")} " else "") + "$percentage%"
            }
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("chat-context-usage"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = fraction,
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = if (fraction >= 0.9f) AriError else AriAccent,
            trackColor = AriTint
        )
        Text(
            if (approximate) "~$percentage%" else "$percentage%",
            color = Ink,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
