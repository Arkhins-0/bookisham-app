package com.bookisham.app.data

import com.bookisham.app.BuildConfig
import com.bookisham.app.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

open class ApiException(val status: Int, message: String) : IOException(message)

/** The server said 401: the session is gone (expired, signed out elsewhere, or the account was disabled). */
class UnauthorizedException : ApiException(401, "Please sign in.")

/**
 * The Bookisham HTTP API, spoken with the session cookie the server set at
 * sign-in. Every call runs on the IO dispatcher and throws [ApiException]
 * carrying the message the server sent when it fails.
 */
class ApiClient(private val session: SessionStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Headers and the session cookie every request carries, before a body is attached. */
    private fun authedBuilder(path: String): Request.Builder {
        val builder = Request.Builder()
            .url(Config.BASE_URL + path)
            .header("Accept", "application/json, */*")
            .header("User-Agent", "Bookisham Android/${BuildConfig.VERSION_NAME}")
            .cacheControl(CacheControl.FORCE_NETWORK)
        session.token?.let { builder.header("Cookie", "$COOKIE=$it") }
        return builder
    }

    private fun request(path: String, method: String, body: String?): Request =
        authedBuilder(path)
            .apply { if (method == "GET") get() else method(method, (body ?: "{}").toRequestBody(jsonType)) }
            .build()

    /** The raw response. The caller must close it. */
    private suspend fun raw(path: String, method: String = "GET", body: String? = null): Response =
        withContext(Dispatchers.IO) {
            try {
                http.newCall(request(path, method, body)).execute()
            } catch (e: IOException) {
                throw ApiException(0, "No connection. Please try again.")
            }
        }

    /** Same as [raw], but with a caller-built body — multipart uploads. */
    private suspend fun rawBody(path: String, method: String, body: RequestBody): Response =
        withContext(Dispatchers.IO) {
            try {
                http.newCall(authedBuilder(path).method(method, body).build()).execute()
            } catch (e: IOException) {
                throw ApiException(0, "No connection. Please try again.")
            }
        }

    private suspend inline fun <reified T> callBody(path: String, method: String, body: RequestBody): T =
        withContext(Dispatchers.IO) {
            rawBody(path, method, body).use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw failure(response.code, text)
                json.decodeFromString<T>(text)
            }
        }

    private fun failure(code: Int, text: String, signOutOn401: Boolean = true): ApiException {
        if (code == 401 && signOutOn401) {
            session.token = null
            return UnauthorizedException()
        }
        val message = runCatching { json.decodeFromString<ApiError>(text).error }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Something went wrong ($code)."
        return ApiException(code, message)
    }

    private suspend inline fun <reified T> call(path: String, method: String = "GET", body: String? = null): T =
        withContext(Dispatchers.IO) {
            raw(path, method, body).use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw failure(response.code, text)
                json.decodeFromString<T>(text)
            }
        }

    /* -- Auth ----------------------------------------------------------- */

    suspend fun login(email: String, password: String): LoginResponse = withContext(Dispatchers.IO) {
        session.token = null
        raw("/api/auth/login", "POST", json.encodeToString(LoginBody(email.trim(), password))).use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw failure(response.code, text, signOutOn401 = false)
            val token = response.headers("Set-Cookie")
                .map { it.substringBefore(';').trim() }
                .firstOrNull { it.startsWith("$COOKIE=") }
                ?.substringAfter('=')
                ?.takeIf { it.isNotBlank() }
                ?: throw ApiException(500, "The server did not return a session.")
            session.token = token
            json.decodeFromString<LoginResponse>(text)
        }
    }

    suspend fun logout() {
        runCatching { raw("/api/auth/logout", "POST", "{}").close() }
        session.clear()
    }

    suspend fun me(): Me = call("/api/me")

    /* -- Books ---------------------------------------------------------- */

    suspend fun library(): List<Book> = call<BookList>("/api/library").books

    suspend fun browse(): List<Book> = call<BookList>("/api/browse").books

    suspend fun openBook(id: String): BookOpen = call("/api/books/$id")

    /** The cover as PNG bytes, or null when the book has none. */
    suspend fun cover(id: String): ByteArray? = withContext(Dispatchers.IO) {
        raw("/api/books/$id/cover").use { response ->
            when {
                response.code == 401 -> throw failure(401, "")
                !response.isSuccessful -> null
                else -> response.body?.bytes()
            }
        }
    }

    /** One page, still wrapped with the session key. See PageRepository. */
    suspend fun page(bookId: String, page: Int, scale: Int): ByteArray = withContext(Dispatchers.IO) {
        raw("/api/read/$bookId/$page?s=$scale").use { response ->
            if (!response.isSuccessful) throw failure(response.code, response.body?.string().orEmpty())
            response.body?.bytes() ?: throw ApiException(500, "A page could not be loaded.")
        }
    }

    suspend fun saveProgress(bookId: String, page: Int) {
        raw("/api/progress", "POST", json.encodeToString(ProgressBody(bookId, page))).close()
    }

    /* -- Account -------------------------------------------------------- */

    suspend fun updateName(name: String): Ok =
        call("/api/account", "PATCH", json.encodeToString(AccountPatch(name = name)))

    suspend fun changePassword(currentPassword: String, newPassword: String): Ok =
        call("/api/account", "PATCH", json.encodeToString(AccountPatch(currentPassword = currentPassword, newPassword = newPassword)))

    /* -- Admin: readers --------------------------------------------------- */

    suspend fun adminUsers(): List<AdminUser> = call<AdminUserList>("/api/admin/users").users

    suspend fun adminUserDetail(id: String): AdminUserDetail = call("/api/admin/users/$id")

    /** The password comes back once, in clear — show it, then let it go. */
    suspend fun adminCreateUser(email: String, name: String, password: String?): CreatedUser =
        call("/api/admin/users", "POST", json.encodeToString(CreateUserBody(email, name, password?.takeIf { it.isNotBlank() })))

    suspend fun adminUserAction(id: String, action: String): UserActionResult =
        call("/api/admin/users/$id", "PATCH", json.encodeToString(UserActionBody(action)))

    suspend fun adminDeleteUser(id: String) {
        raw("/api/admin/users/$id", "DELETE").use { response ->
            if (!response.isSuccessful) throw failure(response.code, response.body?.string().orEmpty())
        }
    }

    suspend fun adminSetUserBooks(id: String, bookIds: List<String>): SetBooksResult =
        call("/api/admin/users/$id/books", "PUT", json.encodeToString(SetBooksBody(bookIds)))

    /* -- Admin: books ------------------------------------------------------ */

    suspend fun adminBooks(): List<AdminBook> = call<AdminBookList>("/api/admin/books").books

    suspend fun adminDeleteBook(id: String) {
        raw("/api/admin/books/$id", "DELETE").use { response ->
            if (!response.isSuccessful) throw failure(response.code, response.body?.string().orEmpty())
        }
    }

    /**
     * Upload a new book, or (with [bookId]) replace one already on the
     * shelf — the same multipart shape either way. [file] and [cover] are
     * streamed from their content Uris, never held whole in memory.
     */
    suspend fun adminSaveBook(
        bookId: String?,
        title: String?,
        author: String?,
        allUsers: Boolean = false,
        file: PickedFile? = null,
        cover: PickedFile? = null,
    ): UploadedBook {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            title?.let { addFormDataPart("title", it) }
            author?.let { addFormDataPart("author", it) }
            if (allUsers) addFormDataPart("allUsers", "1")
            file?.let { addFormDataPart("file", it.name, ContentUriRequestBody(it.resolver, it.uri, it.mediaType)) }
            cover?.let { addFormDataPart("cover", it.name, ContentUriRequestBody(it.resolver, it.uri, it.mediaType)) }
        }.build()

        val path = if (bookId != null) "/api/admin/books/$bookId" else "/api/admin/books"
        val method = if (bookId != null) "PATCH" else "POST"
        return callBody(path, method, body)
    }

    private companion object {
        const val COOKIE = "bk_session"
    }
}

/** A file picked from the system's document or image picker, ready to stream into a multipart body. */
data class PickedFile(
    val uri: android.net.Uri,
    val name: String,
    val mediaType: okhttp3.MediaType,
    val resolver: android.content.ContentResolver,
)
