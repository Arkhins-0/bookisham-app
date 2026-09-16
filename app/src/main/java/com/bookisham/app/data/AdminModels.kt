package com.bookisham.app.data

import kotlinx.serialization.Serializable

/** GET /api/admin/users — one row of the reader list. */
@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val name: String = "",
    val role: String = "user",
    val disabled: Boolean = false,
    val createdAt: String = "",
    val books: Int = 0,
    val lastSeenAt: String? = null,
) {
    val displayName: String get() = name.ifBlank { email }
}

@Serializable
internal data class AdminUserList(val users: List<AdminUser> = emptyList())

/** The account fields of GET /api/admin/users/:id — the same shape, plus `self`. */
@Serializable
data class AdminUserSelf(
    val id: String,
    val email: String,
    val name: String = "",
    val role: String = "user",
    val disabled: Boolean = false,
    val createdAt: String = "",
    val self: Boolean = false,
)

@Serializable
data class AdminDevice(
    val createdAt: String,
    val lastSeenAt: String,
    val expiresAt: String,
    val userAgent: String = "",
)

@Serializable
data class AdminBookRef(val id: String, val title: String, val author: String = "", val pages: Int = 0)

/** GET /api/admin/users/:id — one reader in full, for the assign-books screen. */
@Serializable
data class AdminUserDetail(
    val user: AdminUserSelf,
    val books: List<AdminBookRef> = emptyList(),
    val granted: List<String> = emptyList(),
    val progress: Map<String, Int> = emptyMap(),
    val device: AdminDevice? = null,
)

/** GET /api/admin/books — one row of the book list. */
@Serializable
data class AdminBook(
    val id: String,
    val title: String,
    val author: String = "",
    val format: String = "pdf",
    val pages: Int = 0,
    val readers: Int = 0,
    val createdAt: String = "",
)

@Serializable
internal data class AdminBookList(val books: List<AdminBook> = emptyList())

/** POST /api/admin/users — the password is returned once, in clear. */
@Serializable
data class CreatedUser(
    val id: String,
    val email: String,
    val name: String = "",
    val role: String = "user",
    val password: String,
)

/** PATCH /api/admin/users/:id — a reset also returns the new password once. */
@Serializable
data class UserActionResult(val ok: Boolean = true, val password: String? = null, val name: String? = null)

@Serializable
data class SetBooksResult(val ok: Boolean = true, val bookIds: List<String> = emptyList())

/** POST or PATCH /api/admin/books/:id — the book's shape after the change. */
@Serializable
data class UploadedBook(
    val id: String,
    val title: String,
    val author: String = "",
    val format: String = "pdf",
    val pages: Int = 0,
)

/* Request bodies */

@Serializable
internal data class CreateUserBody(val email: String, val name: String, val password: String? = null)

@Serializable
internal data class UserActionBody(val action: String, val password: String? = null)

@Serializable
internal data class SetBooksBody(val bookIds: List<String>)
