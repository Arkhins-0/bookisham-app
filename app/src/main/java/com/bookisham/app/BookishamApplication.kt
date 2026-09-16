package com.bookisham.app

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.bookisham.app.data.ApiClient
import com.bookisham.app.data.CoverStore
import com.bookisham.app.data.SessionStore

/** One place for the objects that live as long as the process: the session, the API client, the cover cache. */
class BookishamApplication : Application() {
    val session: SessionStore by lazy { SessionStore(this) }
    val api: ApiClient by lazy { ApiClient(session) }
    val covers: CoverStore by lazy { CoverStore(api) }
}

val LocalApp = staticCompositionLocalOf<BookishamApplication> { error("BookishamApplication is not provided") }
