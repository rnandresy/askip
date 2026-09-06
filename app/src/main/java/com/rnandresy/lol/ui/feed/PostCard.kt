@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.model.Bet
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Verdict
import com.rnandresy.lol.ui.components.AdminBadgeLabel
import com.rnandresy.lol.ui.components.AskipAudioPlayer
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.AskipVideoPlayer
import com.rnandresy.lol.ui.components.BetPanel
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.ChainPreview
import com.rnandresy.lol.ui.components.MentionText
import com.rnandresy.lol.ui.components.OathLine
import com.rnandresy.lol.ui.components.OathStamp
import com.rnandresy.lol.ui.components.ReactionAction
import com.rnandresy.lol.ui.components.RumorMeter
import com.rnandresy.lol.ui.components.ScoopButton
import com.rnandresy.lol.ui.components.SealedCapsule
import com.rnandresy.lol.ui.components.SheetAction
import com.rnandresy.lol.ui.components.SheetHeader
import com.rnandresy.lol.ui.components.TagChip
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.VoiceHero
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.REPORT_REASONS
import com.rnandresy.lol.utils.VERDICT_MIN_VOTES
import com.rnandresy.lol.utils.isAdmin
import kotlinx.coroutines.launch

/**
 * La carte de rumeur.
 *
 * Elle empilait dix blocs : verdict, badge de chaleur, serment, Rumeur-mètre,
 * panneau de pari, cinq émojis de réaction, Scoop, tags, chaîne, droit de
 * réponse. À dix rumeurs à l'écran, plus rien ne ressortait.
 *
 * Il n'en reste que quatre — auteur, texte, média, une ligne d'actions. Tout
 * le reste vit dans une feuille qui s'ouvre d'un geste : rien n'a disparu, tout
 * demande simplement de le vouloir.
 */
@Composable
fun PostCard(
    post: Post,
    currentUid: String,
    onAvatarClick: () -> Unit,
    onReaction: (String) -> Unit,
    onVotePoll: (Int) -> Unit,
    onComment: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onVoteVerdict: (Boolean) -> Unit = {},
    onScoop: () -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onReport: () -> Unit = {},
    onGiveKey: () -> Unit = {},
    onPlaceBet: (Boolean, Int) -> Unit = { _, _ -> },
    onRevealSealed: () -> Unit = {},
    canScoop: Boolean = false,
    hotRank: Int = 0,
    myBet: Bet? = null,
    betTokens: Int = 0,
    sealedContent: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val isMyPost = post.userId == currentUid && !post.isAnonymous
    val userIsAdmin = isAdmin(currentUid)
    val postIsAdmin = isAdmin(post.userId) && !post.isAnonymous
    val verdict = post.verdict()

    var sheet by remember { mutableStateOf<PostSheet?>(null) }

    // On ne demande le contenu d'une capsule que lorsqu'elle est censée être
    // ouverte : avant l'heure, le serveur refuserait de toute façon.
    val sealOpen = post.isSealed() && post.isUnsealed()
    LaunchedEffect(post.id, sealOpen, isMyPost) {
        if (post.isSealed() && (sealOpen || isMyPost)) onRevealSealed()
    }

    BubbleCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm),
        fill = if (post.isPinned) MaterialTheme.colorScheme.surfaceVariant else palette.bubble,
        elevation = 3.dp,
        gloss = 0.35f
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            PostHeader(
                post = post,
                postIsAdmin = postIsAdmin,
                verdict = verdict,
                hotRank = hotRank,
                onAvatarClick = onAvatarClick,
                onMore = { sheet = PostSheet.MORE }
            )

            if (post.isTruth()) OathLine()

            if (post.isVoice()) {
                VoiceHero(url = post.audioUrl, duration = post.audioDuration)
            }

            if (post.content.isNotBlank()) {
                MentionText(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (post.isSealed()) {
                SealedCapsule(
                    post = post,
                    currentUid = currentUid,
                    revealedContent = sealedContent,
                    onGiveKey = onGiveKey
                )
            }

            if (post.isChain()) ChainPreview(post)

            PostMedia(post)

            if (post.postType == "poll" && post.pollOption1.isNotBlank()) {
                PollSection(post = post, currentUid = currentUid, onVote = onVotePoll)
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            PostActions(
                post = post,
                currentUid = currentUid,
                onReaction = onReaction,
                onComment = onComment,
                onOpenVerdict = { sheet = PostSheet.VERDICT }
            )
        }
    }

    when (sheet) {
        PostSheet.VERDICT -> VerdictSheet(
            post = post,
            currentUid = currentUid,
            myBet = myBet,
            betTokens = betTokens,
            canScoop = canScoop,
            onVoteVerdict = onVoteVerdict,
            onScoop = onScoop,
            onPlaceBet = onPlaceBet,
            onTagClick = onTagClick,
            onDismiss = { sheet = null }
        )

        PostSheet.MORE -> MoreSheet(
            post = post,
            isMyPost = isMyPost,
            userIsAdmin = userIsAdmin,
            onPin = onPin,
            onDelete = onDelete,
            onReport = { sheet = PostSheet.REPORT },
            onDismiss = { sheet = null }
        )

        PostSheet.REPORT -> ReportSheet(
            onConfirm = { onReport(); sheet = null },
            onDismiss = { sheet = null }
        )

        null -> Unit
    }
}

