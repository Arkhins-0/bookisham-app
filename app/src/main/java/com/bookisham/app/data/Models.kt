package com.bookisham.app.data

import com.bookisham.app.Config
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

/** One book on a shelf: GET /api/library and GET /api/browse. Price and discount are set by the admin, per book. */
@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val format: String = "pdf",
    val pages: Int,
    val page: Int? = null,
    val unlocked: Boolean = true,
    val price: Double? = null,
    val discountPercent: Int? = null,
) {
    val progressPercent: Int
        get() = if (page == null || page <= 0 || pages <= 0) 0 else (page * 100 / pages).coerceIn(0, 100)

    val hasDiscount: Boolean
        get() = price != null && price > 0 && (discountPercent ?: 0) > 0

    /** [price] after the discount, or [price] unchanged when there is none. */
    val discountedPrice: Double?
        get() = price?.let { if (hasDiscount) it * (100 - (discountPercent ?: 0)) / 100.0 else it }
}

/** "₹499", or "₹399.50" when the price isn't a whole number. */
fun formatPrice(value: Double): String {
    val rounded = Math.round(value * 100) / 100.0
    val text = if (rounded == Math.floor(rounded)) rounded.toLong().toString() else "%.2f".format(rounded)
    return "${Config.CURRENCY_SYMBOL}$text"
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

/** One entry in the reader's notification feed: GET /api/notifications. */
@Serializable
data class Notification(
    val id: String,
    val message: String,
    val type: String = "info", // "approved" | "rejected" | "info"
    val read: Boolean = false,
    val createdAt: String,
) {
    val createdAtText: String
        get() = runCatching {
            Instant.parse(createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
        }.getOrDefault(createdAt)
}

@Serializable
internal data class NotificationList(val notifications: List<Notification> = emptyList())

/** GET /api/app-version — the latest GitHub release of this app. */
@Serializable
data class AppVersionInfo(
    val version: String,
    val releaseUrl: String,
    val apkUrl: String? = null,
    val notes: String = "",
)

/** GET /api/legal — the Terms & Conditions and Privacy Policy, as data, rendered natively. */
@Serializable
data class LegalSection(val heading: String, val paragraphs: List<String> = emptyList())

@Serializable
data class LegalDoc(
    val slug: String = "",
    val title: String,
    val updated: String = "",
    val sections: List<LegalSection> = emptyList(),
)

@Serializable
data class LegalDocs(val terms: LegalDoc, val privacy: LegalDoc)

/** POST /api/purchase-requests — what comes back right after submitting one. */
@Serializable
data class PurchaseRequestResult(val id: String, val status: String = "pending")

/* Request bodies */

@Serializable
internal data class LoginBody(val email: String, val password: String)

@Serializable
internal data class SignupBody(val name: String, val phone: String, val email: String, val password: String)

@Serializable
internal data class ProgressBody(val bookId: String, val page: Int)

@Serializable
internal data class AccountPatch(
    val name: String? = null,
    val currentPassword: String? = null,
    val newPassword: String? = null,
)
