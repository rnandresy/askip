package com.rnandresy.lol.ui.feed

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import java.text.SimpleDateFormat
import java.util.*

val REACTIONS = listOf("❤️", "🔥", "😂", "😱", "👀")

// Mémorise la position du scroll entre navigations
object FeedScrollState {
    var firstVisibleIndex:  Int = 0
    var firstVisibleOffset: Int = 0
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

    // Scroll mémorisé
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex        = FeedScrollState.firstVisibleIndex,
        initialFirstVisibleItemScrollOffset = FeedScrollState.firstVisibleOffset
    )
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        FeedScrollState.firstVisibleIndex  = listState.firstVisibleItemIndex
        FeedScrollState.firstVisibleOffset = listState.firstVisibleItemScrollOffset
    }

    // Pull-to-refresh
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) vm.refreshFeed()
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullState.endRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text("Askip 🔥", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onOpenMembers) { Icon(Icons.Default.People, null) }
                    IconButton(onClick = onLogout)      { Icon(Icons.Default.Logout, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick        = { showFabMenu = false; onNewStory() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "📖 Story",
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    SmallFloatingActionButton(
                        onClick        = { showFabMenu = false; onNewPost() },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "📢 Rumeur",
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, null)
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            if (feed.isEmpty() && !isRefreshing) {
                Column(
                    modifier            = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👻", fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aucune rumeur pour l'instant…",
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sois le premier à lancer le ragot ! 🔥",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state               = listState,
                    contentPadding      = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { StoriesBar(stories, uid, onNewStory) { openStory = it } }
                    items(feed, key = { it.id }) { post ->
                        PostCard(
                            post          = post,
                            currentUid    = uid,
                            onAvatarClick = { if (!post.isAnonymous) onOpenProfile(post.userId) },
                            onReaction    = { emoji -> vm.toggleReaction(post, emoji) },
                            onVotePoll    = { opt -> vm.votePoll(post.id, opt) },
                            onComment     = { onOpenComments(post.id) },
                            onPin         = { vm.togglePin(post) },
                            onDelete      = { vm.deletePost(post.id) }
                        )
                    }
                }
            }
            PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    openStory?.let { story ->
        StoryViewDialog(story = story, onClose = { openStory = null })
    }
}

// ── Stories Bar ───────────────────────────────────────────────────────────────

@Composable
fun StoriesBar(
    stories: List<Story>,
    currentUid: String,
    onAddStory: () -> Unit,
    onOpen: (Story) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.clickable(onClick = onAddStory)
            ) {
                Box(
                    modifier         = Modifier
                        .size(58.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Ma story", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
        items(stories, key = { it.id }) { story ->
            val bgColor = runCatching {
                Color(android.graphics.Color.parseColor(story.backgroundColor))
            }.getOrElse { Color(0xFF7C4DFF) }
            val isMe = story.userId == currentUid
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.clickable { onOpen(story) }
            ) {
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(bgColor)
                        .border(
                            2.5.dp,
                            if (isMe) MaterialTheme.colorScheme.tertiary else bgColor.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(story.emoji.ifBlank { "💭" }, fontSize = 24.sp) }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isMe) "Toi" else story.username.take(8),
                    style    = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StoryViewDialog(story: Story, onClose: () -> Unit) {
    val bgColor = runCatching {
        Color(android.graphics.Color.parseColor(story.backgroundColor))
    }.getOrElse { Color(0xFF7C4DFF) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text(story.emoji.ifBlank { "💭" }, fontSize = 80.sp)
                Spacer(Modifier.height(24.dp))
                Text(story.content, style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("— ${story.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }
}

// ── Post Card ─────────────────────────────────────────────────────────────────

@Composable
fun PostCard(
    post: Post,
    currentUid: String,
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

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    username = if (post.isAnonymous) "?" else post.username,
                    photoUrl = if (post.isAnonymous) "" else post.userPhotoUrl,
                    size     = 44,
                    isAdmin  = isAdmin(post.userId) && !post.isAnonymous,
                    onClick  = if (!post.isAnonymous) onAvatarClick else null
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            if (post.isAnonymous) "Quelqu'un 🎭" else post.username,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!post.isAnonymous && isAdmin(post.userId)) AdminBadge()
                        if (post.isPinned) Text("📌", fontSize = 11.sp)
                        if (post.postType == "poll") PollBadge()
                    }
                    Text(
                        formatTs(post.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (userIsAdmin) {
                    IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.PushPin, null,
                            tint     = if (post.isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isMyPost || userIsAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Contenu ───────────────────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            MentionText(text = post.content, style = MaterialTheme.typography.bodyMedium)

            // ── Image ─────────────────────────────────────────────────────────
            if (post.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model              = post.imageUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            // ── Vidéo ─────────────────────────────────────────────────────────
            if (post.videoUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                VideoPlayer(
                    videoUrl = post.videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            // ── Sondage ───────────────────────────────────────────────────────
            if (post.postType == "poll" && post.pollOption1.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                PollSection(post = post, currentUid = currentUid, onVote = onVotePoll)
            }

            // ── Réactions ─────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                REACTIONS.forEach { emoji ->
                    ReactionBtn(
                        emoji    = emoji,
                        count    = post.reactionCount(emoji),
                        isActive = myReaction == emoji,
                        onClick  = { onReaction(emoji) }
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onComment, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.commentCount}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Lecteur vidéo inline ──────────────────────────────────────────────────────

@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player         = exoPlayer
                useController  = true
                layoutParams   = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = modifier
    )
}

// ── Poll section ──────────────────────────────────────────────────────────────

@Composable
private fun PollSection(post: Post, currentUid: String, onVote: (Int) -> Unit) {
    val hasVoted = currentUid in post.pollVoters
    val total    = (post.pollVotes1 + post.pollVotes2).coerceAtLeast(1)
    val pct1     = if (hasVoted) post.pollVotes1 * 100 / total else 0
    val pct2     = if (hasVoted) post.pollVotes2 * 100 / total else 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(1, post.pollOption1, pct1) to post.pollVotes1,
            Triple(2, post.pollOption2, pct2) to post.pollVotes2
        ).forEach { (triple, votes) ->
            val (opt, label, pct) = triple
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !hasVoted) { onVote(opt) }
            ) {
                if (hasVoted && pct > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct / 100f).height(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (hasVoted) {
                        Text("$pct% ($votes)", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(
            if (hasVoted) "${post.pollVotes1 + post.pollVotes2} vote(s) au total"
            else "Appuie pour voter 👆",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Réaction bouton ───────────────────────────────────────────────────────────

@Composable
fun ReactionBtn(emoji: String, count: Int, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        color    = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape    = RoundedCornerShape(20.dp),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 13.sp)
            if (count > 0) {
                Text(
                    "$count",
                    style    = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color    = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Composants partagés ───────────────────────────────────────────────────────

/**
 * Avatar universel : affiche la photo si disponible, sinon l'initiale.
 * [photoUrl] est optionnel — rétrocompatible avec tous les appelants existants.
 */
@Composable
fun UserAvatar(
    username: String,
    photoUrl: String = "",       // ← paramètre optionnel, défaut ""
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
            // Photo réelle
            AsyncImage(
                model              = photoUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(
                        if (isAdmin) Modifier.border(2.dp, Color(0xFFFFD700), CircleShape)
                        else Modifier
                    )
            )
        } else {
            // Avatar initiale
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
                    text       = username.firstOrNull()?.uppercase() ?: "?",
                    fontSize   = (size / 2.5).sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isAdmin) Color(0xFFFFD700)
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AdminBadge() {
    Surface(
        color    = Color(0xFFFFD700).copy(alpha = 0.15f),
        shape    = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
    ) {
        Text(
            "👑",
            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize   = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700)
        )
    }
}

@Composable
fun PollBadge() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
        Text(
            "📊 Sondage",
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            fontSize   = 9.sp, fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun BadgeChip(displayName: String, colorHex: String, modifier: Modifier = Modifier) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(colorHex))
    }.getOrElse { Color(0xFF7C4DFF) }
    Surface(
        color    = color.copy(alpha = 0.15f),
        shape    = RoundedCornerShape(50),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
    ) {
        Text(
            displayName,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize   = 11.sp, fontWeight = FontWeight.Bold, color = color
        )
    }
}

fun formatTs(ts: Long): String = runCatching {
    SimpleDateFormat("dd MMM · HH:mm", Locale.FRENCH).format(Date(ts))
}.getOrElse { "" }