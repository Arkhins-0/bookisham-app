package com.bookisham.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.Book
import com.bookisham.app.data.formatPrice
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.PaperDeep

/** A cover, fetched through the API with the session and kept in memory. */
@Composable
fun CoverImage(bookId: String, modifier: Modifier = Modifier, locked: Boolean = false) {
    val app = LocalApp.current
    var bitmap by remember(bookId) { mutableStateOf(app.covers.cached(bookId)) }
    LaunchedEffect(bookId) {
        if (bitmap == null) bitmap = runCatching { app.covers.load(bookId) }.getOrNull()
    }
    Box(modifier.background(PaperDeep)) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(if (locked) 0.65f else 1f),
                contentScale = ContentScale.Crop,
                colorFilter = if (locked) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
            )
        }
    }
}

/** A book on a shelf: cover, progress, title, author and where the reader is. */
@Composable
fun BookCard(book: Book, unlocked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val page = book.page ?: 0
    Column(modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Ink.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(12.dp)),
        ) {
            CoverImage(book.id, Modifier.fillMaxSize(), locked = !unlocked)
            if (!unlocked) {
                Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            if (unlocked && page > 0) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.2f)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(book.progressPercent / 100f).background(Ember))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (book.author.isNotBlank()) {
            Text(book.author, style = MaterialTheme.typography.bodySmall, color = InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        val caption = when {
            !unlocked -> "Locked"
            page > 0 -> "Page $page of ${book.pages}"
            else -> "${book.pages} pages"
        }
        Text(
            caption.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
            color = InkFaint,
        )
        book.price?.let { PriceRow(book) }
    }
}

/** The price the admin set for this book, with the discount applied when there is one. */
@Composable
private fun PriceRow(book: Book) {
    Spacer(Modifier.height(4.dp))
    if (book.hasDiscount) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                book.discountedPrice?.let { formatPrice(it) }.orEmpty(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = EmberDark,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                book.price?.let { formatPrice(it) }.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                color = InkFaint,
            )
        }
        Text(
            "${book.discountPercent}% off",
            style = MaterialTheme.typography.bodySmall,
            color = Ember,
        )
    } else {
        Text(
            book.price?.let { formatPrice(it) }.orEmpty(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink,
        )
    }
}
