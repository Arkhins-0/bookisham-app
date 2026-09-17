package com.bookisham.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.LocalApp
import com.bookisham.app.R
import com.bookisham.app.data.Book
import com.bookisham.app.data.PickedFile
import com.bookisham.app.data.formatPrice
import com.bookisham.app.ui.BuyBookViewModel
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint

/**
 * Buy a book by UPI: scan the code, pay by hand in any UPI app, attach a
 * screenshot as proof, and submit. There is no payment gateway — an admin
 * looks at the screenshot and decides, which is why the success state says
 * "within 24 hours" rather than "done".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyBookDialog(books: List<Book>, initialBookId: String, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "buy") { BuyBookViewModel(app) }
    val selected = remember(books, initialBookId) { books.firstOrNull { it.id == initialBookId } }

    var screenshot by remember { mutableStateOf<PickedFile?>(null) }
    val pickScreenshot = rememberCoverPicker { screenshot = it }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            if (vm.done) {
                Column(Modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Ember, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Thanks — request sent", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "We'll review your payment and approve it within 24 hours. You'll get a notification either way.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Pill("Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
                return@Column
            }

            Text("Buy this book", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(selected?.title.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = InkFaint)
            Spacer(Modifier.height(20.dp))

            PaperCard {
                Image(
                    painterResource(R.drawable.payment_qr),
                    contentDescription = "UPI payment QR code",
                    modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally).padding(bottom = 4.dp),
                )
                Text(
                    "UPI ID: ${Config.UPI_ID}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Scan to pay with any UPI app",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                val amount = selected?.discountedPrice
                Text(
                    amount?.let { formatPrice(it) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ember,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Amount to pay",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))

            FieldLabel("Payment screenshot")
            Pill(
                screenshot?.name ?: "Choose image",
                onClick = pickScreenshot,
                style = PillStyle.Ghost,
                icon = Icons.Outlined.ImageIcon,
            )

            vm.error?.let {
                Spacer(Modifier.height(14.dp))
                ErrorNote(it)
            }

            Spacer(Modifier.height(20.dp))
            Pill(
                if (vm.submitting) "Submitting…" else "Submit",
                onClick = {
                    val book = selected
                    val file = screenshot
                    if (book != null && file != null) vm.submit(book.id, file)
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Check,
                enabled = !vm.submitting && selected != null && screenshot != null,
            )
        }
    }
}
