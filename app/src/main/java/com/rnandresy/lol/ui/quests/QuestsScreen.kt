package com.rnandresy.lol.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.BetTokenGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.ProgressTrack
import com.rnandresy.lol.ui.components.StatPill
import com.rnandresy.lol.ui.components.StreakChip
import com.rnandresy.lol.ui.components.glyphForQuest
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.BETS_PER_DAY
import com.rnandresy.lol.utils.QuestDef
import com.rnandresy.lol.utils.QuestProgress
import com.rnandresy.lol.utils.RumorEngine
import com.rnandresy.lol.viewmodel.AskipViewModel

/**
 * Les missions du jour et la progression.
 *
 * Trois missions courtes qui se renouvellent à minuit, une barre de niveau,
 * et le Scoop du jour. C'est court exprès : une boucle qui se boucle en cinq
 * minutes donne envie de revenir demain, une liste interminable décourage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen(vm: AskipViewModel, onBack: () -> Unit) {
    val profile by vm.myProfile.collectAsState()
    val progress by vm.questProgress.collectAsState()
    val canScoop by vm.canScoopToday.collectAsState()
    val betTokens by vm.betTokensLeft.collectAsState()
    val quests = vm.todayQuests

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text("Missions du jour", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            item {
                ProgressCard(
                    level = profile?.level() ?: 1,
                    title = profile?.title() ?: "Nouveau",
                    progress = profile?.levelProgress() ?: 0f,
                    xpToNext = profile?.xpToNextLevel() ?: 0L,
                    streak = profile?.streak ?: 0,
                    bestStreak = profile?.bestStreak ?: 0,
                    clout = profile?.clout ?: 0L
                )
            }

            item { ScoopCard(available = canScoop) }

            item { BetTokensCard(left = betTokens, profile = profile) }

            item {
                Text(
                    "AUJOURD'HUI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.sm)
                )
            }

            items(quests, key = { it.id }) { quest ->
                QuestRow(
                    quest = quest,
                    progress = progress,
                    onClaim = { vm.claimQuest(quest) }
                )
            }

            item {
                Text(
                    "Les missions changent à minuit. Elles sont les mêmes pour tout le campus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.md)
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(
    level: Int,
    title: String,
    progress: Float,
    xpToNext: Long,
    streak: Int,
    bestStreak: Int,
    clout: Long
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Niveau $level",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StreakChip(streak)
            }

            ProgressTrack(progress = progress)
            Text(
                "$xpToNext XP avant le niveau ${level + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPill("$clout", "Clout")
                StatPill("$streak", "Série")
                StatPill("$bestStreak", "Record")
            }
        }
    }
}

@Composable
private fun ScoopCard(available: Boolean) {
    val palette = LocalAskipPalette.current
    Surface(
        color = if (available) palette.scoopBg else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(Radius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            AskipGlyph(kind = GlyphKind.GEM, size = 25.dp, tint = palette.scoop)
            Column(Modifier.weight(1f)) {
                Text(
                    if (available) "Ton Scoop du jour t'attend" else "Scoop déjà offert",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (available) palette.scoop else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (available) {
                        "Un seul par jour. Garde-le pour la rumeur qui le mérite."
                    } else {
                        "Il revient dans ${hoursUntilMidnight()} h."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun hoursUntilMidnight(): Int =
    ((RumorEngine.msUntilScoopReset() / 3_600_000L).toInt() + 1).coerceAtLeast(1)

/**
 * Les jetons de pari du jour et le bilan.
 *
 * Le jeton n'est pas la récompense — c'est le droit de parier. La récompense,
 * c'est le clout. C'est ce qui permet à quelqu'un qui vient d'arriver de jouer
 * dès le premier jour, sans réputation à mettre en jeu.
 */
@Composable
private fun BetTokensCard(left: Int, profile: UserProfile?) {
    val palette = LocalAskipPalette.current
    val won = profile?.betsWon ?: 0
    val lost = profile?.betsLost ?: 0
    val accuracy = profile?.betAccuracy() ?: 0f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AskipGlyph(kind = BetTokenGlyph, size = 22.dp, tint = palette.scoop)
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        "$left / $BETS_PER_DAY jetons de pari",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (left > 0) palette.scoop
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (left > 0) "Mise sur le verdict d'une rumeur avant qu'il tombe."
                        else "Tout dépensé. Ils reviennent dans ${hoursUntilMidnight()} h.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (won + lost > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill("$won", "Gagnés", accent = palette.confirmed)
                    StatPill("$lost", "Perdus", accent = palette.debunked)
                    StatPill("${(accuracy * 100).toInt()} %", "Flair")
                }
            }
        }
    }
}

@Composable
private fun QuestRow(
    quest: QuestDef,
    progress: QuestProgress,
    onClaim: () -> Unit
) {
    val count = progress.countOf(quest.id).coerceAtMost(quest.target)
    val done = progress.isDone(quest)
    val claimed = progress.isClaimed(quest)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            AskipGlyph(kind = glyphForQuest(quest.kind), size = 22.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (claimed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                ProgressTrack(
                    progress = progress.progressOf(quest),
                    height = 4.dp,
                    color = if (done) LocalAskipPalette.current.confirmed
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    "$count / ${quest.target}  ·  +${quest.xp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(Space.xs))

            when {
                claimed -> AskipGlyph(
                    kind = GlyphKind.CHECK,
                    size = 19.dp,
                    tint = LocalAskipPalette.current.confirmed
                )
                done -> BubbleButton(
                    text = "Encaisser",
                    onClick = onClaim,
                    tone = BubbleTone.PRIMARY,
                    size = BubbleSize.SMALL
                )
                else -> Text(
                    "${quest.target - count}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
