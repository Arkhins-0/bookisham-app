package com.bookisham.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.navArgument
import com.bookisham.app.LocalApp
import com.bookisham.app.data.Me
import com.bookisham.app.ui.components.ErrorNote
import com.bookisham.app.ui.components.LoadingScreen
import com.bookisham.app.ui.components.LogoRow
import com.bookisham.app.ui.components.Pill
import com.bookisham.app.ui.components.PillStyle
import com.bookisham.app.ui.components.UpdateAvailableDialog
import com.bookisham.app.ui.components.openSafely
import com.bookisham.app.ui.reader.ReaderScreen
import com.bookisham.app.ui.screens.AccountScreen
import com.bookisham.app.ui.screens.AdminBooksScreen
import com.bookisham.app.ui.screens.AdminPurchasesScreen
import com.bookisham.app.ui.screens.AdminReadersScreen
import com.bookisham.app.ui.screens.AdminUserDetailScreen
import com.bookisham.app.ui.screens.BrowseScreen
import com.bookisham.app.ui.screens.LandingScreen
import com.bookisham.app.ui.screens.LegalScreen
import com.bookisham.app.ui.screens.LibraryScreen
import com.bookisham.app.ui.screens.LoginScreen
import com.bookisham.app.ui.screens.NotificationsScreen
import com.bookisham.app.ui.screens.SignupScreen
import com.bookisham.app.ui.theme.Ember
import com.bookisham.app.ui.theme.Ink
import com.bookisham.app.ui.theme.InkFaint
import com.bookisham.app.ui.theme.Paper

/** The whole app: which tree shows depends only on whether someone is signed in. */
@Composable
fun BookishamApp() {
    val app = LocalApp.current
    val vm = rememberViewModel { AppViewModel(app) }
    val uri = LocalUriHandler.current

    when (val state = vm.session) {
        SessionState.Loading -> LoadingScreen()
        is SessionState.Failed -> FailedScreen(state.message, onRetry = vm::refresh, onSignOut = vm::logout)
        SessionState.SignedOut -> SignedOutFlow(onSignedIn = vm::signedIn)
        is SessionState.SignedIn -> SignedInFlow(state.me, vm)
    }

    // Back from the "allow installs" settings screen: carry on where we left off.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && vm.updateStage is UpdateStage.NeedsPermission) vm.install()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val update = vm.updateInfo
    if (update != null && !vm.updateDismissed) {
        UpdateAvailableDialog(
            info = update,
            stage = vm.updateStage,
            onUpdate = vm::downloadAndInstall,
            onInstall = vm::install,
            onOpenSettings = vm::openInstallSettings,
            onOpenReleasePage = {
                uri.openSafely(update.releaseUrl)
                vm.dismissUpdate()
            },
            onDismiss = vm::dismissUpdate,
        )
    }
}

@Composable
private fun FailedScreen(message: String, onRetry: () -> Unit, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Paper).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoRow()
        Spacer(Modifier.height(24.dp))
        Text("Could not reach the library", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        ErrorNote(message)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Try again", onClick = onRetry)
            Pill("Sign out", onClick = onSignOut, style = PillStyle.Ghost)
        }
    }
}

