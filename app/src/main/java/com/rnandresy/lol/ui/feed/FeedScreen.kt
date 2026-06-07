package com.rnandresy.lol.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.ui.components.AdminBadgeLabel
import com.rnandresy.lol.ui.components.AskipAudioPlayer
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.*
import com.rnandresy.lol.ui.components.AskipVideoPlayer
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.MentionText
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

val REACTIONS = listOf("❤️", "🔥", "😂", "😱", "👀")

// Mémorise la position scroll entre navigations
object FeedScrollState {
    var index: Int  = 0
    var offset: Int = 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    vm: AskipViewModel,
    onOpenComments: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onNewPost: () -> Unit,
    onNewStory: () -> Unit,
    onOpenMembers: () -> Unit,
    onLogout: () -> Unit
) {
    val feed         by vm.feedPosts.collectAsState()
    val stories      by vm.stories.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val uid           = vm.currentUserId

    var openStory   by remember { mutableStateOf<Story?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState(FeedScrollState.index, FeedScrollState.offset)
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        FeedScrollState.index  = listState.firstVisibleItemIndex
        FeedScrollState.offset = listState.firstVisibleItemScrollOffset
    }

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) { if (pullState.isRefreshing) vm.refreshFeed() }
    LaunchedEffect(isRefreshing) { if (!isRefreshing) pullState.endRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Askip",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = onOpenMembers) {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, null, modifier = Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    titleContentColor      = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick        = { showFabMenu = false; onNewStory() },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor   = MaterialTheme.colorScheme.onSurface,
                            shape          = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(16.dp))
                                Text("Story 24h", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        SmallFloatingActionButton(
                            onClick        = { showFabMenu = false; onNewPost() },
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor   = MaterialTheme.colorScheme.background,
                            shape          = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                Text("Nouveau post", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick        = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor   = MaterialTheme.colorScheme.background,
                    shape          = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, null)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            if (feed.isEmpty() && !isRefreshing) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    EmptyState("👻", "Aucun post", "Sois le premier à poster !")
                }
            } else {
                LazyColumn(
                    state               = listState,
                    contentPadding      = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (stories.isNotEmpty()) {
                        item { StoriesRow(stories, uid, onNewStory, { openStory = it }) }
                        item {
                            HorizontalDivider(
                                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                    items(feed, key = { it.id }) { post ->
                        PostCard(
                            post          = post,
                            currentUid    = uid,
                            onAvatarClick = { if (!post.isAnonymous) onOpenProfile(post.userId) },
                            onReaction    = { vm.toggleReaction(post, it) },
                            onVotePoll    = { vm.votePoll(post.id, it) },
                            onComment     = { onOpenComments(post.id) },
                            onPin         = { vm.togglePin(post) },
                            onDelete      = { vm.deletePost(post.id) }
                        )
                        HorizontalDivider(
                            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
            PullToRefreshContainer(
                state    = pullState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }

    openStory?.let { story ->
        StoryFullScreen(
            story      = story,
            currentUid = uid,
            onDelete   = { vm.deleteStory(story.id); openStory = null },
            onClose    = { openStory = null }
        )
    }
}

// ── Stories horizontales ──────────────────────────────────────────────────────
@Composable
private fun StoriesRow(
    stories: List<Story>,
    currentUid: String,
    onAdd: () -> Unit,
    onOpen: (Story) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StoryCircle(
                label   = "Ajouter",
                content = {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp))
                    }
                },
                hasRing = false,
                onClick = onAdd
            )
        }
        items(stories, key = { it.id }) { story ->
            var showMenu by remember { mutableStateOf(false) }
            val isMe = story.userId == currentUid

            Box {
                StoryCircle(
                    label   = if (isMe) "Toi" else story.username.take(9),
                    content = {
                        val bg = runCatching {
                            Color(android.graphics.Color.parseColor(story.backgroundColor))
                        }.getOrElse { MaterialTheme.colorScheme.primary }
                        Box(
                            modifier         = Modifier.fillMaxSize().background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(story.emoji.ifBlank { "💭" }, fontSize = 26.sp)
                        }
                    },
                    hasRing = true,
                    onClick = { if (isMe) showMenu = true else onOpen(story) }
                )
                if (isMe) {
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text        = { Text("Voir") },
                            leadingIcon = { Icon(Icons.Default.Visibility, null) },
                            onClick     = { showMenu = false; onOpen(story) }
                        )
                        DropdownMenuItem(
                            text        = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick     = { showMenu = false }  // vm.deleteStory passé via onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryCircle(
    label: String,
    content: @Composable BoxScope.() -> Unit,
    hasRing: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier            = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .then(
                    if (hasRing) Modifier.border(
                        2.dp, MaterialTheme.colorScheme.onBackground, CircleShape
                    ) else Modifier
                )
                .padding(if (hasRing) 2.dp else 0.dp)
                .clip(CircleShape),
            content = content
        )
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Story plein écran ─────────────────────────────────────────────────────────
@Composable
private fun StoryFullScreen(
    story: Story, currentUid: String,
    onDelete: () -> Unit, onClose: () -> Unit
) {
    val bg = runCatching {
        Color(android.graphics.Color.parseColor(story.backgroundColor))
    }.getOrElse { MaterialTheme.colorScheme.surface }

    Dialog(
        onDismissRequest = onClose,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier         = Modifier.fillMaxSize().background(bg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.padding(40.dp)
            ) {
                Text(story.emoji.ifBlank { "💭" }, fontSize = 72.sp)
                Spacer(Modifier.height(20.dp))
                Text(
                    story.content,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "— ${story.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            if (story.userId == currentUid) {
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                }
            }
        }
    }
}

// ── Card post ─────────────────────────────────────────────────────────────────
@Composable
fun PostCard(
    post: Post, currentUid: String,
    onAvatarClick: () -> Unit,
    onReaction: (String) -> Unit,
    onVotePoll: (Int) -> Unit,
    onComment: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val isMyPost    = post.userId == currentUid && !post.isAnonymous
    val userIsAdmin = isAdmin(currentUid)
    val myReaction  = post.getUserReaction(currentUid)
    val postIsAdmin = isAdmin(post.userId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (post.isPinned)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.background
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            AskipAvatar(
                username    = if (post.isAnonymous) "?" else post.username,
                photoUrl    = if (post.isAnonymous) "" else post.userPhotoUrl,
                size        = 40.dp,
                isAdminUser = postIsAdmin && !post.isAnonymous,
                onClick     = if (!post.isAnonymous) onAvatarClick else null
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        if (post.isAnonymous) "Quelqu'un 🎭" else post.username,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (postIsAdmin && !post.isAnonymous) AdminGold
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (postIsAdmin && !post.isAnonymous) AdminBadgeLabel()
                    if (post.isPinned) {
                        Icon(
                            Icons.Default.PushPin, null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (post.postType == "poll") {
                        Surface(
                            color  = MaterialTheme.colorScheme.surfaceVariant,
                            shape  = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Sondage",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    formatTs(post.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions admin/auteur
            Row {
                if (userIsAdmin) {
                    IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.PushPin, null,
                            modifier = Modifier.size(16.dp),
                            tint     = if (post.isPinned) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                }
                if (isMyPost || userIsAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete, null,
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── Contenu ───────────────────────────────────────────────────────────
        if (post.content.isNotBlank()) {
            MentionText(
                text  = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── Image ─────────────────────────────────────────────────────────────
        if (post.imageUrl.isNotBlank()) {
            AsyncImage(
                model              = post.imageUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // ── Vidéo ─────────────────────────────────────────────────────────────
        if (post.videoUrl.isNotBlank()) {
            AskipVideoPlayer(
                videoUrl = post.videoUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // ── Audio ─────────────────────────────────────────────────────────────
        if (post.audioUrl.isNotBlank()) {
            Surface(
                color  = MaterialTheme.colorScheme.surfaceVariant,
                shape  = RoundedCornerShape(12.dp)
            ) {
                AskipAudioPlayer(
                    url      = post.audioUrl,
                    duration = post.audioDuration,
                    isMe     = false
                )
            }
        }

        // ── Sondage ───────────────────────────────────────────────────────────
        if (post.postType == "poll" && post.pollOption1.isNotBlank()) {
            PollSection(post = post, currentUid = currentUid, onVote = onVotePoll)
        }

        // ── Réactions + commentaires ──────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Réactions
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                REACTIONS.forEach { emoji ->
                    ReactionButton(
                        emoji    = emoji,
                        count    = post.reactionCount(emoji),
                        isActive = myReaction == emoji,
                        onClick  = { onReaction(emoji) }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // Commentaires
            TextButton(
                onClick        = onComment,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(15.dp))
                if (post.commentCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${post.commentCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ── Bouton réaction ───────────────────────────────────────────────────────────
@Composable
private fun ReactionButton(emoji: String, count: Int, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color   = if (isActive)
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        else Color.Transparent,
        shape   = RoundedCornerShape(8.dp),
        border  = if (isActive)
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(0.2f))
        else null
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 14.sp)
            if (count > 0) {
                Text(
                    "$count",
                    style    = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color    = if (isActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Section sondage ───────────────────────────────────────────────────────────
@Composable
private fun PollSection(post: Post, currentUid: String, onVote: (Int) -> Unit) {
    val hasVoted = currentUid in post.pollVoters
    val total    = (post.pollVotes1 + post.pollVotes2).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(1, post.pollOption1, post.pollVotes1),
            Triple(2, post.pollOption2, post.pollVotes2)
        ).forEach { (opt, label, votes) ->
            val pct = if (hasVoted) votes.toFloat() / total else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(if (hasVoted && pct > 0.5f) 0.8f else 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = !hasVoted) { onVote(opt) }
            ) {
                // Barre de progression
                if (hasVoted && pct > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(0.06f))
                    )
                }
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (hasVoted && pct > 0.5f) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (hasVoted) {
                        Text(
                            "${(pct * 100).toInt()}%",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Text(
            if (hasVoted) "${post.pollVotes1 + post.pollVotes2} participant(s)"
            else "Appuie pour voter",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}