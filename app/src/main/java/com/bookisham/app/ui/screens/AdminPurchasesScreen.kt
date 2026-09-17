package com.bookisham.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.AdminPurchaseRequest
import com.bookisham.app.data.formatPrice
import com.bookisham.app.ui.AdminPurchasesViewModel
import com.bookisham.app.ui.components.AdminSection
import com.bookisham.app.ui.components.AdminSectionTabs
import com.bookisham.app.ui.components.EmptyCard
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.PaperCard
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.InkFaint

/**
 * Purchase requests awaiting a decision: the reader, the book, what they
 * say they paid, and the screenshot to check it by eye. Approving grants
 * the book; rejecting does not — either way the reader is notified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPurchasesScreen(onOpenReaders: () -> Unit, onOpenBooks: () -> Unit, onSignedOut: () -> Unit) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "admin-purchases") { AdminPurchasesViewModel(app) }
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }
    var screenshotFor by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = vm.refreshing,
        onRefresh = { vm.load(byUser = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    AdminSectionTabs(AdminSection.Purchases) {
                        when (it) {
                            AdminSection.Readers -> onOpenReaders()
                            AdminSection.Books -> onOpenBooks()
                            AdminSection.Purchases -> {}
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Eyebrow("Purchases")
                    Spacer(Modifier.height(8.dp))
                    Text("Purchases", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Readers who paid by UPI and attached a screenshot. Approve to grant the book, or reject — either way they're notified.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint,
                    )
                    vm.actionError?.let {
                        Spacer(Modifier.height(12.dp))
                        ErrorNote(it)
                    }
                }
            }

            val requests = vm.requests
            val error = vm.error
            when {
                requests == null && error != null -> item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ErrorNote(error)
                        Spacer(Modifier.height(12.dp))
                        Pill("Try again", onClick = { vm.load() }, style = PillStyle.Ghost, compact = true)
                    }
                }
                requests == null -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Ember)
                    }
                }
                requests.isEmpty() -> item {
                    Box(Modifier.padding(horizontal = 20.dp)) { EmptyCard("No purchase requests yet", "") }
                }
                else -> items(requests, key = { it.id }) { request ->
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        PurchaseRequestRow(
                            request = request,
                            busy = vm.busyId == request.id,
                            onViewScreenshot = { screenshotFor = request.id },
                            onApprove = { vm.act(request.id, "approve") },
                            onReject = { vm.act(request.id, "reject") },
                        )
                    }
                }
            }
        }
    }

    screenshotFor?.let { id ->
        ScreenshotDialog(requestId = id, onDismiss = { screenshotFor = null })
    }
}

private val STATUS_COLOR = mapOf("approved" to Ember, "rejected" to EmberDark, "pending" to InkFaint)

@Composable
private fun PurchaseRequestRow(
    request: AdminPurchaseRequest,
    busy: Boolean,
    onViewScreenshot: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    PaperCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(request.bookTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${request.displayUser} · ${request.userEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatPrice(request.amount), style = MaterialTheme.typography.titleMedium, color = Ember)
                Text(
                    request.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = STATUS_COLOR[request.status] ?: InkFaint,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Screenshot", onClick = onViewScreenshot, style = PillStyle.Ghost, icon = Icons.Outlined.ImageIcon, compact = true)
            if (request.status == "pending") {
                Pill(
                    if (busy) "…" else "Approve",
                    onClick = onApprove,
                    style = PillStyle.Primary,
                    icon = Icons.Filled.Check,
                    compact = true,
                    enabled = !busy,
                )
                Pill(
                    if (busy) "…" else "Reject",
                    onClick = onReject,
                    style = PillStyle.Ghost,
                    icon = Icons.Filled.Close,
                    compact = true,
                    enabled = !busy,
                )
            }
        }
    }
}

@Composable
private fun ScreenshotDialog(requestId: String, onDismiss: () -> Unit) {
    val app = LocalApp.current
    var bitmap by remember(requestId) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(requestId) { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        val bytes = runCatching { app.api.purchaseRequestScreenshot(requestId) }.getOrNull()
        bitmap = bytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
        failed = bitmap == null
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close", color = Ember) }
        },
        title = { Text("Payment screenshot") },
        text = {
            Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                val bmp = bitmap
                when {
                    bmp != null -> Image(bitmap = bmp.asImageBitmap(), contentDescription = "Payment screenshot")
                    failed -> Text("Could not load the screenshot.", color = InkFaint)
                    else -> CircularProgressIndicator(color = Ember)
                }
            }
        },
    )
}
