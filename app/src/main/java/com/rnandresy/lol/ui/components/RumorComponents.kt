@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.model.Verdict
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.LocalHapticsEnabled
import com.rnandresy.lol.ui.theme.LocalReduceMotion
import com.rnandresy.lol.ui.theme.Motion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.REACTIONS
import com.rnandresy.lol.utils.REACTION_LABELS
import com.rnandresy.lol.utils.RumorEngine
import com.rnandresy.lol.utils.SCOOP_EMOJI
import com.rnandresy.lol.utils.VERDICT_MIN_VOTES
import com.rnandresy.lol.utils.tagDef

// ═════════════════════════════════════════════════════════════════════════════
//  Confort
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Retour tactile respectant le réglage « Vibrations ».
 * Tous les gestes de l'app passent par ici, donc couper l'option les coupe tous.
 */
@Composable
fun rememberTapFeedback(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptic, enabled) {
        { if (enabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
}

/**
 * Rebond d'un élément qu'on vient d'activer.
 * Ne fait rien quand « Réduire les animations » est actif : le mouvement est
 * un plaisir pour la plupart, une gêne réelle pour certaines personnes.
 */
@Composable
private fun rememberPop(active: Boolean): Animatable<Float, *> {
    val scale = remember { Animatable(1f) }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(active, reduceMotion) {
        if (active && !reduceMotion) {
            scale.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
    }
    return scale
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le Rumeur-mètre
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le cœur de l'app : chaque rumeur se fait juger.
 *
 * Deux boutons, une barre. Tant qu'il y a trop peu de votes on affiche
 * « enquête en cours » sans barre — annoncer un verdict sur trois votes
 * serait mensonger, et c'est justement ce qu'on veut éviter dans une app
 * de rumeurs.
 */
@Composable
fun RumorMeter(
    post: Post,
    currentUid: String,
    onVote: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalAskipPalette.current
    val myVote = post.myVerdictVote(currentUid)
    val votes = post.verdictVotes()
    val settled = votes >= VERDICT_MIN_VOTES
    val ratio by animateFloatAsState(
        targetValue = if (settled) post.credibilityRatio() else 0.5f,
        animationSpec = tween(Motion.SLOW, easing = FastOutSlowInEasing),
        label = "credibility"
    )
    val isMine = post.userId == currentUid

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            VerdictVoteButton(
                label = "Crédible",
                emoji = "🧐",
                count = post.credibleBy.size,
                selected = myVote == true,
                accent = palette.confirmed,
                enabled = enabled && !isMine,
                modifier = Modifier.weight(1f),
                onClick = { onVote(true) }
            )
            VerdictVoteButton(
                label = "Bidon",
                emoji = "🚫",
                count = post.fakeBy.size,
                selected = myVote == false,
                accent = palette.debunked,
                enabled = enabled && !isMine,
                modifier = Modifier.weight(1f),
                onClick = { onVote(false) }
            )
        }

        if (settled) {
            // La barre : part verte à gauche, part rouge à droite.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(palette.debunked.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .fillMaxSize()
                        .background(palette.confirmed)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${(ratio * 100).toInt()} % y croient",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$votes votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                if (isMine) {
                    "Enquête en cours — ${VERDICT_MIN_VOTES - votes} votes avant le verdict"
                } else {
                    "Enquête en cours — ton avis compte (${VERDICT_MIN_VOTES - votes} restants)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VerdictVoteButton(
    label: String,
    emoji: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val shape = RoundedCornerShape(Radius.sm)

    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.18f) else palette.bubble,
        tween(Motion.NORMAL),
        label = "voteBg"
    )
    val border by animateColorAsState(
        if (selected) accent else palette.bubbleBorder,
        tween(Motion.NORMAL),
        label = "voteBorder"
    )

    TapArea(
        onTap = {
            tap()
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .bubbleShell(shape, bg, border, palette.shadow, if (selected) 5.dp else 2.dp)
            .semantics { contentDescription = "$label, $count votes" }
    ) {
        BubbleGloss(shape, if (selected) 0.6f else 0.3f)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Space.md, vertical = Space.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Spacer(Modifier.width(Space.xs))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (count > 0) {
                Spacer(Modifier.width(Space.xs))
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** L'étiquette de verdict, posée en tête de carte. */
@Composable
fun VerdictChip(verdict: Verdict, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    val color = when (verdict) {
        Verdict.CONFIRMED -> palette.confirmed
        Verdict.DEBUNKED -> palette.debunked
        Verdict.CONTESTED -> palette.contested
        Verdict.INVESTIGATING -> palette.unknown
    }
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier.bubbleShell(
            shape,
            color.copy(alpha = 0.16f),
            color.copy(alpha = 0.55f),
            palette.shadow,
            2.dp
        )
    ) {
        BubbleGloss(shape, 0.4f)
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text(verdict.emoji, fontSize = 10.sp)
            Text(
                verdict.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = color
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le Scoop
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Un Scoop par jour, pas plus. La rareté est le seul mécanisme qui rend un vote
 * signifiant : si on pouvait en donner à volonté, ça ne vaudrait rien.
 */
@Composable
fun ScoopButton(
    given: Boolean,
    count: Int,
    available: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val scale = rememberPop(given)

    val active = given || count > 0
    val usable = !given && available
    val shape = RoundedCornerShape(Radius.pill)

    TapArea(
        onTap = {
            tap()
            onClick()
        },
        enabled = usable,
        scaleDown = 0.9f,
        modifier = modifier
            // Un Scoop déjà donné garde son relief : c'est une décision qu'on
            // a prise, pas un bouton grisé. Seul l'indisponible s'efface.
            .alpha(if (given || available) 1f else 0.45f)
            .bubbleShell(
                shape,
                if (given) palette.scoopBg else palette.bubble,
                if (given) palette.scoop else palette.bubbleBorder,
                palette.shadow,
                if (given) 5.dp else 2.dp
            )
    ) {
        BubbleGloss(shape, if (given) 0.6f else 0.3f)
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text(
                SCOOP_EMOJI,
                fontSize = 14.sp,
                modifier = Modifier.scale(scale.value)
            )
            if (count > 0) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (active) palette.scoop
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Réactions
// ═════════════════════════════════════════════════════════════════════════════

/**
 * La barre de réactions. Chaque appui donne un petit rebond et une vibration
 * courte : c'est ce qui rend le geste satisfaisant et donne envie de le refaire.
 */
@Composable
fun ReactionBar(
    post: Post,
    currentUid: String,
    onReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val myReaction = post.getUserReaction(currentUid)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.xxs)
    ) {
        REACTIONS.forEach { emoji ->
            ReactionButton(
                emoji = emoji,
                count = post.reactionCount(emoji),
                isActive = myReaction == emoji,
                onClick = { onReaction(emoji) }
            )
        }
    }
}

@Composable
fun ReactionButton(
    emoji: String,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val scale = rememberPop(isActive)
    val shape = RoundedCornerShape(Radius.pill)

    TapArea(
        onTap = {
            tap()
            onClick()
        },
        scaleDown = 0.88f,
        modifier = Modifier
            .bubbleShell(
                shape,
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else palette.bubble,
                if (isActive) MaterialTheme.colorScheme.primary else palette.bubbleBorder,
                palette.shadow,
                if (isActive) 4.dp else 2.dp
            )
            .semantics {
                contentDescription = "${REACTION_LABELS[emoji] ?: emoji}, $count"
            }
    ) {
        BubbleGloss(shape, if (isActive) 0.6f else 0.3f)
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xxs)
        ) {
            Text(emoji, fontSize = 14.sp, modifier = Modifier.scale(scale.value))
            if (count > 0) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Tags
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TagChip(
    slug: String,
    modifier: Modifier = Modifier,
    count: Int = 0,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val def = tagDef(slug)
    val label = def?.label ?: "#$slug"
    val emoji = def?.emoji ?: "#"
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val shape = RoundedCornerShape(Radius.pill)

    val shell = modifier.bubbleShell(
        shape,
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else palette.bubble,
        if (selected) MaterialTheme.colorScheme.primary else palette.bubbleBorder,
        palette.shadow,
        if (selected) 4.dp else 2.dp
    )

    // Un salon sans action reste une étiquette : pas de ressort au doigt sur
    // quelque chose qui ne mène nulle part.
    val body: @Composable BoxScope.() -> Unit = {
        BubbleGloss(shape, if (selected) 0.6f else 0.3f)
        Row(
            modifier = Modifier.padding(horizontal = Space.md, vertical = Space.xs + 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text(emoji, fontSize = 11.sp)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (count > 0) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (onClick != null) {
        TapArea(
            onTap = { tap(); onClick() },
            scaleDown = 0.94f,
            modifier = shell,
            content = body
        )
    } else {
        Box(modifier = shell, content = body)
    }
}

/** La bande « ça circule » : ce dont le campus parle en ce moment. */
@Composable
fun TrendingBar(
    tags: List<RumorEngine.TrendingTag>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Row(
            modifier = Modifier.padding(horizontal = Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text("📈", fontSize = 12.sp)
            Text(
                "ÇA CIRCULE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.lg),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            items(tags, key = { it.slug }) { tag ->
                TagChip(
                    slug = tag.slug,
                    count = tag.count,
                    onClick = { onTagClick(tag.slug) }
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Onglets du fil
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Onglets défilants, pour les jeux trop nombreux pour tenir sur une ligne —
 * là où [SlidingSegmented] s'arrête.
 *
 * Ce sont des puces-bulles : celle qui est active se remplit, les autres
 * gardent le fond de bulle et leur liseré. C'est le même relief que les
 * boutons, donc on les reconnaît comme cliquables sans avoir à l'apprendre.
 */
@Composable
fun <T> SegmentedTabs(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    emojiOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = item == selected
            BubbleChip(
                label = labelOf(item),
                emoji = emojiOf(item),
                filled = isSelected,
                accent = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onSelect(item) }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Progression
// ═════════════════════════════════════════════════════════════════════════════

/** Anneau de niveau autour de l'avatar, rempli selon la progression. */
@Composable
fun LevelBadge(profile: UserProfile, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        profile.levelProgress(),
        tween(Motion.SLOW),
        label = "levelProgress"
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            BubbleChip(
                label = "Nv. ${profile.level()}",
                accent = MaterialTheme.colorScheme.primary
            )
            Text(
                profile.title(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${profile.xpToNextLevel()} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        ProgressTrack(progress = progress)
    }
}

/** Barre de progression maison — pas d'API dépréciée, look constant. */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Radius.pill))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .clip(RoundedCornerShape(Radius.pill))
                .background(color)
        )
    }
}

/** La série de jours actifs. La flamme grossit avec la série. */
@Composable
fun StreakChip(streak: Int, modifier: Modifier = Modifier) {
    if (streak <= 0) return
    val palette = LocalAskipPalette.current
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier.bubbleShell(
            shape,
            palette.streak.copy(alpha = 0.16f),
            palette.streak.copy(alpha = 0.55f),
            palette.shadow,
            2.dp
        )
    ) {
        BubbleGloss(shape, 0.4f)
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text("🔥", fontSize = if (streak >= 30) 14.sp else 11.sp)
            Text(
                "$streak j",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.streak
            )
        }
    }
}

/** Petite statistique affichée sur un profil. */
@Composable
fun StatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Chargement
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Squelette animé pendant le chargement du fil.
 * Un écran vide donne l'impression que l'app est cassée ; un squelette dit
 * « ça arrive » et fait paraître l'attente plus courte.
 */
@Composable
fun PostSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(base))
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                SkeletonBar(width = 120.dp, color = base)
                SkeletonBar(width = 70.dp, height = 8.dp, color = base)
            }
        }
        SkeletonBar(width = 280.dp, color = base)
        SkeletonBar(width = 210.dp, color = base)
    }
}

@Composable
private fun SkeletonBar(width: Dp, color: Color, height: Dp = 11.dp) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(Radius.xs))
            .background(color)
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  Célébration
// ═════════════════════════════════════════════════════════════════════════════

/** Bandeau de montée de niveau. Court, discret, et il se referme tout seul. */
@Composable
fun LevelUpBanner(
    level: Int?,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(level) {
        if (level != null) {
            kotlinx.coroutines.delay(3200)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = level != null,
        enter = fadeIn(tween(Motion.NORMAL)) + expandVertically(),
        exit = fadeOut(tween(Motion.FAST)) + shrinkVertically(),
        modifier = modifier
    ) {
        BubbleCard(
            modifier = Modifier.fillMaxWidth().padding(Space.md),
            fill = MaterialTheme.colorScheme.primaryContainer,
            elevation = 8.dp,
            gloss = 0.7f,
            onClick = onDismiss
        ) {
            Row(
                modifier = Modifier.padding(Space.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                Text("🎉", fontSize = 26.sp)
                Column {
                    Text(
                        "Niveau $level atteint",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Te voilà $title",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/** Le sujet du jour, en tête de fil. Une raison d'ouvrir l'app le matin. */
@Composable
fun DailyPromptCard(
    prompt: String,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    BubbleCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm),
        onClick = onWrite
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.heatHigh.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text("☀️", fontSize = 12.sp)
                Text(
                    "LE SUJET DU JOUR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                prompt,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Appuie pour répondre →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Pastille « ça chauffe » sur les rumeurs les plus brûlantes du moment. */
@Composable
fun HotBadge(rank: Int, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier.bubbleShell(
            shape,
            palette.heatHigh.copy(alpha = 0.20f),
            palette.heatHigh.copy(alpha = 0.5f),
            palette.shadow,
            3.dp
        )
    ) {
        BubbleGloss(shape, 0.5f)
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.heatHigh.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = Space.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xxs)
        ) {
            Text("🔥", fontSize = 10.sp)
            Text(
                "#$rank",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                color = palette.heatHigh
            )
        }
    }
}
