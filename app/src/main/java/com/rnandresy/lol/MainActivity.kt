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
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.rnandresy.lol.ui.feed.FeedScreen
import com.rnandresy.lol.ui.members.MembersScreen
import com.rnandresy.lol.ui.post.CommentsScreen
import com.rnandresy.lol.ui.post.CreatePostScreen
import com.rnandresy.lol.ui.profile.EditProfileScreen
import com.rnandresy.lol.ui.profile.ProfileScreen
import com.rnandresy.lol.ui.settings.SettingsScreen
import com.rnandresy.lol.ui.theme.AskipTheme
import com.rnandresy.lol.viewmodel.AskipViewModel

class MainActivity : ComponentActivity() {

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AskipTheme { AskipApp() } }
    }
}

sealed class BottomNav(val route: String, val icon: ImageVector, val label: String) {
    object Feed : BottomNav("feed", Icons.Default.Home, "Accueil")
    object Members : BottomNav("members", Icons.Default.People, "Membres")
    object Chat : BottomNav("chatlist", Icons.Default.Chat, "Messages")
    object Profile : BottomNav("myprofile", Icons.Default.Person, "Profil")
    object Settings : BottomNav("settings", Icons.Default.Settings, "Réglages")
}

val bottomNavRoutes = listOf(
    BottomNav.Feed, BottomNav.Members, BottomNav.Chat, BottomNav.Profile, BottomNav.Settings
)

@Composable
fun AskipApp() {
    val navController = rememberNavController()
    val vm: AskipViewModel = viewModel()
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes.map { it.route }
    val startDest = remember { if (vm.isLoggedIn.value) "feed" else "login" }

    val goToProfile: (String) -> Unit = { userId ->
        val route = if (userId == vm.currentUserId) "myprofile" else "profile/$userId"
        navController.navigate(route) { launchSingleTop = true }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = isLoggedIn && showBottomBar) {
                NavigationBar {
                    bottomNavRoutes.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth
            composable("login") {
                LoginScreen(
                    viewModel = vm,
                    onLoginSuccess = { navController.navigate("feed") { popUpTo("login") { inclusive = true } } },
                    onGoToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = vm,
                    onRegisterSuccess = { navController.navigate("feed") { popUpTo("register") { inclusive = true } } },
                    onGoToLogin = { navController.popBackStack() }
                )
            }

            // Main tabs
            composable("feed") {
                FeedScreen(vm,
                    onOpenComments = { navController.navigate("comments/$it") },
                    onOpenProfile = goToProfile,
                    onNewPost = { navController.navigate("newpost") },
                    onLogout = {
                        vm.logout()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("members") { MembersScreen(vm, onOpenProfile = goToProfile) }
            composable("chatlist") {
                ChatListScreen(vm,
                    onOpenConversation = { navController.navigate("chat/$it") },
                    onOpenProfile = goToProfile
                )
            }
            composable("myprofile") {
                ProfileScreen(vm, userId = vm.currentUserId,
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate("editprofile") },
                    onOpenChat = { navController.navigate("chat/$it") }
                )
            }
            composable("settings") {
                SettingsScreen(vm, onLogout = {
                    vm.logout()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                })
            }

            // Sub-screens
            composable("newpost") {
                CreatePostScreen(vm,
                    onPostCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() })
            }
            composable("editprofile") {
                EditProfileScreen(vm,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() })
            }
            composable(
                "comments/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) {
                CommentsScreen(vm,
                    postId = it.arguments?.getString("postId") ?: "",
                    onOpenProfile = goToProfile,
                    onBack = { navController.popBackStack() })
            }
            composable(
                "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) {
                ProfileScreen(vm,
                    userId = it.arguments?.getString("userId") ?: "",
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate("editprofile") },
                    onOpenChat = { convId -> navController.navigate("chat/$convId") })
            }
            composable(
                "chat/{convId}",
                arguments = listOf(navArgument("convId") { type = NavType.StringType })
            ) {
                val convId = it.arguments?.getString("convId") ?: ""
                val conv = vm.conversations.collectAsState().value.find { c -> c.id == convId }
                val otherId = conv?.participants?.firstOrNull { id -> id != vm.currentUserId } ?: ""
                ChatScreen(vm,
                    conversationId = convId,
                    otherUserId = otherId,
                    otherUsername = conv?.participantNames?.get(otherId) ?: "Utilisateur",
                    otherPhotoUrl = conv?.participantPhotos?.get(otherId) ?: "",
                    onOpenProfile = goToProfile,
                    onBack = { navController.popBackStack() })
            }
        }
    }
}