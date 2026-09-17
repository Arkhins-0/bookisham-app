package com.bookisham.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookisham.app.LocalApp
import com.bookisham.app.data.AdminUser
import com.bookisham.app.ui.AdminUsersViewModel
import com.bookisham.app.ui.components.AdminSection
import com.bookisham.app.ui.components.AdminSectionTabs
import com.bookisham.app.ui.components.CredentialsCard
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.EmptyCard
import com.bookisham.app.ui.components.Eyebrow
import com.bookisham.app.ui.components.FieldLabel
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.TextBox
import com.bookisham.app.ui.rememberViewModel
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.EmberDark
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.PaperDeep

/**
 * The reader list. Everyone with an account; open one to choose their
 * books. New readers are created from the sheet, which shows the generated
 * password exactly once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReadersScreen(
    onOpenUser: (String) -> Unit,
    onOpenBooks: () -> Unit,
    onOpenPurchases: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val app = LocalApp.current
    val vm = rememberViewModel(key = "admin-users") { AdminUsersViewModel(app) }
    if (vm.signedOut) LaunchedEffect(Unit) { onSignedOut() }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    PullToRefreshBox(
        isRefreshing = vm.refreshing,
        onRefresh = { vm.load(byUser = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    AdminSectionTabs(AdminSection.Readers) {
                        when (it) {
                            AdminSection.Books -> onOpenBooks()
                            AdminSection.Purchases -> onOpenPurchases()
                            AdminSection.Readers -> {}
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Readers")
                            Spacer(Modifier.height(8.dp))
                            Text("Readers", style = MaterialTheme.typography.displaySmall)
                        }
                        Pill("New reader", onClick = { sheetOpen = true }, icon = Icons.Outlined.PersonAdd, compact = true)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Everyone with an account. Open a reader to choose their books.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint,
                    )
                    Spacer(Modifier.height(16.dp))
                    FieldLabel("Search by name, email or phone")
                    TextBox(query, { query = it }, imeAction = ImeAction.Search)
                }
            }

            val users = vm.users?.filter { it.matches(query) }
            val error = vm.error
            when {
                users == null && error != null -> item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ErrorNote(error)
                        Spacer(Modifier.height(12.dp))
                        Pill("Try again", onClick = { vm.load() }, style = PillStyle.Ghost, compact = true)
                    }
                }
                users == null -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Ember)
                    }
                }
                users.isEmpty() -> item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        EmptyCard(
                            if (query.isBlank()) "No readers yet" else "No readers match that search",
                            if (query.isBlank()) "Create the first one above." else "",
                        )
                    }
                }
                else -> items(users, key = { it.id }) { user ->
                    ReaderRow(user, onClick = { onOpenUser(user.id) })
                    HorizontalDivider(color = Ink.copy(alpha = 0.06f))
                }
            }
        }
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                sheetOpen = false
                vm.dismissCreated()
            },
            sheetState = sheetState,
        ) {
            NewReaderSheetContent(vm, onClose = { sheetOpen = false })
        }
    }
}

@Composable
private fun ReaderRow(user: AdminUser, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(PaperDeep, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = InkFaint,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val tags = buildList {
                add(user.email)
                if (user.role == "admin") add("admin")
                if (user.disabled) add("disabled")
            }.joinToString(" · ")
            Text(tags, style = MaterialTheme.typography.bodySmall, color = InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("${user.books}", style = MaterialTheme.typography.bodyMedium, color = InkFaint)
        androidx.compose.material3.Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = InkFaint,
        )
    }
}

@Composable
private fun NewReaderSheetContent(vm: AdminUsersViewModel, onClose: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("New reader", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Use the email the reader contacted you from. The password is shown once — send it to them.",
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
        )
        Spacer(Modifier.height(20.dp))

        val created = vm.created
        if (created != null) {
            CredentialsCard(
                email = created.first,
                password = created.second,
                onDone = {
                    vm.dismissCreated()
                    name = ""; email = ""; password = ""
                    onClose()
                },
            )
        } else {
            FieldLabel("Name")
            TextBox(name, { name = it }, imeAction = ImeAction.Next)
            Spacer(Modifier.height(14.dp))
            FieldLabel("Email")
            TextBox(email, { email = it }, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            Spacer(Modifier.height(14.dp))
            FieldLabel("Password (blank = generate one)")
            TextBox(password, { password = it }, imeAction = ImeAction.Done, onDone = { vm.createUser(email, name, password) })

            vm.createError?.let {
                Spacer(Modifier.height(12.dp))
                ErrorNote(it)
            }

            Spacer(Modifier.height(20.dp))
            Pill(
                if (vm.creating) "Creating…" else "Create account",
                onClick = { vm.createUser(email, name, password) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.PersonAdd,
                enabled = !vm.creating && email.isNotBlank(),
            )
        }
    }
}
