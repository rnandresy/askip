package com.rnandresy.lol.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.theme.PurplePrimary
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: AskipViewModel,
    onOpenComments: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onNewPost: () -> Unit,
    onLogout: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val allBadges by viewModel.allBadges.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val currentUserId = viewModel.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Askip 💬", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewPost,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Poster") }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshFeed() },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            if (posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Aucun post pour l'instant", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        val profile = allProfiles.find { it.userId == post.userId }
                        val badges = profile?.badgeIds?.mapNotNull { bid -> allBadges.find { it.id == bid } } ?: emptyList()
                        PostCard(
                            post = post,
                            badges = badges,
                            currentUserId = currentUserId,
                            onAvatarClick = { onOpenProfile(post.userId) },
                            onLike = { viewModel.toggleLike(post) },
                            onComments = { onOpenComments(post.id) },
                            onPin = { viewModel.togglePin(post) },
                            onDelete = { viewModel.deletePost(post.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    badges: List<Badge>,
    currentUserId: String,
    onAvatarClick: () -> Unit,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val liked = post.likedBy.contains(currentUserId)
    val isMyPost = post.userId == currentUserId
    val userIsAdmin = isAdmin(currentUserId)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    photoUrl = post.userPhotoUrl,
                    username = post.username,
                    size = 42,
                    isAdmin = isAdmin(post.userId),
                    onClick = onAvatarClick
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (isAdmin(post.userId)) {
                            Spacer(Modifier.width(4.dp))
                            AdminBadge()
                        }
                        if (post.isPinned) {
                            Spacer(Modifier.width(4.dp))
                            Text("📌", fontSize = 12.sp)
                        }
                    }
                    if (badges.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            badges.take(3).forEach { BadgeChip(it) }
                        }
                    }
                    val timeStr = remember(post.timestamp) {
                        SimpleDateFormat("dd MMM · HH:mm", Locale.FRENCH).format(Date(post.timestamp))
                    }
                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (userIsAdmin) {
                    IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (post.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Épingler",
                            tint = if (post.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isMyPost || userIsAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(post.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostAction(
                    icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = "${post.likes}",
                    tint = if (liked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onLike
                )
                PostAction(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "${post.commentCount}",
                    onClick = onComments
                )
            }
        }
    }
}

@Composable
fun PostAction(icon: ImageVector, label: String, tint: Color = LocalContentColor.current, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
fun UserAvatar(
    photoUrl: String,
    username: String,
    size: Int,
    isAdmin: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        if (isAdmin) Color(0xFFFFD700).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .then(
                        if (isAdmin) Modifier.border(2.dp, Color(0xFFFFD700), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (username.firstOrNull()?.uppercase() ?: "?"),
                    fontSize = (size / 2.5).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdmin) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AdminBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFD700).copy(alpha = 0.15f),
        modifier = Modifier.border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
    ) {
        Text("👑 ADMIN", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
    }
}

@Composable
fun BadgeChip(badge: Badge) {
    val color = try { Color(android.graphics.Color.parseColor(badge.colorHex)) } catch (_: Exception) { PurplePrimary }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
    ) {
        Text(
            badge.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}