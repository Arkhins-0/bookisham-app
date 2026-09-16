package com.bookisham.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bookisham.app.data.ContentUriRequestBody
import com.bookisham.app.data.PickedFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull

private val BOOK_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/epub+zip",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
)

/** A launcher that opens the system document picker restricted to PDF/EPUB/Word, and hands back a [PickedFile]. */
@Composable
fun rememberBookFilePicker(onPicked: (PickedFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) onPicked(toPickedFile(context, uri, fallbackType = "application/octet-stream"))
    }
    return remember(launcher) { { launcher.launch(BOOK_MIME_TYPES) } }
}

/** A launcher that opens the system image picker, and hands back a [PickedFile]. */
@Composable
fun rememberCoverPicker(onPicked: (PickedFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onPicked(toPickedFile(context, uri, fallbackType = "image/*"))
    }
    return remember(launcher) { { launcher.launch("image/*") } }
}

private fun toPickedFile(context: android.content.Context, uri: Uri, fallbackType: String): PickedFile {
    val resolver = context.contentResolver
    val name = ContentUriRequestBody.displayName(resolver, uri)
    val type = resolver.getType(uri)?.toMediaTypeOrNull() ?: fallbackType.toMediaTypeOrNull()!!
    return PickedFile(uri = uri, name = name, mediaType = type, resolver = resolver)
}
