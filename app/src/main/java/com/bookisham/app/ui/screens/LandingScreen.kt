package com.bookisham.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.R
import com.bookisham.app.ui.components.ContactMenu
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.LogoRow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.openSafely
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.Paper
import java.time.Year

private data class Step(val n: String, val title: String, val body: String)

private val steps = listOf(
    Step("01", "Get in touch", "Message us on WhatsApp or by email with the books you would like to read."),
    Step("02", "We open your account", "You receive a sign-in email and password from us, and the books chosen for you appear in your library."),
    Step("03", "Read anywhere", "Sign in on your phone, tablet or laptop. The reader remembers your page, so you always pick up where you left off."),
)

/**
 * The front door. One photograph, edge to edge, with the two things a
 * visitor came for in the header: how to reach us, and where to sign in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(onSignIn: () -> Unit) {
    val uri = LocalUriHandler.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val faint = Color.White.copy(alpha = 0.10f)

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(screenHeight)) {
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
                LogoRow(dark = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ContactMenu(dark = true)
                    Pill("Login", onClick = onSignIn, style = PillStyle.Ember, compact = true)
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 56.dp),
            ) {
                Eyebrow("A private reading room", color = Paper.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))
                Text("Your books, wherever you are.", style = MaterialTheme.typography.displayLarge, color = Paper)
                Spacer(Modifier.height(20.dp))
                Text(
                    "${Config.APP_NAME} is a members’ library. Ask us for an account, and the books we choose for you are waiting on every device you own.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(28.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pill("Sign in", onClick = onSignIn, style = PillStyle.Ember)
                    if (Config.WHATSAPP.isNotBlank()) {
                        Pill("Ask for an account", onClick = { uri.openSafely("https://wa.me/${Config.WHATSAPP}") }, style = PillStyle.Dark)
                    }
                }
            }
        }

        HorizontalDivider(color = faint)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 56.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
            steps.forEach { step ->
                Column {
                    Text(step.n, style = MaterialTheme.typography.displaySmall, color = Ember)
                    Spacer(Modifier.height(12.dp))
                    Text(step.title, style = MaterialTheme.typography.headlineMedium, color = Paper)
                    Spacer(Modifier.height(8.dp))
                    Text(step.body, style = MaterialTheme.typography.bodyMedium, color = Paper.copy(alpha = 0.7f))
                }
            }
        }

        HorizontalDivider(color = faint)
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("© ${Year.now().value} ${Config.APP_NAME}", style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.6f))
            if (Config.CONTACT_EMAIL.isNotBlank()) {
                Text(
                    Config.CONTACT_EMAIL,
                    style = MaterialTheme.typography.bodySmall,
                    color = Paper.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row {
                Text("Support: ", style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.5f))
                Text(
                    Config.SUPPORT_EMAIL,
                    style = MaterialTheme.typography.bodySmall,
                    color = Paper.copy(alpha = 0.5f),
                    modifier = Modifier.width(0.dp).weight(1f),
                )
            }
            Row {
                Text("Powered by ", style = MaterialTheme.typography.bodySmall, color = Paper.copy(alpha = 0.5f))
                Text(
                    Config.POWERED_BY_NAME,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Paper.copy(alpha = 0.6f),
                    modifier = Modifier.padding(0.dp),
                )
            }
        }
    }
}
