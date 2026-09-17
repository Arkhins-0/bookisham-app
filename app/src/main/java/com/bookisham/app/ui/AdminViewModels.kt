package com.bookisham.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookisham.app.BookishamApplication
import com.bookisham.app.data.AdminBook
import com.bookisham.app.data.AdminPurchaseRequest
import com.bookisham.app.data.AdminUser
import com.bookisham.app.data.AdminUserDetail
import com.bookisham.app.data.PickedFile
import com.bookisham.app.data.UnauthorizedException
import kotlinx.coroutines.launch

/** The reader list: GET /api/admin/users, and creating a new one. */
class AdminUsersViewModel(private val app: BookishamApplication) : ViewModel() {
    var users: List<AdminUser>? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var signedOut: Boolean by mutableStateOf(false)
        private set

    var creating: Boolean by mutableStateOf(false)
        private set
    var createError: String? by mutableStateOf(null)
        private set
    var created: Pair<String, String>? by mutableStateOf(null) // email, password — shown once

    init {
        load()
    }

    fun load(byUser: Boolean = false) {
        viewModelScope.launch {
            refreshing = byUser
            error = null
            try {
                users = app.api.adminUsers()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "The reader list could not be loaded."
            }
            refreshing = false
        }
    }

    fun createUser(email: String, name: String, password: String) {
        if (creating) return
        creating = true
        createError = null
        viewModelScope.launch {
            try {
                val result = app.api.adminCreateUser(email.trim().lowercase(), name.trim(), password)
                created = result.email to result.password
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                createError = e.message ?: "Could not create the account."
            }
            creating = false
        }
    }

    fun dismissCreated() {
        created = null
    }
}

/** One reader in full: their account, every book, and the actions on them. */
class AdminUserDetailViewModel(private val app: BookishamApplication, val userId: String) : ViewModel() {
    var detail: AdminUserDetail? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
    var signedOut: Boolean by mutableStateOf(false)
        private set
    var deleted: Boolean by mutableStateOf(false)
        private set

    var busyAction: String? by mutableStateOf(null)
        private set
    var resetPassword: String? by mutableStateOf(null)

    var savingBooks: Boolean by mutableStateOf(false)
        private set
    var booksSaved: Boolean by mutableStateOf(false)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            error = null
            try {
                detail = app.api.adminUserDetail(userId)
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "This reader could not be loaded."
            }
        }
    }

    fun act(action: String) {
        if (busyAction != null) return
        busyAction = action
        viewModelScope.launch {
            try {
                val result = app.api.adminUserAction(userId, action)
                if (action == "reset-password") resetPassword = result.password
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "That did not work."
            }
            busyAction = null
        }
    }

    fun delete() {
        if (busyAction != null) return
        busyAction = "delete"
        viewModelScope.launch {
            try {
                app.api.adminDeleteUser(userId)
                deleted = true
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "Could not delete."
            }
            busyAction = null
        }
    }

    fun saveBooks(bookIds: List<String>) {
        if (savingBooks) return
        savingBooks = true
        booksSaved = false
        viewModelScope.launch {
            try {
                app.api.adminSetUserBooks(userId, bookIds)
                booksSaved = true
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "Could not save."
            }
            savingBooks = false
        }
    }
}

/** The book list: GET /api/admin/books, uploading, editing and deleting. */
class AdminBooksViewModel(private val app: BookishamApplication) : ViewModel() {
    var books: List<AdminBook>? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var signedOut: Boolean by mutableStateOf(false)
        private set

    var saving: Boolean by mutableStateOf(false)
        private set
    var saveError: String? by mutableStateOf(null)
    var saveDone: String? by mutableStateOf(null)

    var deletingId: String? by mutableStateOf(null)
        private set

    init {
        load()
    }

    fun load(byUser: Boolean = false) {
        viewModelScope.launch {
            refreshing = byUser
            error = null
            try {
                books = app.api.adminBooks()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "The book list could not be loaded."
            }
            refreshing = false
        }
    }

    fun upload(
        title: String,
        author: String,
        allUsers: Boolean,
        file: PickedFile,
        cover: PickedFile?,
        price: String,
        discountPercent: String,
    ) {
        if (saving) return
        saving = true
        saveError = null
        saveDone = null
        viewModelScope.launch {
            try {
                val result = app.api.adminSaveBook(
                    bookId = null,
                    title = title.trim(),
                    author = author.trim(),
                    allUsers = allUsers,
                    file = file,
                    cover = cover,
                    price = price.trim().ifBlank { null },
                    discountPercent = discountPercent.trim().ifBlank { null },
                )
                saveDone = "“${result.title}” added — ${result.pages} pages."
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                saveError = e.message ?: "Upload failed."
            }
            saving = false
        }
    }

    fun edit(
        bookId: String,
        title: String,
        author: String,
        file: PickedFile?,
        cover: PickedFile?,
        price: String,
        discountPercent: String,
    ) {
        if (saving) return
        saving = true
        saveError = null
        saveDone = null
        viewModelScope.launch {
            try {
                val result = app.api.adminSaveBook(
                    bookId = bookId,
                    title = title.trim(),
                    author = author.trim(),
                    file = file,
                    cover = cover,
                    price = price.trim().ifBlank { null },
                    discountPercent = discountPercent.trim().ifBlank { null },
                )
                saveDone = "Saved — ${result.pages} pages."
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                saveError = e.message ?: "Could not save."
            }
            saving = false
        }
    }

    fun delete(bookId: String) {
        if (deletingId != null) return
        deletingId = bookId
        viewModelScope.launch {
            try {
                app.api.adminDeleteBook(bookId)
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "Could not delete."
            }
            deletingId = null
        }
    }

    fun clearSaveState() {
        saveError = null
        saveDone = null
    }
}

/** Purchase requests awaiting the admin's eye: GET /api/admin/purchase-requests, and deciding on them. */
class AdminPurchasesViewModel(private val app: BookishamApplication) : ViewModel() {
    var requests: List<AdminPurchaseRequest>? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var signedOut: Boolean by mutableStateOf(false)
        private set

    var busyId: String? by mutableStateOf(null)
        private set
    var actionError: String? by mutableStateOf(null)

    init {
        load()
    }

    fun load(byUser: Boolean = false) {
        viewModelScope.launch {
            refreshing = byUser
            error = null
            try {
                requests = app.api.adminPurchaseRequests()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                error = e.message ?: "Purchase requests could not be loaded."
            }
            refreshing = false
        }
    }

    fun act(id: String, action: String) {
        if (busyId != null) return
        busyId = id
        actionError = null
        viewModelScope.launch {
            try {
                app.api.adminPurchaseRequestAction(id, action)
                load()
            } catch (e: UnauthorizedException) {
                signedOut = true
            } catch (e: Exception) {
                actionError = e.message ?: "That did not work."
            }
            busyId = null
        }
    }
}
