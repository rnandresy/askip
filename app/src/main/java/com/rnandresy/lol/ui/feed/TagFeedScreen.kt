package com.rnandresy.lol.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.PostSkeleton
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.tagDef
import com.rnandresy.lol.viewmodel.AskipViewModel

/**
 * Le salon d'un tag : toutes les rumeurs qui parlent du même sujet.
 *
 * C'est ce qui transforme un fil unique en plusieurs conversations —
 * on vient pour #drama, on reste pour #exam.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFeedScreen(
    vm: AskipViewModel,
    tag: String,
    onOpenComments: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    onBack: () -> Unit
) {
    val posts by vm.tagPosts.collectAsState()
    val canScoop by vm.canScoopToday.collectAsState()
    val uid = vm.currentUserId
    val def = tagDef(tag)

    // `openTag` vide la liste avant de la recharger : sans ce drapeau on
    // afficherait « rien dans ce salon » pendant le chargement.
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(tag) {
        loading = true
        vm.openTag(tag).join()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${def?.emoji ?: "#"}  ${def?.label ?: tag}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    BubbleIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Retour",
                        onClick = onBack,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                loading && posts.isEmpty() -> Column { repeat(3) { PostSkeleton() } }

                posts.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    EmptyState(
                        def?.emoji ?: "◇",
                        "Rien dans ce salon",
                        "Sois le premier à lancer une rumeur avec #$tag"
                    )
                }

                else -> LazyColumn(contentPadding = PaddingValues(bottom = Space.huge)) {
                    items(posts, key = { it.id }) { post ->
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
                            canScoop = canScoop
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}
