package com.bookisham.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.data.Notification
import com.bookisham.app.ui.NotificationsViewModel
import com.bookisham.app.ui.components.EmptyCard
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.PaperCard
import com.bookisham.app.ui.components.openSafely
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.Paper

/** A reader's own notification feed — purchase decisions today, general messages later. Opening it marks everything read. */
@Composable
fun NotificationsScreen(vm: NotificationsViewModel, onBack: () -> Unit, onSignedOut: () -> Unit) {
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }
    LaunchedEffect(Unit) { vm.markRead() }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = InkFaint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Back", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Notifications", style = MaterialTheme.typography.displaySmall)
        }

        val notifications = vm.notifications
        val error = vm.error
        when {
            notifications == null && error != null -> Box(Modifier.padding(20.dp)) { ErrorNote(error) }
            notifications == null -> {}
            notifications.isEmpty() -> Box(Modifier.padding(20.dp)) { EmptyCard("Nothing here yet", "") }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            ) {
                items(notifications, key = { it.id }) { n ->
                    NotificationRow(n)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: Notification) {
    val uri = LocalUriHandler.current
    val (icon, tint) = iconFor(n.type)

    PaperCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(n.message, style = MaterialTheme.typography.bodyMedium, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(n.createdAtText, style = MaterialTheme.typography.bodySmall, color = InkFaint)
                if (n.type == "rejected" && (Config.WHATSAPP.isNotBlank() || Config.CONTACT_EMAIL.isNotBlank())) {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Think this is wrong? ", style = MaterialTheme.typography.bodySmall, color = InkFaint)
                        Text(
                            if (Config.WHATSAPP.isNotBlank()) "Message us on WhatsApp" else "Email us",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ember,
                            modifier = Modifier.clickable {
                                if (Config.WHATSAPP.isNotBlank()) uri.openSafely("https://wa.me/${Config.WHATSAPP}") else uri.openSafely("mailto:${Config.CONTACT_EMAIL}")
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(type: String): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (type) {
    "approved" -> Icons.Filled.CheckCircle to Ember
    "rejected" -> Icons.Outlined.Cancel to InkFaint
    else -> Icons.Filled.Info to InkFaint
}
