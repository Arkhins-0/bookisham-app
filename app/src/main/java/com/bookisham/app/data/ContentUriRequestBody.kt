package com.bookisham.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

/**
 * A file the admin picked from the system's document or photo picker, sent
 * to the server as a stream rather than loaded whole into memory — a book
 * can be up to 200 MB.
 */
class ContentUriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType,
) : RequestBody() {
    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long =
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        } ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val stream = resolver.openInputStream(uri) ?: throw IOException("Could not open the selected file.")
        stream.use { input -> sink.writeAll(input.source()) }
    }

    companion object {
        /** The name the picker's Uri reports, falling back to the id if the provider has none. */
        fun displayName(resolver: ContentResolver, uri: Uri): String =
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "file"
    }
}
