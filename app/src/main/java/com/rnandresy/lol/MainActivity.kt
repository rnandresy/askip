package com.rnandresy.lol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home

import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.rnandresy.lol.ui.chat.CreateGroupScreen
import com.rnandresy.lol.ui.chat.GroupChatScreen
import com.rnandresy.lol.ui.confession.ConfessionsScreen
import com.rnandresy.lol.ui.feed.FeedScreen
import com.rnandresy.lol.ui.feed.TagFeedScreen
import com.rnandresy.lol.ui.leaderboard.LeaderboardScreen
import com.rnandresy.lol.ui.members.MembersScreen
import com.rnandresy.lol.ui.notifications.NotificationsScreen
import com.rnandresy.lol.ui.post.CommentsScreen
import com.rnandresy.lol.ui.post.CreatePostScreen
import com.rnandresy.lol.ui.post.CreateStoryScreen
import com.rnandresy.lol.ui.profile.AchievementsScreen
import com.rnandresy.lol.ui.profile.EditProfileScreen
import com.rnandresy.lol.ui.profile.ProfileScreen
import com.rnandresy.lol.ui.quests.QuestsScreen
import com.rnandresy.lol.ui.search.SearchScreen
import com.rnandresy.lol.ui.settings.SettingsScreen
import com.rnandresy.lol.ui.theme.AskipTheme
import com.rnandresy.lol.viewmodel.AskipViewModel

class MainActivity : ComponentActivity() {

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val vm: AskipViewModel = viewModel()
            val theme by vm.appTheme.collectAsState()
            val haptics by vm.haptics.collectAsState()
            val reduceMotion by vm.reduceMotion.collectAsState()
            AskipTheme(
                appTheme = theme,
                haptics = haptics,
                reduceMotion = reduceMotion
            ) { AskipApp(vm) }
        }
    }
}

/**
 * Les destinations de la barre du bas.
 *
 * Quatre au lieu de cinq : les notifications sont remontées dans la barre du
 * fil, sous forme de cloche. Ce qui reste en bas, ce sont les deux choses
 * qu'on ouvre vraiment — l'actualité et son profil — plus la recherche et les
 * messages.
 */
private enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    FEED("feed", "Accueil", Icons.Rounded.Home),
    SEARCH("search", "Chercher", Icons.Rounded.Search),
    CHATS("chatlist", "Messages", Icons.Rounded.ChatBubble),
    PROFILE("myprofile", "Profil", Icons.Rounded.Person)
}

