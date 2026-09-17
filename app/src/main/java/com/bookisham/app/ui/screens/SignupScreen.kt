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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.ApiException
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.FieldLabel
import com.bookisham.app.ui.components.LogoRow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.TextBox
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft
import com.bookisham.app.ui.theme.Paper
import kotlinx.coroutines.launch

/** Self-service signup: name, phone, email and password. Succeeding here signs the reader straight in. */
@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignedIn: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        if (busy) return
        error = ""
        when {
            name.isBlank() || phone.isBlank() || email.isBlank() || password.isBlank() -> {
                error = "Fill in your name, phone, email and password."
                return
            }
            password.length < 8 -> {
                error = "Your password needs to be at least 8 characters."
                return
            }
            !agreed -> {
                error = "Please agree to the Terms & Conditions and Privacy Policy."
                return
            }
        }
        busy = true
        focus.clearFocus()
        scope.launch {
            try {
                app.api.signup(name, phone, email, password)
                onSignedIn()
            } catch (e: ApiException) {
                error = e.message ?: "Could not create your account."
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

        Text("Create your account", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "A few details and you're in — you can start browsing the library right away.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )

        Spacer(Modifier.height(32.dp))
        FieldLabel("Name")
        TextBox(name, { name = it }, imeAction = ImeAction.Next)
        Spacer(Modifier.height(16.dp))
        FieldLabel("Phone")
        TextBox(phone, { phone = it }, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
        Spacer(Modifier.height(16.dp))
        FieldLabel("Email")
        TextBox(email, { email = it }, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        Spacer(Modifier.height(16.dp))
        FieldLabel("Password")
        TextBox(password, { password = it }, password = true, imeAction = ImeAction.Done, onDone = { submit() })

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = Ember))
            Column(Modifier.padding(top = 12.dp)) {
                Text("I agree to the", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                Row {
                    Text(
                        "Terms & Conditions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ember,
                        modifier = Modifier.clickable(onClick = onOpenTerms),
                    )
                    Text(" and ", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ember,
                        modifier = Modifier.clickable(onClick = onOpenPrivacy),
                    )
                }
            }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            ErrorNote(error)
        }

        Spacer(Modifier.height(24.dp))
        Pill(
            if (busy) "Creating your account…" else "Create account",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Primary,
            icon = Icons.Filled.PersonAdd,
            enabled = !busy && agreed,
        )

        Spacer(Modifier.height(32.dp))
        Row {
            Text("Already have an account? ", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
            Text(
                "Sign in",
                style = MaterialTheme.typography.bodyMedium,
                color = Ember,
                modifier = Modifier.clickable(onClick = onSignIn),
            )
        }
    }
}
