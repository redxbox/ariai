package com.ariai.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted

@Composable
internal fun ComposerActionButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    stateful: Boolean = false,
    activeStateDescription: String? = null,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (active) AriAccentSoft else Color.Transparent,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "composer-action-background"
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) Accent else Ink.copy(alpha = 0.7f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "composer-action-foreground"
    )
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (stateful) {
                    stateDescription = if (active) activeStateDescription ?: tx("On") else tx("Off")
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun ListeningIndicator() {
    val transition = rememberInfiniteTransition(label = "voice-listening-indicator")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-listening-pulse"
    )
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier.clip(shape)
            .background(AriAccentSoft.copy(alpha = 0.78f))
            .border(1.dp, AriAccent.copy(alpha = 0.14f), shape)
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("voice-listening-indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape)
                .background(AriAccent.copy(alpha = dotAlpha))
        )
        Text(
            text = tx("Listening…"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AriAccent,
            maxLines = 1
        )
    }
}

private val appearedKeys = HashSet<String>()

/** Fades and slides each message in once when it first appears. */
@Composable
internal fun AppearIn(key: String, content: @Composable () -> Unit) {
    // Items already shown once skip the entrance animation (LazyColumn recreates items on scroll).
    var visible by remember(key) { mutableStateOf(appearedKeys.contains(key)) }
    LaunchedEffect(key) {
        appearedKeys.add(key)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 6 }
    ) {
        content()
    }
}

@Composable
internal fun FullScreenMessageEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onCollapse: () -> Unit
) {
    Dialog(
        onDismissRequest = onCollapse,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        val pageShape = paperPopupShape(28.dp)
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp).then(paperPopupModifier(pageShape)),
            shape = pageShape,
            color = AriCard
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(
                    Icons.Default.CloseFullscreen,
                    contentDescription = tx("Collapse"),
                    tint = Ink,
                    modifier = Modifier.size(24.dp).clickable { onCollapse() }
                )
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(tx("Message AriAI"), color = Muted) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AriAccent,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    )
                )
            }
        }
    }
}

/** Thin label in the chat's top-left corner: current thinking depth, opens the depth sheet. */
@Composable
internal fun ThinkingChip(level: ReasoningLevel, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AriCard)
            .border(0.5.dp, AriLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AriMuted, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            level.name.lowercase().replaceFirstChar { it.uppercaseChar() },
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = AriInk
        )
    }
}
