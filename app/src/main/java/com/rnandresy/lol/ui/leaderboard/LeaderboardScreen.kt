@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.SegmentedTabs
import com.rnandresy.lol.ui.components.glyphForRank
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.components.StreakChip
import com.rnandresy.lol.ui.components.VerdictChip
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

/**
 * Le classement du campus.
 *
 * Trois façons d'être bon, volontairement : le clout récompense les rumeurs
 * qu'on croit, l'XP récompense la présence, la série récompense la régularité.
 * Personne n'est premier partout, donc tout le monde a un classement à viser.
 */
private enum class Board(val label: String, val glyph: GlyphKind) {
    CLOUT("Informateurs", GlyphKind.SPARKLE),
    ORACLES("Oracles", GlyphKind.GEM),
    XP("Niveaux", GlyphKind.CHART),
    STREAK("Séries", GlyphKind.FLAME),
    LEGENDS("Rumeurs cultes", GlyphKind.CROWN)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    vm: AskipViewModel,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onBack: () -> Unit
) {
    var board by remember { mutableStateOf(Board.CLOUT) }

    val clout by vm.topClout.collectAsState()
    val xp by vm.topXp.collectAsState()
    val streak by vm.topStreak.collectAsState()
    val legends by vm.legendPosts.collectAsState()

    // Le flair se mesure sur la durée : en dessous de cinq paris réglés, un
    // « 100 % » ne veut rien dire, donc on n'affiche pas la personne.
    val oracles = remember(clout, xp) {
        (clout + xp).distinctBy { it.userId }
            .filter { it.betsWon + it.betsLost >= 5 }
            .sortedWith(
                compareByDescending<UserProfile> { it.betAccuracy() }
                    .thenByDescending { it.betsWon }
            )
    }

    LaunchedEffect(Unit) { vm.loadLeaderboards() }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text("Classement", fontWeight = FontWeight.Bold) },
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
        Column(Modifier.padding(pad)) {
            SegmentedTabs(
                items = Board.entries.toList(),
                selected = board,
                labelOf = { it.label },
                glyphOf = { it.glyph },
                onSelect = { board = it },
                modifier = Modifier.padding(vertical = Space.sm)
            )

            when (board) {
                Board.LEGENDS -> LegendList(legends, onOpenPost)
                else -> {
                    val people = when (board) {
                        Board.CLOUT -> clout
                        Board.XP -> xp
                        // Les oracles se classent au flair, pas au volume :
                        // on écarte ceux qui n'ont pas assez parié pour que le
                        // pourcentage veuille dire quelque chose.
                        Board.ORACLES -> oracles
                        else -> streak
                    }
                    PeopleList(people, board, onOpenProfile, vm.currentUserId)
                }
            }
        }
    }
}

@Composable
private fun PeopleList(
    people: List<UserProfile>,
    board: Board,
    onOpenProfile: (String) -> Unit,
    currentUid: String
) {
    if (people.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            if (board == Board.ORACLES) {
                EmptyState(
                    GlyphKind.GEM,
                    "Aucun oracle pour l'instant",
                    "Il faut 5 paris réglés pour figurer ici."
                )
            } else {
                EmptyState(GlyphKind.CHART, "Classement vide", "Il se remplira dès que ça bougera.")
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = Space.lg,
            end = Space.lg,
            top = Space.sm,
            bottom = Space.huge
        ),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        itemsIndexed(people, key = { _, p -> p.userId }) { index, profile ->
            val value = when (board) {
                Board.CLOUT -> "${profile.clout}"
                Board.XP -> "Nv. ${profile.level()}"
                Board.ORACLES -> "${(profile.betAccuracy() * 100).toInt()} %"
                else -> "${profile.streak} j"
            }
            LeaderRow(
                rank = index + 1,
                profile = profile,
                trailing = value,
                highlighted = profile.userId == currentUid,
                showStreak = board == Board.STREAK,
                onClick = { onOpenProfile(profile.userId) }
            )
        }
    }
}

@Composable
private fun LeaderRow(
    rank: Int,
    profile: UserProfile,
    trailing: String,
    highlighted: Boolean,
    showStreak: Boolean,
    onClick: () -> Unit
) {
    val userIsAdmin = isAdmin(profile.userId) || profile.isAdmin
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    // Sa propre ligne se détache par le remplissage : dans une liste de
    // trente, la retrouver ne doit pas demander de lire les pseudos.
    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        fill = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else palette.bubble,
        elevation = if (highlighted) 5.dp else 2.dp,
        gloss = if (highlighted) 0.5f else 0.28f,
        onClick = { tap(); onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankMedal(rank)
            Spacer(Modifier.width(Space.md))
            AskipAvatar(
                username = profile.username,
                photoUrl = profile.photoUrl,
                size = 40.dp,
                isAdminUser = userIsAdmin
            )
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Text(
                        profile.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (userIsAdmin) AdminGold
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showStreak) StreakChip(profile.streak)
                }
                Text(
                    profile.title(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(Space.sm))
            Text(
                trailing,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Or, argent, bronze pour le podium ; simple numéro ensuite. */
@Composable
private fun RankMedal(rank: Int) {
    val medal = if (rank in 1..3) glyphForRank(rank) else null
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        if (medal != null) {
            AskipGlyph(kind = medal, size = 19.dp)
        } else {
            Text(
                "$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendList(posts: List<Post>, onOpenPost: (String) -> Unit) {
    if (posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            EmptyState(GlyphKind.FLAME, "Pas encore de légende", "Les rumeurs cultes atterrissent ici.")
        }
        return
    }
    val palette = LocalAskipPalette.current

    LazyColumn(
        contentPadding = PaddingValues(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        itemsIndexed(posts, key = { _, p -> p.id }) { index, post ->
            BubbleCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenPost(post.id) }
            ) {
                Column(
                    modifier = Modifier.padding(Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        RankMedal(index + 1)
                        Text(
                            post.username.ifBlank { "Quelqu'un" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        VerdictChip(post.verdict())
                    }
                    Text(
                        post.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        MiniStat(GlyphKind.BUBBLE, post.commentCount)
                        MiniStat(GlyphKind.GEM, post.scoopBy.size, palette.scoop)
                        MiniStat(GlyphKind.HEART, post.totalReactions())
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(
    glyph: GlyphKind,
    value: Int,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        AskipGlyph(kind = glyph, size = 11.dp, tint = tint)
        Text(
            "$value",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}
