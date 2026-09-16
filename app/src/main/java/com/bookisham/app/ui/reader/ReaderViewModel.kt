package com.bookisham.app.ui.reader

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookisham.app.BookishamApplication
import com.bookisham.app.data.ApiException
import com.bookisham.app.data.BookOpen
import com.bookisham.app.data.PageRepository
import com.bookisham.app.data.UnauthorizedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One open book: its shape, where the reader is, and the pages themselves.
 *
 * The page is remembered locally at once and on the server a moment later,
 * so a reload, or another device, lands on the same page.
 */
class ReaderViewModel(
    private val app: BookishamApplication,
    val bookId: String,
    private val scale: Int,
) : ViewModel() {
    var book: BookOpen? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
    var signedOut: Boolean by mutableStateOf(false)
    var page: Int by mutableIntStateOf(app.session.lastPage(bookId) ?: 1)
        private set

    /** Width over height of each page seen so far, so slots have the right shape before they load. */
    val ratios = mutableStateMapOf<Int, Float>()

    private var pages: PageRepository? = null
    private var saveJob: Job? = null
    private var pendingSave: Int? = null

    init {
        open()
    }

    fun open() {
        error = null
        viewModelScope.launch {
            try {
                val opened = app.api.openBook(bookId)
                pages?.close()
                pages = PageRepository(app.api, bookId, opened.keyHex, scale)
                page = opened.startPage.coerceIn(1, opened.pages)
                book = opened
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "This book could not be opened."
            }
        }
    }

    fun cached(n: Int): Bitmap? = pages?.cached(n)

    suspend fun load(n: Int): Bitmap {
        val repo = pages ?: throw ApiException(0, "The book is not open.")
        val bitmap = repo.load(n)
        if (!ratios.containsKey(n)) ratios[n] = bitmap.width.toFloat() / bitmap.height.toFloat()
        return bitmap
    }

    fun ratioOf(n: Int): Float = ratios[n] ?: ratios[1] ?: DEFAULT_RATIO

    /** The slot crossing the middle of the viewport changed. */
    fun onPage(n: Int) {
        val opened = book ?: return
        val clamped = n.coerceIn(1, opened.pages)
        if (clamped == page) return
        page = clamped
        app.session.rememberPage(bookId, clamped)

        for (d in 1..PREFETCH) {
            if (clamped + d <= opened.pages) pages?.prefetch(clamped + d)
            if (clamped - d >= 1) pages?.prefetch(clamped - d)
        }

        pendingSave = clamped
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            pendingSave = null
            runCatching { app.api.saveProgress(bookId, clamped) }
        }
    }

    override fun onCleared() {
        // A save still waiting for its debounce goes out now, on its own.
        pendingSave?.let { n ->
            CoroutineScope(Dispatchers.IO).launch { runCatching { app.api.saveProgress(bookId, n) } }
        }
        pages?.close()
    }

    companion object {
        /** A4 until the first page says otherwise. */
        const val DEFAULT_RATIO = 1f / 1.4142f
        const val PREFETCH = 2
    }
}