private enum class PostSheet { VERDICT, MORE, REPORT }

// ═════════════════════════════════════════════════════════════════════════════
//  En-tête
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PostHeader(
    post: Post,
    postIsAdmin: Boolean,
    verdict: Verdict,
    hotRank: Int,
    onAvatarClick: () -> Unit,
    onMore: () -> Unit
) {
    val palette = LocalAskipPalette.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        AskipAvatar(
            username = if (post.isAnonymous) "?" else post.username,
            photoUrl = if (post.isAnonymous) "" else post.userPhotoUrl,
            size = 40.dp,
            isAdminUser = postIsAdmin,
            onClick = if (!post.isAnonymous) onAvatarClick else null
        )
        Spacer(Modifier.width(Space.md))

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    if (post.isAnonymous) post.username.ifBlank { "Quelqu'un" }
                    else post.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (postIsAdmin) AdminGold else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (postIsAdmin) AdminBadgeLabel()
            }

            // Une seule ligne pour tous les états du post : heure, épinglage,
            // édition, expiration. Elles étaient réparties sur deux rangées.
            Text(
                buildString {
                    append(formatTs(post.timestamp))
                    if (post.isPinned) append(" · épinglé")
                    if (post.isEdited) append(" · modifié")
                    if (post.expiresAt > 0) append(" · éphémère")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Seul un verdict tranché mérite d'être affiché sur la carte : « en
        // enquête » est l'état par défaut, l'écrire n'apprend rien.
        when {
            post.isTruth() && verdict == Verdict.DEBUNKED -> OathStamp(perjury = true)
            post.isTruth() && verdict == Verdict.CONFIRMED -> OathStamp(perjury = false)
            verdict == Verdict.CONFIRMED ->
                BubbleChip("Confirmée", glyph = GlyphKind.CHECK, accent = palette.confirmed)
            verdict == Verdict.DEBUNKED ->
                BubbleChip("Démentie", glyph = GlyphKind.CROSS, accent = palette.debunked)
            hotRank in 1..3 ->
                BubbleChip("#$hotRank", glyph = GlyphKind.FLAME, accent = palette.heatHigh)
            else -> Unit
        }

        TapArea(onTap = onMore, scaleDown = 0.85f, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Rounded.MoreHoriz,
                "Options",
                Modifier.align(Alignment.Center).size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PostMedia(post: Post) {
    if (post.imageUrl.isNotBlank()) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = "Image de la rumeur",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .clip(RoundedCornerShape(Radius.md))
        )
    }
    if (post.videoUrl.isNotBlank()) {
        AskipVideoPlayer(
            videoUrl = post.videoUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Radius.md))
        )
    }
    // Note vocale jointe à une rumeur écrite. Exclue des rumeurs vocales :
    // elles ont déjà leur lecteur en tête de carte.
    if (post.audioUrl.isNotBlank() && !post.isVoice()) {
        Box(
            Modifier
                .clip(RoundedCornerShape(Radius.md))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AskipAudioPlayer(post.audioUrl, post.audioDuration, isMe = false)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  La ligne d'actions
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PostActions(
    post: Post,
    currentUid: String,
    onReaction: (String) -> Unit,
    onComment: () -> Unit,
    onOpenVerdict: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val votes = post.verdictVotes()
    val settled = votes >= VERDICT_MIN_VOTES
    val ratio = post.credibilityRatio()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        ReactionAction(
            myReaction = post.getUserReaction(currentUid),
            total = post.totalReactions(),
            onReact = onReaction
        )

        TapArea(onTap = onComment, scaleDown = 0.9f) {
            Row(
                modifier = Modifier.padding(Space.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Icon(
                    Icons.Rounded.ChatBubbleOutline,
                    "Commentaires",
                    Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (post.commentCount > 0) {
                    Text(
                        "${post.commentCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (post.scoopBy.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                AskipGlyph(kind = GlyphKind.GEM, size = 12.dp, tint = palette.scoop)
                Text(
                    "${post.scoopBy.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.scoop
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Le Rumeur-mètre tenait cinq lignes sur chaque carte. Il se résume ici
        // à une pastille : le chiffre suffit à savoir où en est la rumeur, et
        // un appui ouvre le vote, les paris et le Scoop.
        if (post.postType != "poll" && (!post.isSealed() || post.isUnsealed())) {
            BubbleChip(
                label = if (settled) "${(ratio * 100).toInt()} %" else "Enquête",
                glyph = if (settled) GlyphKind.SCALE else GlyphKind.SEARCH,
                accent = when {
                    !settled -> palette.unknown
                    ratio >= 0.6f -> palette.confirmed
                    ratio <= 0.4f -> palette.debunked
                    else -> palette.contested
                },
                onClick = onOpenVerdict
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Feuilles
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun VerdictSheet(
    post: Post,
    currentUid: String,
    myBet: Bet?,
    betTokens: Int,
    canScoop: Boolean,
    onVoteVerdict: (Boolean) -> Unit,
    onScoop: () -> Unit,
    onPlaceBet: (Boolean, Int) -> Unit,
    onTagClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier.padding(bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.lg)
        ) {
            SheetHeader(
                "Cette rumeur",
                "Tranche, mise, ou offre ton Scoop."
            )

            Column(Modifier.padding(horizontal = Space.xl)) {
                RumorMeter(
                    post = post,
                    currentUid = currentUid,
                    onVote = onVoteVerdict
                )
            }

            Column(Modifier.padding(horizontal = Space.xl)) {
                BetPanel(
                    post = post,
                    myBet = myBet,
                    tokensLeft = betTokens,
                    isMyPost = post.userId == currentUid,
                    onPlaceBet = { side, stake ->
                        onPlaceBet(side, stake)
                        close()
                    }
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = Space.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                ScoopButton(
                    given = currentUid in post.scoopBy,
                    count = post.scoopBy.size,
                    available = canScoop && post.userId != currentUid,
                    onClick = { onScoop(); close() }
                )
                Text(
                    if (currentUid in post.scoopBy) "Scoop déjà offert"
                    else "Un seul Scoop par jour — garde-le pour la bonne rumeur.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (post.tags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    items(post.tags) { tag ->
                        TagChip(slug = tag, onClick = { onTagClick(tag); close() })
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreSheet(
    post: Post,
    isMyPost: Boolean,
    userIsAdmin: Boolean,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(bottom = Space.xxl)) {
            SheetHeader("Options")

            if (userIsAdmin) {
                SheetAction(
                    glyph = GlyphKind.FLAG,
                    label = if (post.isPinned) "Désépingler" else "Épingler en tête",
                    onClick = { onPin(); close() }
                )
            }
            if (!isMyPost) {
                SheetAction(
                    glyph = GlyphKind.FLAG,
                    label = "Signaler",
                    subtitle = "L'administration recevra ton signalement.",
                    onClick = { onReport() }
                )
            }
            if (isMyPost || userIsAdmin) {
                SheetAction(
                    glyph = GlyphKind.CROSS,
                    label = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onDelete(); close() }
                )
            }
        }
    }
}

@Composable
private fun ReportSheet(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(bottom = Space.xxl)) {
            SheetHeader("Signaler", "Qu'est-ce qui ne va pas ?")
            REPORT_REASONS.forEach { reason ->
                SheetAction(glyph = GlyphKind.STAR, label = reason, onClick = onConfirm)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Sondage
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PollSection(post: Post, currentUid: String, onVote: (Int) -> Unit) {
    val hasVoted = currentUid in post.pollVoters
    val total = (post.pollVotes1 + post.pollVotes2).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf(
            Triple(1, post.pollOption1, post.pollVotes1),
            Triple(2, post.pollOption2, post.pollVotes2)
        ).forEach { (option, label, votes) ->
            val pct = if (hasVoted) votes.toFloat() / total else 0f
            val leading = hasVoted && pct > 0.5f

            TapArea(
                onTap = { if (!hasVoted) onVote(option) },
                enabled = !hasVoted,
                scaleDown = 0.98f,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (hasVoted && pct > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(pct)
                            .height(46.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (leading) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (hasVoted) {
                        Text(
                            "${(pct * 100).toInt()} %",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
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