@Composable
private fun SignedOutFlow(onSignedIn: () -> Unit) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "landing",
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) {
        composable("landing") {
            LandingScreen(
                onSignIn = { nav.navigate("login") },
                onCreateAccount = { nav.navigate("signup") },
            )
        }
        composable("login") {
            LoginScreen(
                onBack = { nav.popBackStack() },
                onCreateAccount = { nav.navigate("signup") },
                onSignedIn = onSignedIn,
            )
        }
        composable("signup") {
            SignupScreen(
                onBack = { nav.popBackStack() },
                onSignIn = { nav.navigate("login") { popUpTo("signup") { inclusive = true } } },
                onSignedIn = onSignedIn,
                onOpenTerms = { nav.navigate("legal/terms") },
                onOpenPrivacy = { nav.navigate("legal/privacy") },
            )
        }
        composable(
            "legal/{kind}",
            arguments = listOf(navArgument("kind") { type = NavType.StringType }),
        ) { entry ->
            LegalScreen(
                kind = entry.arguments?.getString("kind").orEmpty(),
                standalone = true,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun SignedInFlow(me: Me, vm: AppViewModel) {
    val app = LocalApp.current
    val notificationsVm = rememberViewModel(key = "notifications") { NotificationsViewModel(app) }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val inReader = route?.startsWith("read/") == true
    val inAdminDrilldown = route?.startsWith("adminUser/") == true
    val hideChrome = inReader || inAdminDrilldown

    val tabs = buildList {
        add(Tab("library", "Library", Icons.AutoMirrored.Outlined.LibraryBooks))
        if (me.user.isAdmin) add(Tab("admin", "Admin", Icons.Outlined.AdminPanelSettings))
        else add(Tab("browse", "Browse", Icons.Outlined.Explore))
        add(Tab("account", "Account", Icons.Outlined.Person))
    }
    // The admin section has three screens ("admin", "adminBooks", "adminPurchases") behind one tab.
    fun tabSelected(tab: Tab) = route == tab.route || (tab.route == "admin" && (route == "adminBooks" || route == "adminPurchases"))

    Scaffold(
        containerColor = Paper,
        topBar = {
            if (!hideChrome) {
                AppTopBar(
                    unreadNotifications = notificationsVm.unreadCount,
                    onOpenNotifications = {
                        notificationsVm.load()
                        nav.navigate("notifications") { launchSingleTop = true }
                    },
                    onLogout = vm::logout,
                )
            }
        },
        bottomBar = {
            if (!hideChrome) {
                NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tabSelected(tab),
                            onClick = {
                                // The notifications feed sits above whichever tab opened it. Drop it
                                // before switching, or the tab's saved state would bring it back.
                                if (route == "notifications") nav.popBackStack()
                                if (!tabSelected(tab)) {
                                    nav.navigate(tab.route) {
                                        popUpTo("library") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Ember,
                                selectedTextColor = Ink,
                                indicatorColor = Ember.copy(alpha = 0.12f),
                                unselectedIconColor = InkFaint,
                                unselectedTextColor = InkFaint,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "library",
            modifier = Modifier.padding(if (hideChrome) PaddingValues(0.dp) else padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable("library") {
                LibraryScreen(me, onOpen = { nav.navigate("read/$it") }, onSignedOut = vm::signedOut)
            }
            composable("browse") {
                BrowseScreen(me, onOpen = { nav.navigate("read/$it") }, onSignedOut = vm::signedOut)
            }
            composable("account") {
                AccountScreen(
                    me,
                    updateInfo = vm.updateInfo,
                    onNameSaved = vm::nameChanged,
                    onLogout = vm::logout,
                    onSignedOut = vm::signedOut,
                    onUpdate = vm::showUpdate,
                    onOpenTerms = { nav.navigate("legal/terms") },
                    onOpenPrivacy = { nav.navigate("legal/privacy") },
                )
            }
            composable(
                "legal/{kind}",
                arguments = listOf(navArgument("kind") { type = NavType.StringType }),
            ) { entry ->
                LegalScreen(
                    kind = entry.arguments?.getString("kind").orEmpty(),
                    standalone = false,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("admin") {
                AdminReadersScreen(
                    onOpenUser = { nav.navigate("adminUser/$it") },
                    onOpenBooks = {
                        nav.navigate("adminBooks") { popUpTo("admin") { inclusive = true } }
                    },
                    onOpenPurchases = {
                        nav.navigate("adminPurchases") { popUpTo("admin") { inclusive = true } }
                    },
                    onSignedOut = vm::signedOut,
                )
            }
            composable("adminBooks") {
                AdminBooksScreen(
                    onPreview = { nav.navigate("read/$it") },
                    onOpenReaders = {
                        nav.navigate("admin") { popUpTo("adminBooks") { inclusive = true } }
                    },
                    onOpenPurchases = {
                        nav.navigate("adminPurchases") { popUpTo("adminBooks") { inclusive = true } }
                    },
                    onSignedOut = vm::signedOut,
                )
            }
            composable("adminPurchases") {
                AdminPurchasesScreen(
                    onOpenReaders = {
                        nav.navigate("admin") { popUpTo("adminPurchases") { inclusive = true } }
                    },
                    onOpenBooks = {
                        nav.navigate("adminBooks") { popUpTo("adminPurchases") { inclusive = true } }
                    },
                    onSignedOut = vm::signedOut,
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    vm = notificationsVm,
                    onBack = { nav.popBackStack() },
                    onSignedOut = vm::signedOut,
                )
            }
            composable(
                "adminUser/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                AdminUserDetailScreen(
                    userId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onSignedOut = vm::signedOut,
                )
            }
            composable(
                "read/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                ReaderScreen(
                    bookId = entry.arguments?.getString("bookId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onSignedOut = vm::signedOut,
                )
            }
        }
    }
}

/** The bar across the top of every signed-in page but the reader. */
@Composable
private fun AppTopBar(unreadNotifications: Int, onOpenNotifications: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Paper.copy(alpha = 0.92f))) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LogoRow()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Ink)
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(8.dp)
                                .background(Ember, CircleShape),
                        )
                    }
                }
                Pill("Sign out", onClick = onLogout, style = PillStyle.Ghost, compact = true, icon = Icons.AutoMirrored.Filled.Logout)
            }
        }
        HorizontalDivider(color = Ink.copy(alpha = 0.10f))
    }
}
