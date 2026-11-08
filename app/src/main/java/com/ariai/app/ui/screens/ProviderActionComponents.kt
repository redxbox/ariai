package com.ariai.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ariai.app.data.remote.ConnectionResult
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx

@Composable
internal fun ProviderActionBar(
    fetching: Boolean,
    checkingConnection: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit
) {
    val saveEnabled = canSave && !fetching
    val testEnabled = !checkingConnection && !fetching
    val graphite = Color(0xFF464850)
    val brandAccent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("provider-save-action-bar"),
        shape = RoundedCornerShape(26.dp),
        color = AriCard,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.62f)),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .animateContentSize(tween(180, easing = FastOutSlowInEasing)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.weight(1f).height(48.dp).testTag("provider-save"),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = graphite,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = muted
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp)
            ) {
                if (fetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = muted
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        tx("Save"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedButton(
                onClick = onCheckConnection,
                enabled = testEnabled,
                modifier = Modifier.weight(1f).height(48.dp).testTag("provider-test-connection"),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AriInk,
                    disabledContentColor = muted
                ),
                border = BorderStroke(0.85.dp, AriLine.copy(alpha = if (testEnabled) 0.82f else 0.55f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                if (checkingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = brandAccent
                    )
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = brandAccent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        tx("Test"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProviderConnectionDialog(
    providerName: String,
    checking: Boolean,
    result: ConnectionResult?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogShape = paperPopupShape(29.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .then(paperPopupModifier(dialogShape))
                .testTag("provider-connection-dialog"),
            shape = dialogShape,
            color = AriTint,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(AriAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AriAccent, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            tx("Check connection"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = AriInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(providerName, fontSize = 12.sp, color = AriMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // One fixed-height status slot and a single crossfade prevent the dialog from
                // changing height twice (the old visibility + size animations caused clipping/jitter).
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp, max = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = checking to result,
                        animationSpec = tween(170, easing = FastOutSlowInEasing),
                        label = "provider-connection-status"
                    ) { (isChecking, current) ->
                        when {
                            isChecking -> Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AriAccentSoft.copy(alpha = 0.58f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AriAccent, modifier = Modifier.size(22.dp))
                                    Text(tx("Checking…"), color = AriMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            current is ConnectionResult.Ok -> Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AriAccentGreenSoft,
                                modifier = Modifier.fillMaxWidth().testTag("provider-connection-result")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AriAccentGreen, modifier = Modifier.size(23.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            "${tx("Connected")} · ${current.models} ${tx("models")}",
                                            color = AriAccentGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("${current.durationMs} ms", color = AriMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                            current is ConnectionResult.Failed -> Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AriError.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth().testTag("provider-connection-result")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        current.problem + (current.code?.let { " (HTTP $it)" } ?: ""),
                                        color = AriError,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(current.hint, color = AriMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            else -> Spacer(Modifier.height(1.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (result is ConnectionResult.Failed && !checking) {
                        TextButton(onClick = onRetry, modifier = Modifier.testTag("provider-connection-retry")) {
                            Text(tx("Retry"), color = AriAccent)
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("provider-connection-close")) {
                        Text(tx("Close"), color = AriAccent)
                    }
                }
            }
        }
    }
}
