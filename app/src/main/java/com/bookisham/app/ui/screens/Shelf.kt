package com.bookisham.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.LocalApp
import com.bookisham.app.data.Book
import com.bookisham.app.data.Me
import com.bookisham.app.ui.ShelfViewModel
import com.bookisham.app.ui.components.BookCard
import com.bookisham.app.ui.components.BuyBookDialog
import com.bookisham.app.ui.components.EmptyCard
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft

/**
 * The library: the books this reader has been given, and nothing else. An
 * admin sees every book, which is how they check what a reader will see.
 */
@Composable
fun LibraryScreen(me: Me, onOpen: (String) -> Unit, onSignedOut: () -> Unit) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "library") { ShelfViewModel(app, browse = false) }
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }

    val whatsapp = me.contact.whatsapp.ifBlank { Config.WHATSAPP }

    Shelf(
        vm = vm,
        header = {
            Column {
                Eyebrow("Your library")
                Spacer(Modifier.height(8.dp))
                Text(
                    if (me.user.name.isNotBlank()) "Hello, ${me.user.firstName}." else "Your books",
                    style = MaterialTheme.typography.displayMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text("Signed in until ${me.expiresText}", style = MaterialTheme.typography.bodySmall, color = InkFaint)
            }
        },
        empty = {
            EmptyCard(
                title = "Nothing on your shelf yet",
                body = "Books appear here once the admin has added them to your account." +
                    if (whatsapp.isNotBlank()) " Message us on WhatsApp if you are expecting one." else "",
            )
        },
        onBook = { onOpen(it.id) },
    )
}

/**
 * Every book in the library, not just the ones this reader has been given.
 * Recently read books lead the shelf; a locked cover opens nothing but a
 * note pointing the reader at the admin.
 */
@Composable
fun BrowseScreen(me: Me, onOpen: (String) -> Unit, onSignedOut: () -> Unit) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "browse") { ShelfViewModel(app, browse = true) }
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }
    var lockedNote by remember { mutableStateOf(false) }
    var buyFor by remember { mutableStateOf<String?>(null) }

    Shelf(
        vm = vm,
        header = {
            Column {
                Eyebrow("Every book")
                Spacer(Modifier.height(8.dp))
                Text("Browse the library", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recently read books come first. A locked cover means the admin has not given you this one yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
            }
        },
        empty = { EmptyCard("No books in the library yet", "") },
        onBook = { book ->
            when {
                me.user.isAdmin || book.unlocked -> onOpen(book.id)
                book.price != null -> buyFor = book.id
                else -> lockedNote = true
            }
        },
    )

    if (lockedNote) {
        AlertDialog(
            onDismissRequest = { lockedNote = false },
            confirmButton = { TextButton(onClick = { lockedNote = false }) { Text("OK", color = Ember) } },
            title = { Text("Locked", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Ask your admin for access to this book.") },
        )
    }

    buyFor?.let { bookId ->
        BuyBookDialog(books = vm.books.orEmpty(), initialBookId = bookId, onDismiss = { buyFor = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Shelf(
    vm: ShelfViewModel,
    header: @Composable () -> Unit,
    empty: @Composable () -> Unit,
    onBook: (Book) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = vm.refreshing,
        onRefresh = { vm.load(byUser = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }

            val books = vm.books
            val error = vm.error
            when {
                books == null && error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        ErrorNote(error)
                        Spacer(Modifier.height(12.dp))
                        Pill("Try again", onClick = { vm.load() }, style = PillStyle.Ghost, compact = true)
                    }
                }
                books == null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Ember)
                    }
                }
                books.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { empty() }
                else -> items(books, key = { it.id }) { book ->
                    BookCard(book = book, unlocked = book.unlocked, onClick = { onBook(book) })
                }
            }
        }
    }
}
