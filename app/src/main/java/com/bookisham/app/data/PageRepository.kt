package com.bookisham.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pages of one book, for one session.
 *
 * A page arrives as an opaque byte stream: PNG bytes XORed with a key the
 * server derived from the session and the book (see pageKey in lib/books.ts
 * on the server). It is unwrapped here, decoded, and kept in a memory-bounded
 * cache so a long book costs no more than a short one. Nothing is ever
 * written to disk.
 */
class PageRepository(
    private val api: ApiClient,
    private val bookId: String,
    keyHex: String,
    private val scale: Int,
) {
    private val key: ByteArray = keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private val cache = object : LruCache<Int, Bitmap>(cacheBudget()) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }
    private val inflight = HashMap<Int, Deferred<Bitmap>>()
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun cached(page: Int): Bitmap? = cache.get(page)

    suspend fun load(page: Int): Bitmap {
        cache.get(page)?.let { return it }
        val job = lock.withLock { inflight.getOrPut(page) { scope.async { fetch(page) } } }
        try {
            return job.await()
        } finally {
            lock.withLock { inflight.remove(page, job) }
        }
    }

    /** Warm the cache without caring whether it works. */
    fun prefetch(page: Int) {
        if (cache.get(page) != null) return
        scope.async { runCatching { load(page) } }
    }

    private suspend fun fetch(page: Int): Bitmap {
        val bytes = api.page(bookId, page, scale)
        if (key.isNotEmpty()) {
            for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw ApiException(500, "This page could not be rendered.")
        cache.put(page, bitmap)
        return bitmap
    }

    fun close() {
        scope.cancel()
        cache.evictAll()
    }

    private companion object {
        /** A third of what the process may use, capped so a big phone does not hoard. */
        fun cacheBudget(): Int {
            val max = Runtime.getRuntime().maxMemory()
            return (max / 3).coerceAtMost(256L * 1024 * 1024).toInt()
        }
    }
}
