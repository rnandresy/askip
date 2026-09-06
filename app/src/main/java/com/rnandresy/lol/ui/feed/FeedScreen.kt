@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.LevelUpBanner
import com.rnandresy.lol.ui.components.PostSkeleton
import com.rnandresy.lol.ui.components.SheetAction
import com.rnandresy.lol.ui.components.SheetHeader
import com.rnandresy.lol.ui.components.SlidingSegmented
import com.rnandresy.lol.ui.components.StarDust
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.TruthPageHeader
import com.rnandresy.lol.ui.components.VoiceRecordFab
import com.rnandresy.lol.ui.components.glyphForFeedTab
import com.rnandresy.lol.ui.components.glyphForSection
import com.rnandresy.lol.ui.components.glyphForSymbol
import com.rnandresy.lol.ui.components.glyphForWeather
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.FeedSection
import com.rnandresy.lol.utils.FeedTab
import com.rnandresy.lol.utils.POST_TYPE_NORMAL
import com.rnandresy.lol.utils.POST_TYPE_TRUTH
import com.rnandresy.lol.utils.levelTitle

/** Mémorise la position de défilement entre deux navigations. */
object FeedScrollState {
    var index: Int = 0
    var offset: Int = 0
}

/**
 * Le fil.
 *
 * Il empilait six bandeaux avant la première rumeur : météo, sections, tri,
 * sujet du jour, stories, tendances. Il n'en reste qu'un — le sélecteur de
 * section — plus une ligne de réglages discrète. Le tri, l'ambiance du campus
 * et le sujet du jour sont toujours là, à un geste.
 */
