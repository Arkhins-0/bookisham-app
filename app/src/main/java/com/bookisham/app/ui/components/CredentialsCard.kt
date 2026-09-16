package com.bookisham.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.InkFaint

/**
 * The one time a password is shown. The admin copies the whole message and
 * sends it to the reader on WhatsApp or by email; after "Done" it is gone.
 */
@Composable
fun CredentialsCard(email: String, password: String, onDone: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val message = "Your Bookisham account is ready.\n\n" +
        "Sign in at ${Config.BASE_URL}/login\n" +
        "Email: $email\n" +
        "Password: $password\n\n" +
        "Signing in on a new device signs out the previous one."

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Column(
        Modifier
            .background(Ember.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("SEND THESE TO THE READER", style = MaterialTheme.typography.labelMedium, color = EmberDark)
        Spacer(Modifier.height(10.dp))
        SelectionContainer {
            Text(message, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Pill(
                if (copied) "Copied" else "Copy message",
                onClick = {
                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("Bookisham account", message))
                    copied = true
                },
                style = PillStyle.Ember,
                compact = true,
            )
            Spacer(Modifier.width(8.dp))
            Pill("Done", onClick = onDone, style = PillStyle.Ghost, compact = true)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "The password cannot be shown again. Reset it from the reader's page if it is lost.",
            style = MaterialTheme.typography.labelSmall,
            color = InkFaint,
        )
    }
}
