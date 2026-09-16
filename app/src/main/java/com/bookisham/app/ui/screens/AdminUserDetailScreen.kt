package com.bookisham.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.AdminUserDetail
import com.bookisham.app.ui.AdminUserDetailViewModel
import com.bookisham.app.ui.components.CredentialsCard
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.IconPill
import com.bookisham.app.ui.components.LoadingScreen
import com.bookisham.app.ui.components.PaperCard
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One reader: their account, the actions on it, which books they may open, and their device. */
@Composable
fun AdminUserDetailScreen(userId: String, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "admin-user:$userId") { AdminUserDetailViewModel(app, userId) }

    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }
    if (vm.deleted) LaunchedEffect(Unit) { onBack() }

    var confirmDelete by remember { mutableStateOf(false) }

    val detail = vm.detail
    when {
        detail != null -> UserDetailBody(vm, detail, onBack, onDeleteRequested = { confirmDelete = true })
        vm.error != null -> Column(Modifier.fillMaxSize().padding(20.dp)) {
            ErrorNote(vm.error!!)
            Spacer(Modifier.height(12.dp))
            Pill("Try again", onClick = { vm.load() }, style = PillStyle.Ghost, compact = true)
        }
        else -> LoadingScreen()
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this account?") },
            text = { Text("Their progress and access are removed too. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text("Delete", color = EmberDark)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun UserDetailBody(
    vm: AdminUserDetailViewModel,
    detail: AdminUserDetail,
    onBack: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    val user = detail.user
    var selected by remember(detail.granted) { mutableStateOf(detail.granted.toSet()) }
    val dirty = selected != detail.granted.toSet()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(
                    Modifier.clickable(onClick = onBack),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = InkFaint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("All readers", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
                }
                Spacer(Modifier.height(16.dp))

                Text(user.name.ifBlank { user.email }, style = MaterialTheme.typography.displaySmall)
                val tags = buildList {
                    add(user.email)
                    if (user.role == "admin") add("admin")
                    if (user.disabled) add("disabled")
                }.joinToString(" · ")
                Text(tags, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                Text(
                    "ID: ${user.id}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = InkFaint,
                )

                Spacer(Modifier.height(16.dp))

                vm.resetPassword?.let { password ->
                    CredentialsCard(email = user.email, password = password, onDone = { vm.resetPassword = null })
                    Spacer(Modifier.height(16.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        if (vm.busyAction == "reset-password") "…" else "Reset password",
                        onClick = { vm.act("reset-password") },
                        style = PillStyle.Ghost,
                        icon = Icons.Outlined.Key,
                        compact = true,
                        enabled = vm.busyAction == null,
                    )
                    Pill(
                        if (vm.busyAction == "sign-out") "…" else "Sign out device",
                        onClick = { vm.act("sign-out") },
                        style = PillStyle.Ghost,
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        compact = true,
                        enabled = vm.busyAction == null,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!user.self) {
                        Pill(
                            when {
                                vm.busyAction == "disable" || vm.busyAction == "enable" -> "…"
                                user.disabled -> "Enable"
                                else -> "Disable"
                            },
                            onClick = { vm.act(if (user.disabled) "enable" else "disable") },
                            style = PillStyle.Ghost,
                            icon = if (user.disabled) Icons.Filled.CheckCircle else Icons.Outlined.Block,
                            compact = true,
                            enabled = vm.busyAction == null,
                        )
                        Pill(
                            if (vm.busyAction == "delete") "…" else "Delete",
                            onClick = onDeleteRequested,
                            style = PillStyle.Ghost,
                            icon = Icons.Outlined.Delete,
                            compact = true,
                            enabled = vm.busyAction == null,
                        )
                    }
                }

                vm.error?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorNote(it)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                PaperCard {
                    Text("Books this reader may open", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tick a book and save. Nothing else is visible to them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkFaint,
                    )
                    Spacer(Modifier.height(12.dp))

                    if (detail.books.isEmpty()) {
                        Text("No books uploaded yet.", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
                    } else {
                        detail.books.forEach { book ->
                            val on = selected.contains(book.id)
                            val page = detail.progress[book.id]
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (on) selected - book.id else selected + book.id
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = on, onCheckedChange = null, colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Ember))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(book.title, style = MaterialTheme.typography.bodyMedium)
                                    val caption = buildList {
                                        if (book.author.isNotBlank()) add(book.author)
                                        add("${book.pages} pages")
                                        if (page != null && page > 0) add("on page $page")
                                    }.joinToString(" · ")
                                    Text(caption, style = MaterialTheme.typography.bodySmall, color = InkFaint)
                                }
                            }
                            HorizontalDivider(color = Ink.copy(alpha = 0.06f))
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(
                                if (vm.savingBooks) "Saving…" else "Save access",
                                onClick = { vm.saveBooks(selected.toList()) },
                                icon = Icons.Filled.Check,
                                enabled = dirty && !vm.savingBooks,
                                compact = true,
                            )
                            if (vm.booksSaved && !dirty) {
                                Spacer(Modifier.width(10.dp))
                                Text("Saved.", style = MaterialTheme.typography.bodySmall, color = InkFaint)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${selected.size} of ${detail.books.size} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                PaperCard {
                    Text("Device", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    val device = detail.device
                    if (device != null) {
                        DeviceRow("Signed in", formatInstant(device.createdAt))
                        DeviceRow("Last seen", formatInstant(device.lastSeenAt))
                        DeviceRow("Session ends", formatInstant(device.expiresAt))
                        DeviceRow("Browser", device.userAgent.ifBlank { "—" })
                    } else {
                        Text("Not signed in on any device.", style = MaterialTheme.typography.bodySmall, color = InkFaint)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Joined ${formatInstant(user.createdAt, dateOnly = true)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkFaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = InkFaint)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatInstant(iso: String, dateOnly: Boolean = false): String = runCatching {
    val zoned = Instant.parse(iso).atZone(ZoneId.systemDefault())
    if (dateOnly) {
        zoned.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } else {
        zoned.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }
}.getOrDefault(iso)
