package com.ariai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.util.tx

/** Per-conversation generation controls; only saved values affect later requests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickParametersSheet(
    temperature: Float,
    topP: Float,
    systemPrompt: String?,
    onSave: (temperature: Float, topP: Float, systemPrompt: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var temperatureValue by remember(temperature) { mutableFloatStateOf(temperature.coerceIn(0f, 2f)) }
    var topPValue by remember(topP) { mutableFloatStateOf(topP.coerceIn(0f, 1f)) }
    var promptValue by remember(systemPrompt) { mutableStateOf(systemPrompt.orEmpty()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AriCard,
        contentColor = AriInk
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tx("Quick parameters"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                tx("These settings apply to this conversation. A provider may ignore unsupported parameters."),
                color = AriMuted,
                fontSize = 13.sp
            )

            ParameterSlider(
                title = tx("Temperature"),
                valueLabel = String.format(java.util.Locale.ROOT, "%.1f", temperatureValue),
                helper = tx("Lower is more focused; higher is more varied."),
                value = temperatureValue,
                range = 0f..2f,
                steps = 19,
                onValueChange = { temperatureValue = it }
            )
            ParameterSlider(
                title = tx("Top-P"),
                valueLabel = String.format(java.util.Locale.ROOT, "%.2f", topPValue),
                helper = tx("Adjust how broadly the model samples likely words."),
                value = topPValue,
                range = 0f..1f,
                steps = 19,
                onValueChange = { topPValue = it }
            )

            OutlinedTextField(
                value = promptValue,
                onValueChange = { promptValue = it },
                label = { Text(tx("System prompt")) },
                placeholder = { Text(tx("Optional instructions for this conversation")) },
                minLines = 3,
                maxLines = 7,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                tx("An empty prompt keeps the selected agent's instructions, if any."),
                color = AriMuted,
                fontSize = 12.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = {
                        temperatureValue = 0.7f
                        topPValue = 1.0f
                        promptValue = ""
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(tx("Reset"), color = AriInk) }
                Button(
                    onClick = {
                        onSave(temperatureValue.coerceIn(0f, 2f), topPValue.coerceIn(0f, 1f), promptValue.trim().takeIf { it.isNotEmpty() })
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriCard)
                ) { Text(tx("Save settings")) }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    title: String,
    valueLabel: String,
    helper: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Medium, color = AriInk)
            Text(valueLabel, fontWeight = FontWeight.SemiBold, color = AriInk)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
        Text(helper, color = AriMuted, fontSize = 12.sp)
    }
}
