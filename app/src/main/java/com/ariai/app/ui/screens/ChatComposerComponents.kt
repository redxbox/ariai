package com.ariai.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextStyle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.SearchMode
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted
private val PageBg: Color @Composable @ReadOnlyComposable get() = AriPaper

@Composable
internal fun ChatComposerBar(
    pending: List<Attachment>,
    onRemoveAttachment: (String) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onExpand: () -> Unit,
    chatMode: ChatMode,
    onChatModeChange: (ChatMode) -> Unit,
    selectedModelReady: Boolean,
    isStreaming: Boolean,
    isSending: Boolean,
    onStopGeneration: () -> Unit,
    onSend: () -> Unit,
    onShowAttachments: () -> Unit,
    searchMode: SearchMode,
    onShowSearch: () -> Unit,
    listening: Boolean,
    onToggleListening: () -> Unit,
    activeAgent: Agent?
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(PageBg).navigationBarsPadding().imePadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (pending.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                items(pending, key = { it.id }) { attachment ->
                    PendingChip(attachment) { onRemoveAttachment(attachment.id) }
                }
            }
        }

        // Keep model selection above the conversation so it never crowds message entry.
        val composerShape = RoundedCornerShape(32.dp)
        Surface(
            shape = composerShape,
            color = AriCard,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(tween(220, easing = FastOutSlowInEasing))
                .border(1.dp, AriLine.copy(alpha = 0.9f), composerShape)
                .testTag("chat-composer-container")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    .animateContentSize(tween(220, easing = FastOutSlowInEasing))
            ) {
                // Long messages get an expand button that opens a full-screen editor.
                if (inputText.length > 120 || inputText.contains('\n')) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onExpand) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = tx("Expand message"),
                                tint = Ink.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (chatMode != ChatMode.TEXT) {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)) {
                        ModeChip(mode = chatMode, onClose = { onChatModeChange(ChatMode.TEXT) }, modifier = Modifier)
                    }
                }
                val canSend = (inputText.isNotBlank() || pending.isNotEmpty()) && selectedModelReady && !isStreaming && !isSending
                val sendButtonColor by animateColorAsState(
                    targetValue = if (canSend) AriAccentSoft else AriAccentSoft.copy(alpha = 0.58f),
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "chat-send-background"
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Resolve paragraph direction from the input itself so Persian remains RTL
                    // even when the app's interface language is English.
                    TextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                activeAgent?.name?.takeIf { it.isNotBlank() }?.let { "${tx("Message")} $it" }
                                    ?: tx("Message AriAI"),
                                color = Muted
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Start,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        modifier = Modifier.weight(1f).testTag("chat-composer"),
                        maxLines = 12,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Accent,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink
                        )
                    )
                    if (isStreaming || isSending) {
                        // Keep a 48 dp tap target while drawing a smaller, rounded stop control.
                        Surface(
                            onClick = { onStopGeneration() },
                            modifier = Modifier.size(48.dp).testTag("chat-send-button"),
                            shape = CircleShape,
                            color = Color.Transparent,
                            contentColor = AriAccent,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Surface(
                                    modifier = Modifier.size(40.dp).testTag("chat-send-button-visual"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = AriAccentSoft,
                                    contentColor = AriAccent,
                                    shadowElevation = 1.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Stop, contentDescription = tx("Stop"), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            onClick = { onSend() },
                            enabled = canSend,
                            modifier = Modifier.size(48.dp).testTag("chat-send-button"),
                            shape = CircleShape,
                            color = Color.Transparent,
                            contentColor = AriAccent,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Surface(
                                    modifier = Modifier.size(40.dp).testTag("chat-send-button-visual"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = sendButtonColor,
                                    contentColor = AriAccent,
                                    shadowElevation = if (canSend) 2.dp else 1.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = tx("Send"),
                                            tint = AriAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComposerActionButton(Icons.Default.Add, tx("Add to message"), active = false) {
                        onShowAttachments()
                    }
                    Spacer(Modifier.width(4.dp))
                    ComposerActionButton(
                        Icons.Default.Search,
                        tx("Web search"),
                        active = searchMode != SearchMode.OFF,
                        stateful = true
                    ) {
                        onShowSearch()
                    }
                    Spacer(Modifier.width(4.dp))
                    ComposerActionButton(
                        icon = if (listening) Icons.Default.Stop else Icons.Default.Mic,
                        description = if (listening) tx("Stop listening") else tx("Voice input"),
                        active = listening,
                        stateful = true,
                        activeStateDescription = tx("Listening…")
                    ) {
                        onToggleListening()
                    }
                    if (listening) {
                        Spacer(Modifier.width(6.dp))
                        ListeningIndicator()
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