@Composable
fun FeedScreen(
    vm: com.rnandresy.lol.viewmodel.AskipViewModel,
    onOpenComments: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onNewPost: (String) -> Unit,
    onNewStory: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenTag: (String) -> Unit = {},
    onOpenQuests: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onOpenMembers: () -> Unit = {}
) {
    val feed by vm.feedPosts.collectAsState()
    val stories by vm.stories.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val feedLoaded by vm.feedLoaded.collectAsState()
    val hasMore by vm.hasMorePosts.collectAsState()
    val section by vm.feedSection.collectAsState()
    val tab by vm.feedTab.collectAsState()
    val ledger by vm.truthLedger.collectAsState()
    val weather by vm.campusWeather.collectAsState()
    val trending by vm.trendingTags.collectAsState()
    val canScoop by vm.canScoopToday.collectAsState()
    val myBets by vm.myBets.collectAsState()
    val betTokens by vm.betTokensLeft.collectAsState()
    val sealed by vm.sealedContents.collectAsState()
    val levelUp by vm.levelUp.collectAsState()
    val unreadNotifs by vm.unreadNotifCount.collectAsState()
    val isRecording by vm.isRecording.collectAsState()
    val recordingSeconds by vm.recordingSeconds.collectAsState()

    val context = LocalContext.current
    val uid = vm.currentUserId

    var openStory by remember { mutableStateOf<Story?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showTuning by remember { mutableStateOf(false) }

    val listState = rememberLazyListState(FeedScrollState.index, FeedScrollState.offset)
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        FeedScrollState.index = listState.firstVisibleItemIndex
        FeedScrollState.offset = listState.firstVisibleItemScrollOffset
    }
    var firstPass by remember { mutableStateOf(true) }
    LaunchedEffect(tab, section) {
        if (firstPass) firstPass = false else listState.scrollToItem(0)
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = {
                    Text(
                        "Askip",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    BubbleIconButton(
                        icon = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        onClick = onOpenNotifications,
                        badge = unreadNotifs,
                        modifier = Modifier.padding(end = Space.lg)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FeedFab(
                section = section,
                expanded = showFabMenu,
                isRecording = isRecording,
                recordingSeconds = recordingSeconds,
                onToggle = { showFabMenu = !showFabMenu },
                onNewStory = { showFabMenu = false; onNewStory() },
                onNewPost = { showFabMenu = false; onNewPost(POST_TYPE_NORMAL) },
                onNewTruth = { onNewPost(POST_TYPE_TRUTH) },
                onStartVoice = { vm.startVoiceRecording(context) },
                onStopVoice = {
                    vm.stopRecordingForPost()?.let { (file, seconds) ->
                        vm.createVoicePost(file, seconds)
                    }
                },
                onCancelVoice = vm::cancelVoiceRecording
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { vm.refreshFeed() },
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            Column {
                LevelUpBanner(
                    level = levelUp,
                    title = levelTitle(levelUp ?: 1),
                    onDismiss = vm::clearLevelUp
                )

                SlidingSegmented(
                    items = FeedSection.entries.toList(),
                    selected = section,
                    labelOf = { it.label },
                    onSelect = vm::setFeedSection,
                    modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
                    glyphOf = ::glyphForSection
                )

                // Les réglages du fil sont regroupés sur une seule ligne
                // discrète : le tri à gauche, l'ambiance du campus à droite.
                if (section == FeedSection.MAIN) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.lg, vertical = Space.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        BubbleChip(
                            label = tab.label,
                            glyph = glyphForFeedTab(tab),
                            accent = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showTuning = true }
                        )
                        Spacer(Modifier.weight(1f))
                        BubbleChip(
                            label = weather.weather.label,
                            glyph = glyphForWeather(weather.weather),
                            accent = LocalAskipPalette.current.contested,
                            onClick = { showTuning = true }
                        )
                    }
                }

                when {
                    feed.isEmpty() && (isRefreshing || !feedLoaded) ->
                        Column(Modifier.weight(1f)) { repeat(4) { PostSkeleton() } }

                    feed.isEmpty() -> Box(
                        Modifier.weight(1f).fillMaxSize(),
                        Alignment.Center
                    ) {
                        FeedEmptyState(section, tab)
                    }

                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 110.dp, top = Space.xs)
                    ) {
                        if (section == FeedSection.TRUTH) {
                            item(key = "truth") { TruthPageHeader(ledger) }
                        }
                        if (section == FeedSection.MAIN && stories.isNotEmpty()) {
                            item(key = "stories") {
                                StoriesRow(stories, uid, onNewStory) { openStory = it }
                            }
                        }

                        itemsIndexed(feed, key = { _, p -> p.id }) { index, post ->
                            PostCard(
                                post = post,
                                currentUid = uid,
                                onAvatarClick = {
                                    if (!post.isAnonymous) onOpenProfile(post.userId)
                                },
                                onReaction = { vm.toggleReaction(post, it) },
                                onVotePoll = { vm.votePoll(post.id, it) },
                                onComment = { onOpenComments(post.id) },
                                onPin = { vm.togglePin(post) },
                                onDelete = { vm.deletePost(post.id) },
                                onVoteVerdict = { vm.voteVerdict(post, it) },
                                onScoop = { vm.giveScoop(post) },
                                onTagClick = onOpenTag,
                                onReport = { vm.reportPost(post, "Signalement") },
                                onGiveKey = { vm.giveKey(post) },
                                onPlaceBet = { side, stake -> vm.placeBet(post, side, stake) },
                                onRevealSealed = { vm.revealSealed(post.id) },
                                canScoop = canScoop,
                                hotRank = if (section == FeedSection.MAIN &&
                                    tab == FeedTab.HOT && index < 3
                                ) index + 1 else 0,
                                myBet = myBets[post.id],
                                betTokens = betTokens,
                                sealedContent = sealed[post.id]?.content
                            )
                        }

                        // Le pied de liste n'est composé qu'une fois atteint :
                        // c'est lui qui déclenche la page suivante.
                        item(key = "footer") {
                            if (hasMore) {
                                LaunchedEffect(feed.size) { vm.loadMorePosts() }
                                PostSkeleton()
                            } else if (feed.size > 5) {
                                Text(
                                    "Tu as tout lu.",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Space.xxl),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTuning) {
        FeedTuningSheet(
            currentTab = tab,
            weatherLabel = weather.weather.label,
            weatherBlurb = weather.weather.blurb,
            postsToday = weather.postsToday,
            prompt = vm.dailyPrompt,
            trending = trending,
            onPickTab = { vm.setFeedTab(it); showTuning = false },
            onWritePrompt = { showTuning = false; onNewPost(POST_TYPE_NORMAL) },
            onOpenQuests = { showTuning = false; onOpenQuests() },
            onOpenLeaderboard = { showTuning = false; onOpenLeaderboard() },
            onOpenMembers = { showTuning = false; onOpenMembers() },
            onOpenTag = { showTuning = false; onOpenTag(it) },
            onDismiss = { showTuning = false }
        )
    }

    openStory?.let { story ->
        StoryFullScreen(
            story = story,
            currentUid = uid,
            onDelete = { vm.deleteStory(story.id); openStory = null },
            onClose = { openStory = null }
        )
    }
}

@Composable
private fun FeedEmptyState(section: FeedSection, tab: FeedTab) {
    when (section) {
        FeedSection.VOICE -> EmptyState(GlyphKind.PEOPLE, "Silence radio",
            "Appuie sur le micro et lance la première rumeur vocale."
        )
        FeedSection.TRUTH -> EmptyState(GlyphKind.SCALE, "Registre vierge",
            "Aucun serment n'a encore été prêté."
        )
        FeedSection.MAIN -> EmptyState(
            glyph = glyphForFeedTab(tab),
            title = when (tab) {
                FeedTab.CONFESSIONS -> "Aucune confession"
                FeedTab.LEGENDS -> "Pas encore de légende"
                else -> "Le campus est calme"
            },
            subtitle = "Lance la première rumeur"
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Réglages du fil
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Tout ce qui encombrait le haut du fil, rassemblé en un seul endroit :
 * l'ordre d'affichage, l'ambiance du campus, le sujet du jour, les missions.
 */
@Composable
private fun FeedTuningSheet(
    currentTab: FeedTab,
    weatherLabel: String,
    weatherBlurb: String,
    postsToday: Int,
    prompt: String,
    trending: List<com.rnandresy.lol.utils.RumorEngine.TrendingTag>,
    onPickTab: (FeedTab) -> Unit,
    onWritePrompt: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenMembers: () -> Unit,
    onOpenTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(bottom = Space.xxl)) {
            SheetHeader("Trier le fil")
            FeedTab.entries.forEach { entry ->
                SheetAction(
                    glyph = glyphForFeedTab(entry),
                    label = entry.label,
                    subtitle = if (entry == currentTab) "Actuellement affiché" else null,
                    onClick = { onPickTab(entry) }
                )
            }

            Spacer(Modifier.height(Space.md))
            SheetHeader("Le campus", "$weatherLabel — $weatherBlurb")
            SheetAction(
                glyph = GlyphKind.CHART,
                label = "$postsToday rumeur(s) sur 24 h",
                subtitle = "L'ambiance se calcule sur l'activité récente.",
                onClick = onDismiss
            )
            SheetAction(
                glyph = GlyphKind.SUN,
                label = "Sujet du jour",
                subtitle = prompt,
                onClick = onWritePrompt
            )
            SheetAction(
                glyph = GlyphKind.STAR,
                label = "Missions du jour",
                subtitle = "Trois missions, renouvelées à minuit.",
                onClick = onOpenQuests
            )
            SheetAction(
                glyph = GlyphKind.FLAME,
                label = "Classement",
                subtitle = "Informateurs, oracles, séries, rumeurs cultes.",
                onClick = onOpenLeaderboard
            )
            SheetAction(
                glyph = GlyphKind.PEOPLE,
                label = "Membres du campus",
                onClick = onOpenMembers
            )

            // « Ça circule » occupait une bande permanente dans le fil. Les
            // tags restent ici, où on vient chercher de quoi explorer.
            if (trending.isNotEmpty()) {
                Spacer(Modifier.height(Space.md))
                SheetHeader("Ça circule", "Ce dont le campus parle en ce moment.")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    items(trending, key = { it.slug }) { tag ->
                        com.rnandresy.lol.ui.components.TagChip(
                            slug = tag.slug,
                            count = tag.count,
                            onClick = { onOpenTag(tag.slug) }
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Bouton de création
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedFab(
    section: FeedSection,
    expanded: Boolean,
    isRecording: Boolean,
    recordingSeconds: Int,
    onToggle: () -> Unit,
    onNewStory: () -> Unit,
    onNewPost: () -> Unit,
    onNewTruth: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onCancelVoice: () -> Unit
) {
    when (section) {
        FeedSection.VOICE -> VoiceRecordFab(
            isRecording = isRecording,
            seconds = recordingSeconds,
            onStart = onStartVoice,
            onStop = onStopVoice,
            onCancel = onCancelVoice
        )

        FeedSection.TRUTH -> BubbleButton(
            text = "Jurer",
            glyph = GlyphKind.SCALE,
            onClick = onNewTruth,
            size = BubbleSize.LARGE
        )

        FeedSection.MAIN -> Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    BubbleButton(
                        text = "Story 24 h",
                        icon = Icons.Rounded.AutoStories,
                        onClick = onNewStory,
                        tone = BubbleTone.SOFT,
                        size = BubbleSize.SMALL
                    )
                    BubbleButton(
                        text = "Nouvelle rumeur",
                        icon = Icons.Rounded.Edit,
                        onClick = onNewPost,
                        size = BubbleSize.SMALL
                    )
                }
            }
            BubbleIconButton(
                icon = if (expanded) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = if (expanded) "Fermer" else "Créer",
                onClick = onToggle,
                tone = BubbleTone.PRIMARY,
                diameter = 58.dp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Stories
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun StoriesRow(
    stories: List<Story>,
    currentUid: String,
    onAdd: () -> Unit,
    onOpen: (Story) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            StoryCircle("Ajouter", hasRing = false, onClick = onAdd) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AskipGlyph(
                        kind = GlyphKind.PLUS,
                        size = 21.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(stories, key = { it.id }) { story ->
            StoryCircle(
                label = if (story.userId == currentUid) "Toi" else story.username.take(9),
                hasRing = true,
                seen = story.isSeenBy(currentUid),
                onClick = { onOpen(story) }
            ) {
                val bg = runCatching {
                    Color(android.graphics.Color.parseColor(story.backgroundColor))
                }.getOrElse { MaterialTheme.colorScheme.primary }
                Box(
                    Modifier.fillMaxSize().background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    AskipGlyph(kind = glyphForSymbol(story.emoji), size = 26.dp, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StoryCircle(
    label: String,
    hasRing: Boolean,
    onClick: () -> Unit,
    seen: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    TapArea(onTap = onClick, scaleDown = 0.93f) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .then(
                        // L'anneau s'éteint une fois la story vue : on repère
                        // d'un coup d'œil ce qui reste à découvrir.
                        if (hasRing) Modifier.border(
                            2.dp,
                            if (seen) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.primary,
                            CircleShape
                        ) else Modifier
                    )
                    .padding(if (hasRing) 3.dp else 0.dp)
                    .clip(CircleShape),
                content = content
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StoryFullScreen(
    story: Story,
    currentUid: String,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val bg = runCatching {
        Color(android.graphics.Color.parseColor(story.backgroundColor))
    }.getOrElse { MaterialTheme.colorScheme.surface }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(bg),
            contentAlignment = Alignment.Center
        ) {
            StarDust(count = 30, seed = 21, tint = Color.White)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Space.huge)
            ) {
                AskipGlyph(kind = glyphForSymbol(story.emoji), size = 72.dp, tint = Color.White)
                Spacer(Modifier.height(Space.xl))
                Text(
                    story.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.md))
                Text(
                    "— ${story.username} · disparaît dans ${story.hoursLeft()} h",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                if (story.userId == currentUid) {
                    BubbleChip("Supprimer", glyph = GlyphKind.CROSS, onClick = onDelete)
                }
                BubbleChip("Fermer", glyph = GlyphKind.CROSS, onClick = onClose)
            }
        }
    }
}
