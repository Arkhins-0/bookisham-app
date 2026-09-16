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
import com.bookisham.app.data.Book
import com.bookisham.app.data.Me
import com.bookisham.app.data.UnauthorizedException
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

    init {
        refresh()
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
