package com.bookisham.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/** Covers, decoded once and kept while there is room. */
class CoverStore(private val api: ApiClient) {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun cached(bookId: String): Bitmap? = cache.get(bookId)

    suspend fun load(bookId: String): Bitmap? {
        cache.get(bookId)?.let { return it }
        val bytes = api.cover(bookId) ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        cache.put(bookId, bitmap)
        return bitmap
    }

    fun clear() {
        cache.evictAll()
    }
}
