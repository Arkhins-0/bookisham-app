package com.bookisham.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * The session token, in app-private storage (backups are off in the
 * manifest), and the page each book was last open on so the reader can land
 * there before the server has answered.
 */
class SessionStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bookisham", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }.apply()
        }

    val signedIn: Boolean get() = token != null

    fun rememberPage(bookId: String, page: Int) {
        prefs.edit().putInt("page:$bookId", page).apply()
    }

    fun lastPage(bookId: String): Int? = prefs.getInt("page:$bookId", 0).takeIf { it > 0 }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
