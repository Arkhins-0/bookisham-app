package com.bookisham.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bookisham.app.BookishamApplication
import com.bookisham.app.BuildConfig
import com.bookisham.app.data.AppVersionInfo
import com.bookisham.app.data.Book
import com.bookisham.app.data.Me
import com.bookisham.app.data.Notification
import com.bookisham.app.data.PickedFile
import com.bookisham.app.data.UnauthorizedException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A ViewModel built by [create], scoped to the nearest owner (the activity or the navigation entry). */
@Composable
inline fun <reified VM : ViewModel> rememberViewModel(key: String? = null, crossinline create: () -> VM): VM =
    viewModel(key = key, factory = viewModelFactory { initializer { create() } })

/* -- Who is signed in --------------------------------------------------------- */

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class Failed(val message: String) : SessionState
    data class SignedIn(val me: Me) : SessionState
}

class AppViewModel(private val app: BookishamApplication) : ViewModel() {
    var session: SessionState by mutableStateOf(SessionState.Loading)
        private set

    /** Set only when the latest GitHub release is newer than this build. */
    var updateInfo: AppVersionInfo? by mutableStateOf(null)
        private set
    var updateDismissed: Boolean by mutableStateOf(false)
        private set

    init {
        refresh()
        checkForUpdate()
    }

    /** Checked once per app open — an update popup that reappears on every screen would be worse than none. */
    fun checkForUpdate() {
        viewModelScope.launch {
            val info = runCatching { app.api.appVersion() }.getOrNull() ?: return@launch
            if (isNewerVersion(info.version, BuildConfig.VERSION_NAME)) updateInfo = info
        }
    }

    fun dismissUpdate() {
        updateDismissed = true
    }

    /** Ask the server who the stored token belongs to. */
    fun refresh() {
        viewModelScope.launch {
            if (!app.session.signedIn) {
                session = SessionState.SignedOut
                return@launch
            }
            session = try {
                SessionState.SignedIn(app.api.me())
            } catch (e: UnauthorizedException) {
                SessionState.SignedOut
            } catch (e: Exception) {
                SessionState.Failed(e.message ?: "No connection. Please try again.")
            }
        }
    }

    /** The login screen has stored the token; now find out who we are. */
    fun signedIn() {
        session = SessionState.Loading
        refresh()
    }

    /** A request came back 401: the session ended elsewhere. */
    fun signedOut() {
        app.session.clear()
        app.covers.clear()
        session = SessionState.SignedOut
    }

    fun logout() {
        viewModelScope.launch {
            app.api.logout()
            app.covers.clear()
            session = SessionState.SignedOut
        }
    }

    fun nameChanged(name: String) {
        val current = session as? SessionState.SignedIn ?: return
        session = current.copy(me = current.me.copy(user = current.me.user.copy(name = name)))
    }
}

/** Whether [remote] is a newer version than [local] — dotted numeric segments, non-numeric suffixes ignored. */
fun isNewerVersion(remote: String, local: String): Boolean {
    fun segments(v: String) = v.trim().removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    val r = segments(remote)
    val l = segments(local)
    for (i in 0 until maxOf(r.size, l.size)) {
        val rv = r.getOrElse(i) { 0 }
        val lv = l.getOrElse(i) { 0 }
        if (rv != lv) return rv > lv
    }
    return false
}

/* -- A shelf of books ------------------------------------------------------- */

class ShelfViewModel(private val app: BookishamApplication, private val browse: Boolean) : ViewModel() {
    var books: List<Book>? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var signedOut: Boolean by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load(byUser: Boolean = false) {
        viewModelScope.launch {
            refreshing = byUser
            error = null
            try {
                books = if (browse) app.api.browse() else app.api.library()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "The shelf could not be loaded."
            }
            refreshing = false
        }
    }
}

/* -- Buying a book ------------------------------------------------------------ */

/** Submits one purchase request: a book, a payment screenshot, nothing more. */
class BuyBookViewModel(private val app: BookishamApplication) : ViewModel() {
    var submitting: Boolean by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var done: Boolean by mutableStateOf(false)
        private set

    fun submit(bookId: String, screenshot: PickedFile) {
        if (submitting) return
        submitting = true
        error = null
        viewModelScope.launch {
            try {
                app.api.submitPurchaseRequest(bookId, screenshot)
                done = true
            } catch (e: Exception) {
                error = e.message ?: "Could not submit your request."
            }
            submitting = false
        }
    }
}

/* -- Notifications -------------------------------------------------------------- */

/** The reader's own notification feed — also the source of the bell's unread badge. */
class NotificationsViewModel(private val app: BookishamApplication) : ViewModel() {
    var notifications: List<Notification>? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var signedOut: Boolean by mutableStateOf(false)
        private set

    val unreadCount: Int get() = notifications?.count { !it.read } ?: 0

    init {
        load()
        // Keep the bell honest without a restart: poll once a minute for as long as someone is signed in.
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            try {
                notifications = app.api.notifications()
                error = null
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                if (notifications == null) error = e.message ?: "Notifications could not be loaded."
            }
        }
    }

    /** The feed was opened — mark everything read, here and on the server. */
    fun markRead() {
        val current = notifications ?: return
        if (current.none { !it.read }) return
        notifications = current.map { it.copy(read = true) }
        viewModelScope.launch { runCatching { app.api.markNotificationsRead() } }
    }
}
