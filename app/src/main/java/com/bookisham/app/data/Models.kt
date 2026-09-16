package com.bookisham.app.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String = "",
    val role: String = "user",
) {
    val isAdmin: Boolean get() = role == "admin"
    val firstName: String get() = name.trim().split(" ").firstOrNull().orEmpty()
}

@Serializable
data class Contact(val whatsapp: String = "", val email: String = "")

/** GET /api/me */
@Serializable
data class Me(
    val user: User,
    val expiresAt: String,
    val contact: Contact = Contact(),
) {
    /** "Signed in until ..." in the local time zone. */
    val expiresText: String
        get() = runCatching {
            Instant.parse(expiresAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
        }.getOrDefault(expiresAt)
}

/** One book on a shelf: GET /api/library and GET /api/browse. */
@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val format: String = "pdf",
    val pages: Int,
    val page: Int? = null,
    val unlocked: Boolean = true,
) {
    val progressPercent: Int
        get() = if (page == null || page <= 0 || pages <= 0) 0 else (page * 100 / pages).coerceIn(0, 100)
}

@Serializable
data class BookList(val books: List<Book> = emptyList())

/** GET /api/books/{id}: what the reader needs to open a book. */
@Serializable
data class BookOpen(
    val id: String,
    val title: String,
    val author: String = "",
    val format: String = "pdf",
    val pages: Int,
    val startPage: Int = 1,
    val keyHex: String,
    val watermark: String = "",
)

@Serializable
data class LoginResponse(val role: String = "user", val name: String = "")

@Serializable
data class ApiError(val error: String = "")

@Serializable
data class Ok(val ok: Boolean = true, val name: String? = null)

/* Request bodies */

@Serializable
internal data class LoginBody(val email: String, val password: String)

@Serializable
internal data class ProgressBody(val bookId: String, val page: Int)

@Serializable
internal data class AccountPatch(
    val name: String? = null,
    val currentPassword: String? = null,
    val newPassword: String? = null,
)
