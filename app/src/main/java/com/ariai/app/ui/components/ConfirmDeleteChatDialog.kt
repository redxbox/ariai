package com.ariai.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.paperPopupModifier
import com.ariai.app.ui.theme.paperPopupShape
import com.ariai.app.util.tx

@Composable
fun ConfirmDeleteChatDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val shape = paperPopupShape(26.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = paperPopupModifier(shape),
        shape = shape,
        containerColor = AriCard,
        title = { Text(tx("Delete chat?")) },
        text = { Text(tx("This conversation will be removed from this device.")) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(tx("Delete"), color = AriAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tx("Cancel"))
            }
        }
    )
}
