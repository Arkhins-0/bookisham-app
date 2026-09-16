package com.bookisham.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bookisham.app.Config
import com.bookisham.app.LocalApp
import com.bookisham.app.data.ApiException
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.FieldLabel
import com.bookisham.app.ui.components.LogoRow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.TextBox
import com.bookisham.app.ui.components.openSafely
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft
import com.bookisham.app.ui.theme.Paper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onBack: () -> Unit, onSignedIn: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val uri = LocalUriHandler.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        if (busy) return
        error = ""
        if (email.isBlank() || password.isBlank()) {
            error = "Enter your email and password."
            return
        }
        busy = true
        focus.clearFocus()
        scope.launch {
            try {
                app.api.login(email, password)
                onSignedIn()
            } catch (e: ApiException) {
                error = e.message ?: "Could not sign in."
                busy = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        LogoRow()
        Spacer(Modifier.height(40.dp))

        Row(
            Modifier.clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = InkFaint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Back to home", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
        }
        Spacer(Modifier.height(24.dp))

        Text("Welcome back", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sign in with the details you were sent. Signing in here signs out any other device.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )

        Spacer(Modifier.height(32.dp))
        FieldLabel("Email or username")
        TextBox(email, { email = it }, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        Spacer(Modifier.height(16.dp))
        FieldLabel("Password")
        TextBox(password, { password = it }, password = true, imeAction = ImeAction.Done, onDone = { submit() })

        if (error.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            ErrorNote(error)
        }

        Spacer(Modifier.height(24.dp))
        Pill(
            if (busy) "Signing in…" else "Sign in",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Primary,
            icon = Icons.AutoMirrored.Filled.Login,
            enabled = !busy,
        )

        Spacer(Modifier.height(32.dp))
        Row {
            Text("No account yet? ", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
            when {
                Config.WHATSAPP.isNotBlank() -> Text(
                    "Message us on WhatsApp",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ember,
                    modifier = Modifier.clickable { uri.openSafely("https://wa.me/${Config.WHATSAPP}") },
                )
                Config.CONTACT_EMAIL.isNotBlank() -> Text(
                    "Email us",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Ember,
                    modifier = Modifier.clickable { uri.openSafely("mailto:${Config.CONTACT_EMAIL}") },
                )
                else -> Text("Ask the admin for one.", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
            }
        }
    }
}
