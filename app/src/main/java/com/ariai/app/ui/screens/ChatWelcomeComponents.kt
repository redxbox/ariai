package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.Provider
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx

private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted

@Composable
internal fun EmptyChatWelcome(
    modifier: Modifier,
    activeAgent: Agent?,
    compactWelcome: Boolean,
    selectedModelReady: Boolean,
    modelSetupMessage: String,
    providers: List<Provider>,
    currentProvider: Provider?,
    agentToolsSummary: List<String>,
    agentExamples: List<String>,
    onManageProviders: () -> Unit,
    onInputTextChange: (String) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compactWelcome) 6.dp else 10.dp)
        ) {
            if (activeAgent == null) {
                Text(
                    text = "AriAI",
                    modifier = Modifier.testTag("empty-chat-watermark"),
                    color = Ink.copy(alpha = 0.16f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 34.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = AriCard.copy(alpha = 0.72f),
                            offset = Offset(0f, 1f),
                            blurRadius = 0f
                        )
                    )
                )
            } else {
                Box(
                    modifier = Modifier.size(if (compactWelcome) 38.dp else 48.dp)
                        .clip(CircleShape)
                        .background(AriAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        activeAgent.avatar,
                        fontSize = if (compactWelcome) 18.sp else 22.sp
                    )
                }
                Text(
                    activeAgent.name,
                    modifier = Modifier.testTag("welcome-agent-name"),
                    color = Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                )
                Text(
                    tx("How can I help you?"),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compactWelcome) 17.sp else 19.sp,
                    textAlign = TextAlign.Center
                )
                activeAgent?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        description,
                        modifier = Modifier.fillMaxWidth().testTag("welcome-agent-description"),
                        color = Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (agentToolsSummary.isNotEmpty()) {
                    Text(
                        "${tx("Tools")}: ${agentToolsSummary.joinToString(" · ")}",
                        modifier = Modifier.fillMaxWidth().testTag("welcome-agent-tools"),
                        color = AriAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!selectedModelReady) {
                    Text(
                        modelSetupMessage,
                        color = Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!compactWelcome && providers.isNotEmpty() &&
                        (currentProvider == null || currentProvider.models.isEmpty())
                    ) {
                        Button(
                            onClick = onManageProviders,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AriAccentSoft,
                                contentColor = AriAccent
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(tx("Manage providers"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                } else if (!compactWelcome) {
                    Text(
                        tx("Type a message below to start."),
                        color = Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().testTag("welcome-suggestions"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        if (agentExamples.isNotEmpty()) {
                            agentExamples.forEachIndexed { index, example ->
                                item(key = "agent-example:$index") {
                                    AssistChip(
                                        onClick = { onInputTextChange(example) },
                                        modifier = Modifier.testTag("agent-example-$index"),
                                        label = {
                                            Text(example, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = AriCard,
                                            labelColor = Ink,
                                            leadingIconContentColor = AriAccent
                                        )
                                    )
                                }
                            }
                        } else {
                            item(key = "suggestion:brainstorm") {
                                AssistChip(
                                    onClick = { onInputTextChange(tx("Help me brainstorm a few ideas.")) },
                                    label = { Text(tx("Brainstorm")) },
                                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = AriCard,
                                        labelColor = Ink,
                                        leadingIconContentColor = AriAccent
                                    )
                                )
                            }
                            item(key = "suggestion:summarize") {
                                AssistChip(
                                    onClick = { onInputTextChange(tx("Summarize this for me.")) },
                                    label = { Text(tx("Summarize")) },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = AriCard,
                                        labelColor = Ink,
                                        leadingIconContentColor = AriAccent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
