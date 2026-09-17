package com.bookisham.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bookisham.app.BuildConfig
import com.bookisham.app.Config
import com.bookisham.app.LocalApp
import com.bookisham.app.data.ApiException
import com.bookisham.app.data.AppVersionInfo
import com.bookisham.app.data.Me
import com.bookisham.app.data.UnauthorizedException
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.FieldLabel
import com.bookisham.app.ui.components.PaperCard
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.TextBox
import com.bookisham.app.ui.components.openSafely
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.InkSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A signed-in reader changing their own name and password. The email and id cannot be touched. */
@Composable
fun AccountScreen(
    me: Me,
    updateInfo: AppVersionInfo?,
    onNameSaved: (String) -> Unit,
    onLogout: () -> Unit,
    onSignedOut: () -> Unit,
    onUpdate: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val uri = LocalUriHandler.current

    var name by rememberSaveable { mutableStateOf(me.user.name) }
    var nameBusy by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf("") }
    var nameSaved by remember { mutableStateOf(false) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordBusy by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var passwordSaved by remember { mutableStateOf(false) }

    fun saveName() {
        if (nameBusy) return
        nameBusy = true
        nameError = ""
        nameSaved = false
        focus.clearFocus()
        scope.launch {
            try {
                val trimmed = name.trim().take(80)
                app.api.updateName(trimmed)
                name = trimmed
                nameSaved = true
                onNameSaved(trimmed)
            } catch (e: UnauthorizedException) {
                onSignedOut()
            } catch (e: ApiException) {
                nameError = e.message ?: "Could not save."
            }
            nameBusy = false
        }
    }

    fun savePassword() {
        if (passwordBusy) return
        passwordError = ""
        passwordSaved = false
        if (newPassword.trim().length < 8) {
            passwordError = "The new password must be at least 8 characters."
            return
        }
        if (newPassword != confirmPassword) {
            passwordError = "The new passwords do not match."
            return
        }
        passwordBusy = true
        focus.clearFocus()
        scope.launch {
            try {
                app.api.changePassword(currentPassword, newPassword)
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                passwordSaved = true
            } catch (e: UnauthorizedException) {
                onSignedOut()
            } catch (e: ApiException) {
                passwordError = e.message ?: "Could not change your password."
            }
            passwordBusy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Eyebrow("Your account")
        Spacer(Modifier.height(8.dp))
        Text("Account settings", style = MaterialTheme.typography.displayMedium)

        Spacer(Modifier.height(24.dp))
        PaperCard {
            FieldLabel("Email")
            Text(me.user.email, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            Spacer(Modifier.height(12.dp))
            CopyRow("User ID", me.user.id, mono = true)
            Spacer(Modifier.height(12.dp))
            CopyRow("UPI ID for payments", Config.UPI_ID)
        }

        Spacer(Modifier.height(16.dp))
        PaperCard {
            Text("Name", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            FieldLabel("Name")
            TextBox(name, { name = it.take(80) }, imeAction = ImeAction.Done, onDone = { saveName() })
            if (nameError.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ErrorNote(nameError)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(if (nameBusy) "Saving…" else "Save name", onClick = { saveName() }, icon = Icons.Filled.Check, enabled = !nameBusy)
                if (nameSaved) {
                    Spacer(Modifier.width(12.dp))
                    Text("Saved.", style = MaterialTheme.typography.bodySmall, color = InkFaint)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PaperCard {
            Text("Password", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            FieldLabel("Current password")
            TextBox(currentPassword, { currentPassword = it }, password = true)
            Spacer(Modifier.height(12.dp))
            FieldLabel("New password")
            TextBox(newPassword, { newPassword = it }, password = true)
            Spacer(Modifier.height(12.dp))
            FieldLabel("Confirm new password")
            TextBox(confirmPassword, { confirmPassword = it }, password = true, imeAction = ImeAction.Done, onDone = { savePassword() })
            if (passwordError.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ErrorNote(passwordError)
            }
            if (passwordSaved) {
                Spacer(Modifier.height(12.dp))
                Text("Password changed.", style = MaterialTheme.typography.bodySmall, color = InkFaint)
            }
            Spacer(Modifier.height(16.dp))
            Pill(if (passwordBusy) "Saving…" else "Change password", onClick = { savePassword() }, icon = Icons.Outlined.Key, enabled = !passwordBusy)
        }


        Spacer(Modifier.height(16.dp))
        PaperCard {
            Text("App version", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("You have v${BuildConfig.VERSION_NAME}.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            if (updateInfo != null) {
                Spacer(Modifier.height(4.dp))
                Text("v${updateInfo.version} is available.", style = MaterialTheme.typography.bodyMedium, color = Ember)
                Spacer(Modifier.height(16.dp))
                Pill("Update app", onClick = onUpdate, icon = Icons.Filled.SystemUpdate)
            } else {
                Spacer(Modifier.height(4.dp))
                Text("You're up to date.", style = MaterialTheme.typography.bodySmall, color = InkFaint)
            }
        }

        Spacer(Modifier.height(16.dp))
        PaperCard {
            Text("Legal", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row {
                Text(
                    "Terms & Conditions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ember,
                    modifier = Modifier.clickable(onClick = onOpenTerms),
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ember,
                    modifier = Modifier.clickable(onClick = onOpenPrivacy),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Pill("Sign out", onClick = onLogout, style = PillStyle.Ghost, icon = Icons.AutoMirrored.Filled.Logout)

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = Ink.copy(alpha = 0.10f))
        Spacer(Modifier.height(16.dp))
        Row {
            Text("Support: ", style = MaterialTheme.typography.bodySmall, color = InkFaint)
            Text(
                Config.SUPPORT_EMAIL,
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
                modifier = Modifier.clickable { uri.openSafely("mailto:${Config.SUPPORT_EMAIL}") },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("Powered by ${Config.POWERED_BY_NAME}", style = MaterialTheme.typography.bodySmall, color = InkFaint)
    }
}

/** A value with a copy button beside it; "Copied" shows for a moment after a tap. */
@Composable
private fun CopyRow(label: String, value: String, mono: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(value) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    FieldLabel(label)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            value,
            style = if (mono) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
            color = if (mono) InkFaint else InkSoft,
            modifier = Modifier.weight(1f),
        )
        if (copied) {
            Text("Copied", style = MaterialTheme.typography.bodySmall, color = Ember)
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = { clipboard.setText(AnnotatedString(value)); copied = true }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy $label", tint = InkFaint, modifier = Modifier.size(18.dp))
        }
    }
}
