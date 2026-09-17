package com.bookisham.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.LocalApp
import com.bookisham.app.R
import com.bookisham.app.data.Book
import com.bookisham.app.data.formatPrice
import com.bookisham.app.ui.ShelfViewModel
import com.bookisham.app.ui.components.ContactMenu
import com.bookisham.app.ui.components.CoverImage
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.LogoRow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.Paper
import java.time.Year
import kotlinx.coroutines.launch

private data class Step(val n: String, val title: String, val body: String)

private val steps = listOf(
    Step("01", "Create your account", "Sign up with your name, phone, email and a password — it takes under a minute."),
    Step("02", "Choose your books", "Browse the library, with each book's price and any discount shown up front."),
    Step("03", "Read anywhere", "Sign in on your phone, tablet or laptop. The reader remembers your page, so you always pick up where you left off."),
)

/**
 * The front door. One photograph, edge to edge, with the two things a
 * visitor came for in the header: how to reach us, and where to sign in.
 * The how-it-works steps and the footer live in a side menu so the root
 * page itself never scrolls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(onSignIn: () -> Unit, onCreateAccount: () -> Unit) {
    val faint = Color.White.copy(alpha = 0.10f)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Ink, drawerContentColor = Paper) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LogoRow(dark = true)
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close menu", tint = Paper)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    ContactMenu(dark = true)

                    Spacer(Modifier.height(32.dp))
                    Eyebrow("How it works", color = Paper.copy(alpha = 0.6f))
                    Spacer(Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        steps.forEach { step ->
                            Column {
                                Text(step.n, style = MaterialTheme.typography.displaySmall, color = Ember)
                                Spacer(Modifier.height(8.dp))
                                Text(step.title, style = MaterialTheme.typography.headlineMedium, color = Paper)
                                Spacer(Modifier.height(6.dp))
                                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = Paper.copy(alpha = 0.7f))
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    HorizontalDivider(color = faint)
                    Spacer(Modifier.height(24.dp))

                    Text("© ${Year.now().value} ${Config.APP_NAME}", style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.6f))
                    if (Config.CONTACT_EMAIL.isNotBlank()) {
                        Text(
                            Config.CONTACT_EMAIL,
                            style = MaterialTheme.typography.bodySmall,
                            color = Paper.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Text("Support: ", style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.5f))
                        Text(Config.SUPPORT_EMAIL, style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("Powered by ", style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.5f))
                        Text(
                            Config.POWERED_BY_NAME,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Paper.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().background(Ink)) {
            Box(Modifier.weight(1f)) {
                Image(
                    painterResource(R.drawable.banner),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0f to Ink.copy(alpha = 0.2f), 0.5f to Ink.copy(alpha = 0.5f), 1f to Ink)),
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu", tint = Paper)
                        }
                        LogoRow(dark = true)
                    }
                    Pill("Login", onClick = onSignIn, style = PillStyle.Ember, compact = true)
                }

                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Eyebrow("A private reading room", color = Paper.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    Text("Your books, wherever you are.", style = MaterialTheme.typography.displayLarge, color = Paper)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "${Config.APP_NAME} is a members’ library. Create your own account, and start reading on every device you own.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(28.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Pill("Sign in", onClick = onSignIn, style = PillStyle.Ember)
                        Pill("Create account", onClick = onCreateAccount, style = PillStyle.Dark)
                    }
                }
            }

            PublicBrowseSection(onBookTap = onSignIn, modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * What's on the shelf, for a visitor who has not signed in yet — the same
 * catalogue as the signed-in Browse tab, with each book's price and
 * discount, but every cover taps through to sign in rather than opening.
 */
@Composable
private fun PublicBrowseSection(onBookTap: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "landing-browse") { ShelfViewModel(app, browse = true) }
    val faint = Color.White.copy(alpha = 0.10f)

    Column(modifier.fillMaxWidth().background(Ink)) {
        HorizontalDivider(color = faint)
        Column(Modifier.padding(top = 20.dp, bottom = 8.dp)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Eyebrow("Available now", color = Paper.copy(alpha = 0.6f))
                Spacer(Modifier.height(6.dp))
                Text("Browse the library", style = MaterialTheme.typography.titleLarge, color = Paper)
            }
            Spacer(Modifier.height(16.dp))

            val books = vm.books
            when {
                vm.signedOut || vm.error != null -> Box(Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "Sign in to see what's on the shelf.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Paper.copy(alpha = 0.6f),
                    )
                }
                books == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Ember)
                }
                books.isEmpty() -> Box(Modifier.padding(horizontal = 20.dp)) {
                    Text("No books in the library yet.", style = MaterialTheme.typography.bodyMedium, color = Paper.copy(alpha = 0.6f))
                }
                else -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(books, key = { it.id }) { book -> PublicBookCard(book, onClick = onBookTap) }
                }
            }
        }
    }
}

@Composable
private fun PublicBookCard(book: Book, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            Modifier
                .width(120.dp)
                .aspectRatio(3f / 4f)
                .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(10.dp)),
        ) {
            CoverImage(book.id, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.labelLarge,
            color = Paper,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        book.price?.let { price ->
            Spacer(Modifier.height(2.dp))
            if (book.hasDiscount) {
                Text(
                    "${book.discountedPrice?.let { formatPrice(it) }} · ${book.discountPercent}% off",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(formatPrice(price), style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.6f), maxLines = 1)
            }
        }
    }
}
