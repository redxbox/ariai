package com.ariai.app.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.paperPopupModifier
import com.ariai.app.ui.theme.paperPopupShape
import com.ariai.app.util.tx
import androidx.compose.ui.unit.dp

/** Discloses the Android speech-recognition data flow before microphone permission/use. */
@Composable
internal fun VoiceInputDisclosureDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogShape = paperPopupShape(26.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.then(paperPopupModifier(dialogShape)),
        shape = dialogShape,
        containerColor = AriCard,
        title = { Text(tx("Voice input")) },
        text = {
            Text(
                tx("The Android speech-recognition service receives your audio to transcribe it and may process it locally or remotely under its own privacy policy. The transcript is added to your message; it is sent to your AI provider only when you tap Send.")
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(tx("Continue")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tx("Cancel")) }
        }
    )
}
