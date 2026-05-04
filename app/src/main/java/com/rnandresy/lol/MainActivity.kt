package com.rnandresy.lol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rnandresy.lol.ui.auth.LoginScreen
import com.rnandresy.lol.ui.auth.RegisterScreen
import com.rnandresy.lol.ui.chat.ChatListScreen
import com.rnandresy.lol.ui.chat.ChatScreen
import com.rnandresy.lol.ui.confession.ConfessionsScreen
import com.rnandresy.lol.ui.feed.FeedScreen
import com.rnandresy.lol.ui.members.MembersScreen
import com.rnandresy.lol.ui.notifications.NotificationsScreen
import com.rnandresy.lol.ui.post.CommentsScreen
import com.rnandresy.lol.ui.post.CreatePostScreen
import com.rnandresy.lol.ui.post.CreateStoryScreen
import com.rnandresy.lol.ui.profile.AchievementsScreen
import com.rnandresy.lol.ui.profile.EditProfileScreen
import com.rnandresy.lol.ui.profile.ProfileScreen
import com.rnandresy.lol.ui.settings.SettingsScreen
import com.rnandresy.lol.ui.theme.AskipTheme
import com.rnandresy.lol.viewmodel.AskipViewModel

class MainActivity : ComponentActivity() {

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.rnandresy.lol.utils.CloudinaryUploader.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AskipTheme { AskipApp() } }
    }
}

@Composable
fun AskipApp() {
    val nav = rememberNavController()
    val vm: AskipViewModel = viewModel()
    val isLoggedIn  by vm.isLoggedIn.collectAsState()
    val backStack   by nav.currentBackStackEntryAsState()
    val route        = backStack?.destination?.route
    val startDest    = remember { if (vm.isLoggedIn.value) "feed" else "login" }
    val unreadNotifs by vm.unreadNotifCount.collectAsState()

    val bottomRoutes = setOf("feed", "confessions", "notifications", "chatlist", "myprofile")
    val showNav      = isLoggedIn && route in bottomRoutes

    val goProfile: (String) -> Unit = { uid ->
        val dest = if (uid == vm.currentUserId) "myprofile" else "profile/$uid"
        nav.navigate(dest) { launchSingleTop = true }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showNav) {
                NavigationBar {
                    // Accueil
                    NavigationBarItem(
                        selected = route == "feed",
                        onClick  = { nav.navigate("feed") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.Home, null) },
                        label    = { Text("Askip") }
                    )
                    // Confessions
                    NavigationBarItem(
                        selected = route == "confessions",
                        onClick  = { nav.navigate("confessions") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.TheaterComedy, null) },
                        label    = { Text("Confessions") }
                    )
                    // Notifications (avec badge)
                    NavigationBarItem(
                        selected = route == "notifications",
                        onClick  = { nav.navigate("notifications") { launchSingleTop = true; restoreState = true } },
                        icon     = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge {
                                            Text(if (unreadNotifs > 99) "99+" else "$unreadNotifs")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, null)
                            }
                        },
                        label = { Text("Notifs") }
                    )
                    // Messages
                    NavigationBarItem(
                        selected = route == "chatlist",
                        onClick  = { nav.navigate("chatlist") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.Chat, null) },
                        label    = { Text("Messages") }
                    )
                    // Profil
                    NavigationBarItem(
                        selected = route == "myprofile",
                        onClick  = { nav.navigate("myprofile") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.Person, null) },
                        label    = { Text("Profil") }
                    )
                }
            }
        }
    ) { innerPad ->
        NavHost(
            navController    = nav,
            startDestination = startDest,
            modifier         = Modifier.padding(innerPad)
        ) {
            // ── Auth ──────────────────────────────────────────────────────────
            composable("login") {
                LoginScreen(vm,
                    onSuccess    = { nav.navigate("feed") { popUpTo("login") { inclusive = true } } },
                    onGoRegister = { nav.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(vm,
                    onSuccess = { nav.navigate("feed") { popUpTo("register") { inclusive = true } } },
                    onGoLogin = { nav.popBackStack() }
                )
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            composable("feed") {
                FeedScreen(vm,
                    onOpenComments  = { nav.navigate("comments/$it") },
                    onOpenProfile   = goProfile,
                    onNewPost       = { nav.navigate("newpost") },
                    onNewStory      = { nav.navigate("newstory") },
                    onOpenMembers   = { nav.navigate("members") },
                    onLogout        = {
                        vm.logout()
                        nav.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("confessions") {
                ConfessionsScreen(vm, onOpenComments = { nav.navigate("comments/$it") })
            }
            composable("notifications") {
                NotificationsScreen(
                    vm                 = vm,
                    onOpenPost         = { postId -> nav.navigate("comments/$postId") },
                    onOpenConversation = { convId -> nav.navigate("chat/$convId") },
                    onOpenProfile      = goProfile
                )
            }
            composable("chatlist") {
                ChatListScreen(vm,
                    onOpenChat    = { nav.navigate("chat/$it") },
                    onOpenProfile = goProfile
                )
            }
            composable("myprofile") {
                ProfileScreen(vm,
                    userId         = vm.currentUserId,
                    onBack         = { nav.popBackStack() },
                    onEditProfile  = { nav.navigate("editprofile") },
                    onOpenChat     = { nav.navigate("chat/$it") },
                    onAchievements = { nav.navigate("achievements/${vm.currentUserId}") },
                    onSettings     = { nav.navigate("settings") }
                )
            }

            // ── Sub-screens ───────────────────────────────────────────────────
            composable("newpost") {
                CreatePostScreen(vm, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
            composable("newstory") {
                CreateStoryScreen(vm, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
            composable("editprofile") {
                EditProfileScreen(vm, onSaved = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(vm,
                    onLogout = {
                        vm.logout()
                        nav.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("members") {
                MembersScreen(vm, onOpenProfile = goProfile, onBack = { nav.popBackStack() })
            }
            composable(
                route     = "comments/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) {
                CommentsScreen(vm,
                    postId        = it.arguments?.getString("postId") ?: "",
                    onOpenProfile = goProfile,
                    onBack        = { nav.popBackStack() }
                )
            }
            composable(
                route     = "profile/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) {
                val uid = it.arguments?.getString("uid") ?: ""
                ProfileScreen(vm,
                    userId         = uid,
                    onBack         = { nav.popBackStack() },
                    onEditProfile  = { nav.navigate("editprofile") },
                    onOpenChat     = { convId -> nav.navigate("chat/$convId") },
                    onAchievements = { nav.navigate("achievements/$uid") },
                    onSettings     = {}
                )
            }
            composable(
                route     = "achievements/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) {
                AchievementsScreen(vm,
                    userId = it.arguments?.getString("uid") ?: "",
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route     = "chat/{convId}",
                arguments = listOf(navArgument("convId") { type = NavType.StringType })
            ) {
                val convId      = it.arguments?.getString("convId") ?: ""
                val myId        = vm.currentUserId
                val conv        = vm.conversations.collectAsState().value.find { c -> c.id == convId }
                val otherId     = conv?.participants?.firstOrNull { p -> p != myId } ?: ""
                val otherName   = conv?.participantNames?.get(otherId) ?: "Utilisateur"
                val profilesMap by vm.profilesMap.collectAsState()
                val otherPhoto  = profilesMap[otherId]?.photoUrl ?: ""
                ChatScreen(vm,
                    convId        = convId,
                    otherUserId   = otherId,
                    otherUsername = otherName,
                    otherPhotoUrl = otherPhoto,
                    onOpenProfile = goProfile,
                    onBack        = { nav.popBackStack() }
                )
            }
        }
    }
}