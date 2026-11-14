package com.ariai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.paperPopupModifier
import com.ariai.app.ui.theme.paperPopupShape
import com.ariai.app.util.UiLang
import com.ariai.app.util.promptTemplates
import com.ariai.app.util.tx

/** Rename dialog for the current chat. Saving and dismissal are handled by the caller. */
@Composable
internal fun RenameChatDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogShape = paperPopupShape(26.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = paperPopupModifier(dialogShape),
        shape = dialogShape,
        containerColor = AriCard,
        title = { Text(tx("Rename chat")) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.testTag("chat-rename-field"),
                singleLine = true,
                placeholder = { Text(tx("Chat title")) }
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(tx("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tx("Cancel")) }
        }
    )
}

/** Prompt template picker. [onPick] receives the localized template text. */
@Composable
internal fun PromptTemplatesDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx("Prompt templates")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                promptTemplates.forEach { template ->
                    TextButton(
                        onClick = { onPick(template.text(UiLang.code)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tx(template.title), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tx("Cancel")) } }
    )
}

/** Warning shown before attaching a video with a non-Gemini provider. */
@Composable
internal fun NonGeminiVideoWarningDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tx("This provider may not support video")) },
        text = { Text(tx("Video input is supported by Gemini providers. With the selected provider, the video may be ignored or rejected. Continue anyway?")) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(tx("Continue")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tx("Cancel")) }
        }
    )
}
