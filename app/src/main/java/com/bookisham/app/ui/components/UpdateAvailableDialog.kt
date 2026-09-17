package com.bookisham.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.bookisham.app.data.AppVersionInfo
import com.bookisham.app.ui.theme.Ember

/** Shown once per app open when a newer GitHub release exists. */
@Composable
fun UpdateAvailableDialog(info: AppVersionInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("Bookisham v${info.version} is available. Update from your Account page any time.") },
        confirmButton = { TextButton(onClick = onUpdate) { Text("Update", color = Ember) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
}