@Composable
fun AskipApp(vm: AskipViewModel) {
    val nav = rememberNavController()
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val startDest = remember { if (vm.isLoggedIn.value) "feed" else "login" }

    val unreadNotifs by vm.unreadNotifCount.collectAsState()
    val unreadMsgs by vm.unreadMessagesCount.collectAsState()
    val claimableXp by vm.claimableXp.collectAsState()

    val error by vm.error.collectAsState()
    val info by vm.info.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Un seul endroit affiche les messages d'erreur et de confirmation :
    // avant, chaque écran gérait les siens et certains n'en montraient aucun.
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(info) {
        info?.let {
            snackbar.showSnackbar(it)
            vm.clearInfo()
        }
    }

    val showNav = isLoggedIn && Tab.entries.any { it.route == route }

    val goProfile: (String) -> Unit = { uid ->
        val dest = if (uid == vm.currentUserId) "myprofile" else "profile/$uid"
        nav.navigate(dest) { launchSingleTop = true }
    }
    val goTab: (Tab) -> Unit = { tab ->
        nav.navigate(tab.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo("feed") { saveState = true }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(
                visible = showNav,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        val badge = when (tab) {
                            Tab.CHATS -> unreadMsgs
                            // Un simple point sur le profil : « il y a quelque
                            // chose à récupérer », sans nombre à comprendre.
                            Tab.PROFILE -> if (claimableXp > 0) -1 else 0
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goTab(tab) },
                            icon = { TabIcon(tab.icon, tab.label, selected, badge) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = startDest,
            modifier = Modifier.padding(pad)
        ) {

            // ── Authentification ──────────────────────────────────────────────
            composable("login") {
                LoginScreen(
                    vm,
                    onSuccess = { nav.navigate("feed") { popUpTo("login") { inclusive = true } } },
                    onGoRegister = { nav.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    vm,
                    onSuccess = {
                        nav.navigate("feed") { popUpTo("register") { inclusive = true } }
                    },
                    onGoLogin = { nav.popBackStack() }
                )
            }

            // ── Onglets principaux ────────────────────────────────────────────
            composable("feed") {
                FeedScreen(
                    vm,
                    onOpenComments = { nav.navigate("comments/$it") },
                    onOpenProfile = goProfile,
                    onNewPost = { type -> nav.navigate("newpost?type=$type") },
                    onNewStory = { nav.navigate("newstory") },
                    onOpenNotifications = { nav.navigate("notifications") },
                    onOpenTag = { nav.navigate("tag/$it") },
                    onOpenQuests = { nav.navigate("quests") },
                    onOpenLeaderboard = { nav.navigate("leaderboard") },
                    onOpenMembers = { nav.navigate("members") }
                )
            }
            composable("search") {
                SearchScreen(
                    vm,
                    onOpenPost = { nav.navigate("comments/$it") },
                    onOpenProfile = goProfile,
                    onOpenGroup = { nav.navigate("groupchat/$it") }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    vm,
                    onOpenPost = { nav.navigate("comments/$it") },
                    onOpenConversation = { nav.navigate("chat/$it") },
                    onOpenProfile = goProfile
                )
            }
            composable("chatlist") {
                ChatListScreen(
                    vm,
                    onOpenChat = { nav.navigate("chat/$it") },
                    onOpenGroup = { nav.navigate("groupchat/$it") },
                    onOpenProfile = goProfile,
                    onCreateGroup = { nav.navigate("creategroup") }
                )
            }
            composable("myprofile") {
                ProfileScreen(
                    vm, userId = vm.currentUserId,
                    onBack = { nav.popBackStack() },
                    onEditProfile = { nav.navigate("editprofile") },
                    onOpenChat = { nav.navigate("chat/$it") },
                    onAchievements = { nav.navigate("achievements/${vm.currentUserId}") },
                    onSettings = { nav.navigate("settings") }
                )
            }

            // ── Création ──────────────────────────────────────────────────────
            // Le type est optionnel : la Page de Vérité ouvre l'éditeur
            // directement sur « vérité », le reste garde l'ancien chemin.
            composable(
                "newpost?type={type}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "normal"
                    }
                )
            ) { entry ->
                CreatePostScreen(
                    vm,
                    onDone = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                    initialType = entry.arguments?.getString("type") ?: "normal"
                )
            }
            composable("newstory") {
                CreateStoryScreen(
                    vm,
                    onDone = { nav.popBackStack() },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("editprofile") {
                EditProfileScreen(
                    vm,
                    onSaved = { nav.popBackStack() },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("creategroup") {
                CreateGroupScreen(
                    vm,
                    onDone = { groupId ->
                        nav.navigate("groupchat/$groupId") {
                            popUpTo("creategroup") { inclusive = true }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            // ── Découverte ────────────────────────────────────────────────────
            composable("members") {
                MembersScreen(vm, onOpenProfile = goProfile, onBack = { nav.popBackStack() })
            }
            composable("confessions") {
                ConfessionsScreen(
                    vm,
                    onOpenComments = { nav.navigate("comments/$it") }
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(
                    vm,
                    onOpenProfile = goProfile,
                    onOpenPost = { nav.navigate("comments/$it") },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("quests") {
                QuestsScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(
                "tag/{tag}",
                arguments = listOf(navArgument("tag") { type = NavType.StringType })
            ) { entry ->
                TagFeedScreen(
                    vm,
                    tag = entry.arguments?.getString("tag").orEmpty(),
                    onOpenComments = { nav.navigate("comments/$it") },
                    onOpenProfile = goProfile,
                    onOpenTag = { nav.navigate("tag/$it") },
                    onBack = { nav.popBackStack() }
                )
            }

            // ── Réglages ──────────────────────────────────────────────────────
            composable("settings") {
                SettingsScreen(
                    vm,
                    onLogout = {
                        vm.logout()
                        nav.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            // ── Détails ───────────────────────────────────────────────────────
            composable(
                "comments/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { entry ->
                CommentsScreen(
                    vm,
                    postId = entry.arguments?.getString("postId").orEmpty(),
                    onOpenProfile = goProfile,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                "profile/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { entry ->
                val uid = entry.arguments?.getString("uid").orEmpty()
                ProfileScreen(
                    vm, userId = uid,
                    onBack = { nav.popBackStack() },
                    onEditProfile = { nav.navigate("editprofile") },
                    onOpenChat = { convId -> nav.navigate("chat/$convId") },
                    onAchievements = { nav.navigate("achievements/$uid") },
                    onSettings = {}
                )
            }
            composable(
                "achievements/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { entry ->
                AchievementsScreen(
                    vm,
                    userId = entry.arguments?.getString("uid").orEmpty(),
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                "chat/{convId}",
                arguments = listOf(navArgument("convId") { type = NavType.StringType })
            ) { entry ->
                val convId = entry.arguments?.getString("convId").orEmpty()
                val myId = vm.currentUserId
                val conv = vm.conversations.collectAsState().value.find { it.id == convId }
                val otherId = conv?.participants?.firstOrNull { it != myId }.orEmpty()
                val otherName = conv?.participantNames?.get(otherId) ?: "Utilisateur"
                val profs by vm.profilesMap.collectAsState()
                ChatScreen(
                    vm,
                    convId = convId,
                    otherUserId = otherId,
                    otherUsername = otherName,
                    otherPhotoUrl = profs[otherId]?.photoUrl.orEmpty(),
                    onOpenProfile = goProfile,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                "groupchat/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId").orEmpty()
                val group = vm.groups.collectAsState().value.find { it.id == groupId }
                GroupChatScreen(
                    vm,
                    groupId = groupId,
                    group = group,
                    onOpenProfile = goProfile,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

/**
 * Icône d'onglet avec sa pastille.
 * [badge] : nombre à afficher, 0 pour aucune pastille, -1 pour un simple point
 * (les missions à encaisser n'ont pas de compte utile à montrer).
 */
@Composable
private fun TabIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tabScale"
    )
    BadgedBox(
        badge = {
            when {
                badge > 0 -> Badge { Text(if (badge > 99) "99+" else "$badge") }
                badge < 0 -> Badge()
            }
        }
    ) {
        Icon(icon, label, Modifier.scale(scale))
    }
}
