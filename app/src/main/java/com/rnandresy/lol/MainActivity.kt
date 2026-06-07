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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rnandresy.lol.ui.auth.LoginScreen
import com.rnandresy.lol.ui.auth.RegisterScreen
import com.rnandresy.lol.ui.chat.ChatListScreen
import com.rnandresy.lol.ui.chat.ChatScreen
import com.rnandresy.lol.ui.chat.CreateGroupScreen
import com.rnandresy.lol.ui.chat.GroupChatScreen
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
import com.rnandresy.lol.ui.search.SearchScreen
import com.rnandresy.lol.ui.settings.SettingsScreen
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.ui.theme.AskipTheme
import com.rnandresy.lol.viewmodel.AskipViewModel

class MainActivity : ComponentActivity() {
    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            val vm: AskipViewModel = viewModel()
            val theme by vm.appTheme.collectAsState()
            AskipTheme(appTheme = theme) { AskipApp(vm) }
        }
    }
}

@Composable
fun AskipApp(vm: AskipViewModel) {
    val nav         = rememberNavController()
    val isLoggedIn  by vm.isLoggedIn.collectAsState()
    val backStack   by nav.currentBackStackEntryAsState()
    val route        = backStack?.destination?.route
    val startDest    = remember { if (vm.isLoggedIn.value) "feed" else "login" }
    val unreadNotifs by vm.unreadNotifCount.collectAsState()

    val bottomRoutes = setOf("feed", "search", "notifications", "chatlist", "myprofile")
    val showNav      = isLoggedIn && route in bottomRoutes

    val goProfile: (String) -> Unit = { uid ->
        val dest = if (uid == vm.currentUserId) "myprofile" else "profile/$uid"
        nav.navigate(dest) { launchSingleTop = true }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showNav) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "feed",
                        onClick  = { nav.navigate("feed") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.Home, null) },
                        label    = { Text("Accueil") }
                    )
                    NavigationBarItem(
                        selected = route == "search",
                        onClick  = { nav.navigate("search") { launchSingleTop = true } },
                        icon     = { Icon(Icons.Default.Search, null) },
                        label    = { Text("Chercher") }
                    )
                    NavigationBarItem(
                        selected = route == "notifications",
                        onClick  = { nav.navigate("notifications") { launchSingleTop = true; restoreState = true } },
                        icon     = {
                            BadgedBox(badge = {
                                if (unreadNotifs > 0) Badge { Text(if (unreadNotifs > 99) "99+" else "$unreadNotifs") }
                            }) { Icon(Icons.Default.Notifications, null) }
                        },
                        label = { Text("Notifs") }
                    )
                    NavigationBarItem(
                        selected = route == "chatlist",
                        onClick  = { nav.navigate("chatlist") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.Chat, null) },
                        label    = { Text("Messages") }
                    )
                    NavigationBarItem(
                        selected = route == "myprofile",
                        onClick  = { nav.navigate("myprofile") { launchSingleTop = true; restoreState = true } },
                        icon     = { Icon(Icons.Default.Person, null) },
                        label    = { Text("Profil") }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(navController = nav, startDestination = startDest, modifier = Modifier.padding(pad)) {

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

            composable("feed") {
                FeedScreen(vm,
                    onOpenComments = { nav.navigate("comments/$it") },
                    onOpenProfile  = goProfile,
                    onNewPost      = { nav.navigate("newpost") },
                    onNewStory     = { nav.navigate("newstory") },
                    onOpenMembers  = { nav.navigate("members") },
                    onLogout       = { vm.logout(); nav.navigate("login") { popUpTo(0) { inclusive = true } } }
                )
            }
            composable("search") {
                SearchScreen(vm,
                    onOpenPost    = { nav.navigate("comments/$it") },
                    onOpenProfile = goProfile,
                    onOpenGroup   = { nav.navigate("groupchat/$it") }
                )
            }
            composable("notifications") {
                NotificationsScreen(vm,
                    onOpenPost         = { nav.navigate("comments/$it") },
                    onOpenConversation = { nav.navigate("chat/$it") },
                    onOpenProfile      = goProfile
                )
            }
            composable("chatlist") {
                ChatListScreen(vm,
                    onOpenChat    = { nav.navigate("chat/$it") },
                    onOpenGroup   = { nav.navigate("groupchat/$it") },
                    onOpenProfile = goProfile,
                    onCreateGroup = { nav.navigate("creategroup") }
                )
            }
            composable("myprofile") {
                ProfileScreen(vm, userId = vm.currentUserId,
                    onBack = { nav.popBackStack() }, onEditProfile = { nav.navigate("editprofile") },
                    onOpenChat = { nav.navigate("chat/$it") },
                    onAchievements = { nav.navigate("achievements/${vm.currentUserId}") },
                    onSettings = { nav.navigate("settings") }
                )
            }

            composable("newpost") { CreatePostScreen(vm, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
            composable("newstory") { CreateStoryScreen(vm, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
            composable("editprofile") { EditProfileScreen(vm, onSaved = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
            composable("members") { MembersScreen(vm, onOpenProfile = goProfile, onBack = { nav.popBackStack() }) }
            composable("settings") {
                SettingsScreen(vm,
                    onLogout = { vm.logout(); nav.navigate("login") { popUpTo(0) { inclusive = true } } },
                    onBack   = { nav.popBackStack() }
                )
            }
            composable("creategroup") {
                CreateGroupScreen(vm, onDone = { groupId -> nav.navigate("groupchat/$groupId") { popUpTo("creategroup") { inclusive = true } } }, onBack = { nav.popBackStack() })
            }

            composable("comments/{postId}", arguments = listOf(navArgument("postId") { type = NavType.StringType })) {
                CommentsScreen(vm, postId = it.arguments?.getString("postId") ?: "", onOpenProfile = goProfile, onBack = { nav.popBackStack() })
            }
            composable("profile/{uid}", arguments = listOf(navArgument("uid") { type = NavType.StringType })) {
                val uid = it.arguments?.getString("uid") ?: ""
                ProfileScreen(vm, userId = uid, onBack = { nav.popBackStack() },
                    onEditProfile  = { nav.navigate("editprofile") },
                    onOpenChat     = { convId -> nav.navigate("chat/$convId") },
                    onAchievements = { nav.navigate("achievements/$uid") },
                    onSettings     = {}
                )
            }
            composable("achievements/{uid}", arguments = listOf(navArgument("uid") { type = NavType.StringType })) {
                AchievementsScreen(vm, userId = it.arguments?.getString("uid") ?: "", onBack = { nav.popBackStack() })
            }
            composable("chat/{convId}", arguments = listOf(navArgument("convId") { type = NavType.StringType })) {
                val convId     = it.arguments?.getString("convId") ?: ""
                val myId       = vm.currentUserId
                val conv       = vm.conversations.collectAsState().value.find { c -> c.id == convId }
                val otherId    = conv?.participants?.firstOrNull { p -> p != myId } ?: ""
                val otherName  = conv?.participantNames?.get(otherId) ?: "Utilisateur"
                val profsMap   by vm.profilesMap.collectAsState()
                val otherPhoto = profsMap[otherId]?.photoUrl ?: ""
                ChatScreen(vm, convId = convId, otherUserId = otherId,
                    otherUsername = otherName, otherPhotoUrl = otherPhoto,
                    onOpenProfile = goProfile, onBack = { nav.popBackStack() }
                )
            }
            composable("groupchat/{groupId}", arguments = listOf(navArgument("groupId") { type = NavType.StringType })) {
                val groupId = it.arguments?.getString("groupId") ?: ""
                val group   = vm.groups.collectAsState().value.find { g -> g.id == groupId }
                GroupChatScreen(vm, groupId = groupId, group = group,
                    onOpenProfile = goProfile, onBack = { nav.popBackStack() }
                )
            }
        }
    }
}