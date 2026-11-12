package com.ariai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriLine
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.util.tx

/** Compact, dismissible notice for a locally saved crash report. */
@Composable
internal fun CrashReportNotice(
    onCopyReport: () -> Unit,
    onDeleteReport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AriCard),
        border = BorderStroke(1.dp, AriLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx("Crash report saved"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = tx("A diagnostic report from the last session is stored on this device."),
                        style = MaterialTheme.typography.bodySmall,
                        color = AriMuted
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(tx("Dismiss"))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDeleteReport) {
                    Text(tx("Delete report"))
                }
                Button(
                    onClick = onCopyReport,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tx("Copy report"))
                }
            }
        }
    }
}
