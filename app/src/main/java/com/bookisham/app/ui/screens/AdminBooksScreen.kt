package com.bookisham.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.AdminBook
import com.bookisham.app.data.PickedFile
import com.bookisham.app.ui.AdminBooksViewModel
import com.bookisham.app.ui.components.AdminSection
import com.bookisham.app.ui.components.AdminSectionTabs
import com.bookisham.app.ui.components.CoverImage
import com.bookisham.app.ui.components.EmptyCard
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.FieldLabel
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.TextBox
import com.bookisham.app.ui.components.rememberBookFilePicker
import com.bookisham.app.ui.components.rememberCoverPicker
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint

/**
 * Every book in the library. Uploading paginates before the row is
 * written, so a file that will not open never becomes a book; the same is
 * true of replacing one from the edit sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBooksScreen(onPreview: (String) -> Unit, onOpenReaders: () -> Unit, onSignedOut: () -> Unit) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "admin-books") { AdminBooksViewModel(app) }
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }

    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminBook?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminBook?>(null) }

    PullToRefreshBox(
        isRefreshing = vm.refreshing,
        onRefresh = { vm.load(byUser = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    AdminSectionTabs(AdminSection.Books) { if (it == AdminSection.Readers) onOpenReaders() }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Books")
                            Spacer(Modifier.height(8.dp))
                            Text("Books", style = MaterialTheme.typography.displaySmall)
                        }
                        Pill(
                            "Upload",
                            onClick = { editing = null; sheetOpen = true },
                            icon = Icons.Outlined.UploadFile,
                            compact = true,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Everything in the library. A book is only visible to the readers you give it to.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint,
                    )
                }
            }

            val books = vm.books
            val error = vm.error
            when {
                books == null && error != null -> item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ErrorNote(error)
                        Spacer(Modifier.height(12.dp))
                        Pill("Try again", onClick = { vm.load() }, style = PillStyle.Ghost, compact = true)
                    }
                }
                books == null -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Ember)
                    }
                }
                books.isEmpty() -> item {
                    Box(Modifier.padding(horizontal = 20.dp)) { EmptyCard("No books yet", "Upload the first one above.") }
                }
                else -> items(books, key = { it.id }) { book ->
                    BookRow(
                        book = book,
                        deleting = vm.deletingId == book.id,
                        onPreview = { onPreview(book.id) },
                        onEdit = { editing = book; sheetOpen = true },
                        onDelete = { deleteTarget = book },
                    )
                    HorizontalDivider(color = Ink.copy(alpha = 0.06f))
                }
            }
        }
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false; vm.clearSaveState() },
            sheetState = sheetState,
        ) {
            BookFormSheetContent(
                vm = vm,
                editing = editing,
                onClose = { sheetOpen = false },
            )
        }
    }

    deleteTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete “${book.title}”?") },
            text = { Text("Every reader loses access and their place in it. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(book.id); deleteTarget = null }) {
                    Text("Delete", color = EmberDark)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BookRow(
    book: AdminBook,
    deleting: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Box(
            Modifier
                .width(64.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            CoverImage(book.id, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.author.isNotBlank()) {
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = InkFaint, maxLines = 1)
            }
            Text(
                "${book.format.uppercase()} · ${book.pages} pages · ${book.readers} reader${if (book.readers == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Preview", onClick = onPreview, style = PillStyle.Ghost, icon = Icons.Outlined.Visibility, compact = true)
                Pill("Edit", onClick = onEdit, style = PillStyle.Ghost, icon = Icons.Outlined.Edit, compact = true)
                Pill(
                    if (deleting) "…" else "Delete",
                    onClick = onDelete,
                    style = PillStyle.Ghost,
                    icon = Icons.Outlined.Delete,
                    compact = true,
                    enabled = !deleting,
                )
            }
        }
    }
}

@Composable
private fun BookFormSheetContent(vm: AdminBooksViewModel, editing: AdminBook?, onClose: () -> Unit) {
    var title by rememberSaveable(editing?.id) { mutableStateOf(editing?.title ?: "") }
    var author by rememberSaveable(editing?.id) { mutableStateOf(editing?.author ?: "") }
    var allUsers by rememberSaveable(editing?.id) { mutableStateOf(false) }
    var file by remember(editing?.id) { mutableStateOf<PickedFile?>(null) }
    var cover by remember(editing?.id) { mutableStateOf<PickedFile?>(null) }

    val pickBook = rememberBookFilePicker { file = it }
    val pickCover = rememberCoverPicker { cover = it }

    val done = vm.saveDone
    LaunchedEffect(done) {
        if (done != null) {
            kotlinx.coroutines.delay(1200)
            onClose()
        }
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(if (editing != null) "Edit book" else "Upload a book", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            if (editing != null) "Change the title, author, cover, or replace the file itself. Readers keep their page."
            else "PDF, EPUB or Word (.docx). A cover is optional — the first page is used otherwise.",
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
        )
        Spacer(Modifier.height(20.dp))

        FieldLabel("Title")
        TextBox(title, { title = it }, imeAction = ImeAction.Next)
        Spacer(Modifier.height(14.dp))
        FieldLabel("Author")
        TextBox(author, { author = it }, imeAction = ImeAction.Done)
        Spacer(Modifier.height(14.dp))

        FieldLabel(if (editing != null) "Replace the book file (optional)" else "Book file")
        Pill(
            file?.name ?: "Choose file",
            onClick = pickBook,
            style = PillStyle.Ghost,
            icon = Icons.Outlined.AttachFile,
        )
        Spacer(Modifier.height(14.dp))

        FieldLabel("Cover (optional)")
        Pill(
            cover?.name ?: "Choose image",
            onClick = pickCover,
            style = PillStyle.Ghost,
            icon = Icons.Outlined.Image,
        )

        if (editing == null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = allUsers, onCheckedChange = { allUsers = it }, colors = CheckboxDefaults.colors(checkedColor = Ember))
                Spacer(Modifier.width(4.dp))
                Text("Give every reader access immediately", style = MaterialTheme.typography.bodyMedium)
            }
        }

        vm.saveError?.let {
            Spacer(Modifier.height(14.dp))
            ErrorNote(it)
        }
        vm.saveDone?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = InkFaint)
        }

        Spacer(Modifier.height(20.dp))
        Pill(
            when {
                vm.saving && editing == null -> "Uploading and paginating…"
                vm.saving -> "Saving…"
                editing != null -> "Save"
                else -> "Upload"
            },
            onClick = {
                if (editing != null) {
                    vm.edit(editing.id, title, author, file, cover)
                } else {
                    file?.let { vm.upload(title, author, allUsers, it, cover) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Check,
            enabled = !vm.saving && title.isNotBlank() && (editing != null || file != null),
        )
    }
}
