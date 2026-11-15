package com.ariai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.AIModel
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.util.tx

/**
 * Manual correction of what one model accepts and returns. Used when a provider publishes
 * no modality data, or publishes it wrongly. The result is marked as user-set.
 */
@Composable
internal fun ModelCapabilitiesDialog(
    model: AIModel,
    onSave: (AIModel) -> Unit,
    onDismiss: () -> Unit,
) {
    var textOut by remember(model.id) { mutableStateOf(model.outputsText) }
    var imageOut by remember(model.id) { mutableStateOf(model.supportsImageGen) }
    var audioOut by remember(model.id) { mutableStateOf(model.supportsAudioGen) }
    var videoOut by remember(model.id) { mutableStateOf(model.supportsVideoGen) }
    var visionIn by remember(model.id) { mutableStateOf(model.supportsVision) }
    var audioIn by remember(model.id) { mutableStateOf(model.supportsAudioInput) }
    var videoIn by remember(model.id) { mutableStateOf(model.supportsVideoInput) }
    var tools by remember(model.id) { mutableStateOf(model.supportsFunctionCalling) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx("Edit capabilities"), color = AriInk) },
        text = {
            Column {
                Text(
                    tx("Capabilities are detected from the provider. Change them only when they are wrong."),
                    fontSize = 12.sp,
                    color = AriMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CapabilitySwitch(tx("Text output"), textOut) { textOut = it }
                CapabilitySwitch(tx("Image output"), imageOut) { imageOut = it }
                CapabilitySwitch(tx("Audio output"), audioOut) { audioOut = it }
                CapabilitySwitch(tx("Video output"), videoOut) { videoOut = it }
                CapabilitySwitch(tx("Vision input"), visionIn) { visionIn = it }
                CapabilitySwitch(tx("Audio input"), audioIn) { audioIn = it }
                CapabilitySwitch(tx("Video input"), videoIn) { videoIn = it }
                CapabilitySwitch(tx("Tools"), tools) { tools = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    model.copy(
                        outputsText = textOut,
                        supportsImageGen = imageOut,
                        supportsAudioGen = audioOut,
                        supportsVideoGen = videoOut,
                        supportsVision = visionIn,
                        supportsAudioInput = audioIn,
                        supportsVideoInput = videoIn,
                        supportsFunctionCalling = tools,
                        capabilityOverridden = true
                    )
                )
            }) { Text(tx("Save"), color = AriInk) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tx("Cancel"), color = AriMuted) }
        }
    )
}

@Composable
private fun CapabilitySwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = AriInk)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
