package com.bookisham.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.R
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft
import com.bookisham.app.ui.theme.Night
import com.bookisham.app.ui.theme.Paper
import com.bookisham.app.ui.theme.WhatsAppGreen

/* -- Buttons: the .btn-* classes of globals.css ---------------------------- */

enum class PillStyle { Primary, Ember, Ghost, Dark }

private data class PillColors(val container: Color, val content: Color, val border: Color?)

private fun colorsOf(style: PillStyle): PillColors = when (style) {
    PillStyle.Primary -> PillColors(Ink, Paper, null)
    PillStyle.Ember -> PillColors(Ember, Color.White, null)
    PillStyle.Ghost -> PillColors(Color.White.copy(alpha = 0.6f), Ink, Ink.copy(alpha = 0.15f))
    PillStyle.Dark -> PillColors(Color.White.copy(alpha = 0.06f), Color.White, Color.White.copy(alpha = 0.15f))
}

@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val c = colorsOf(style)
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        enabled = enabled,
        shape = CircleShape,
        color = c.container,
        contentColor = c.content,
        border = c.border?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 20.dp, vertical = if (compact) 7.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun IconPill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Dark,
    enabled: Boolean = true,
) {
    val c = colorsOf(style)
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (enabled) 1f else 0.3f),
        enabled = enabled,
        shape = CircleShape,
        color = c.container,
        contentColor = c.content,
        border = c.border?.let { BorderStroke(1.dp, it) },
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
        }
    }
}

/* -- Text ------------------------------------------------------------------- */

@Composable
fun Eyebrow(text: String, color: Color = InkFaint) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = InkSoft,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun ErrorNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = EmberDark,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(Ember.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/* -- Fields ----------------------------------------------------------------- */

@Composable
fun TextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Ember,
            unfocusedBorderColor = Ink.copy(alpha = 0.15f),
            cursorColor = Ember,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
        ),
    )
}

/* -- Surfaces --------------------------------------------------------------- */

/** The .card class: white, rounded, a hairline and a soft shadow. */
@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Ink.copy(alpha = 0.25f))
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Ink.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun EmptyCard(title: String, body: String) {
    PaperCard(Modifier.padding(top = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LogoRow(dark: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(Config.APP_NAME, style = MaterialTheme.typography.titleLarge, color = if (dark) Paper else Ink)
    }
}

/** Fills the screen with the mark, fading in and out, while something loads. */
@Composable
fun LoadingScreen(night: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        Modifier.fillMaxSize().background(if (night) Night else Paper),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier.size(72.dp).alpha(alpha).clip(RoundedCornerShape(18.dp)),
        )
    }
}

/* -- Contact ---------------------------------------------------------------- */

fun UriHandler.openSafely(uri: String) {
    runCatching { openUri(uri) }
}

/** "Contact": the two ways to reach the admin. WhatsApp opens a chat; email opens the mail app. */
@Composable
fun ContactMenu(dark: Boolean = false, whatsapp: String = Config.WHATSAPP, email: String = Config.CONTACT_EMAIL) {
    if (whatsapp.isBlank() && email.isBlank()) return
    val uri = LocalUriHandler.current
    var open by remember { mutableStateOf(false) }

    Box {
        Pill(
            "Contact",
            onClick = { open = true },
            style = if (dark) PillStyle.Dark else PillStyle.Ghost,
            icon = Icons.Filled.ExpandMore,
            compact = true,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (whatsapp.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("WhatsApp", fontWeight = FontWeight.SemiBold)
                            Text("+$whatsapp", style = MaterialTheme.typography.bodySmall, color = InkFaint)
                        }
                    },
                    leadingIcon = {
                        Box(Modifier.size(32.dp).background(WhatsAppGreen, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        open = false
                        uri.openSafely("https://wa.me/$whatsapp?text=" + Uri.encode("Hello, I would like a Bookisham account."))
                    },
                )
            }
            if (email.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Email", fontWeight = FontWeight.SemiBold)
                            Text(email, style = MaterialTheme.typography.bodySmall, color = InkFaint)
                        }
                    },
                    leadingIcon = {
                        Box(Modifier.size(32.dp).background(Ink, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = Paper, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        open = false
                        uri.openSafely("mailto:$email?subject=" + Uri.encode("Bookisham account"))
                    },
                )
            }
        }
    }
}
