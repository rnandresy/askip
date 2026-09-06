@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.LocalReduceMotion
import com.rnandresy.lol.ui.theme.Motion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.FeedSection
import com.rnandresy.lol.utils.OATH_TEXT
import com.rnandresy.lol.utils.PERJURY_STAMP
import com.rnandresy.lol.utils.RumorEngine

// ═════════════════════════════════════════════════════════════════════════════
//  Le sélecteur des trois actualités
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Trois espaces, pas trois tris.
 *
 * Le sélecteur est plus haut et plus large que les onglets de tri en dessous :
 * c'est une hiérarchie visuelle, pas un caprice. On change de lieu ici, on
 * réordonne le même lieu en dessous.
 */
@Composable
fun FeedSectionBar(
    selected: FeedSection,
    onSelect: (FeedSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val tap = rememberTapFeedback()
    val palette = LocalAskipPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        FeedSection.entries.forEach { section ->
            val isSelected = section == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surfaceVariant,
                tween(Motion.NORMAL),
                label = "sectionBg"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.background
                else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(Motion.NORMAL),
                label = "sectionFg"
            )
            val shape = RoundedCornerShape(Radius.md)
            TapArea(
                onTap = {
                    if (!isSelected) tap()
                    onSelect(section)
                },
                scaleDown = 0.96f,
                modifier = Modifier
                    .weight(1f)
                    .bubbleShell(
                        shape,
                        bg,
                        if (isSelected) bg else palette.bubbleBorder,
                        palette.shadow,
                        if (isSelected) 6.dp else 2.dp
                    )
            ) {
                BubbleGloss(shape, if (isSelected) 0.7f else 0.3f)
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = Space.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    AskipGlyph(kind = glyphForSection(section), size = 15.dp, tint = fg)
                    Text(
                        section.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  La Page de Vérité
// ═════════════════════════════════════════════════════════════════════════════

/**
 * L'en-tête du registre.
 *
 * Il se tient au sérieux du début à la fin : majuscules, sceau, vocabulaire de
 * greffier. Et il affiche, sans le moindre commentaire, la part des serments
 * que le campus a démentis. C'est le chiffre qui fait la blague — l'app ne se
 * moque de personne, elle se contente de tenir les comptes.
 */
@Composable
fun TruthPageHeader(
    ledger: RumorEngine.TruthLedger,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val rate by animateFloatAsState(
        ledger.perjuryRate,
        tween(Motion.SLOW),
        label = "perjuryRate"
    )

    BubbleCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AskipGlyph(kind = GlyphKind.SCALE, size = 25.dp)
            Text(
                "LA PAGE DE VÉRITÉ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Toute publication est faite sous serment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (ledger.sworn > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "TAUX DE PARJURE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(rate * 100).toInt()} %",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (rate >= 0.5f) palette.debunked else palette.confirmed
                        )
                    }
                    ProgressTrack(
                        progress = rate,
                        height = 6.dp,
                        color = palette.debunked
                    )
                    Text(
                        "${ledger.perjuries} parjure(s) et ${ledger.upheld} serment(s) " +
                            "tenu(s) sur ${ledger.judged} jugé(s) — ${ledger.sworn} au registre.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                ledger.statement,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Le tampon apposé sur un serment jugé.
 *
 * Incliné comme un vrai tampon d'huissier : c'est un détail, mais c'est lui qui
 * fait basculer la page du sérieux au comique.
 */
@Composable
fun OathStamp(perjury: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    val color = if (perjury) palette.debunked else palette.confirmed

    Box(
        modifier = modifier
            .rotate(if (perjury) -8f else -4f)
            .border(2.dp, color.copy(alpha = 0.7f), RoundedCornerShape(Radius.xs))
            .padding(horizontal = Space.sm, vertical = 2.dp)
    ) {
        Text(
            if (perjury) PERJURY_STAMP else "SERMENT TENU",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            fontSize = 10.sp,
            color = color
        )
    }
}

/** La ligne « publié sous serment » en tête d'une rumeur de la Page de Vérité. */
@Composable
fun OathLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        AskipGlyph(kind = GlyphKind.CHECK, size = 10.dp)
        Text(
            "PUBLIÉ SOUS SERMENT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Le serment, à prêter avant de publier.
 *
 * Il faut cocher pour continuer. Ce petit geste supplémentaire est tout
 * l'intérêt : il engage, et il rend le démenti savoureux.
 */
@Composable
fun OathDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var sworn by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AskipGlyph(kind = GlyphKind.SCALE, size = 27.dp) },
        title = {
            Text(
                "Serment",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text(
                    OATH_TEXT,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                TapArea(
                    onTap = { sworn = !sworn },
                    scaleDown = 0.99f
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = Space.sm)
                    ) {
                        Checkbox(checked = sworn, onCheckedChange = { sworn = it })
                        Text(
                            "Je le jure",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "Le campus tranchera. Un serment démenti est enregistré " +
                        "comme parjure, et compte dans le taux de la page.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = sworn) {
                Text("Publier sous serment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  L'actualité vocale
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le composeur vocal : un bouton, pas un champ.
 *
 * On appuie pour parler, on rappuie pour envoyer. Pas de maintien du doigt —
 * tenir le bouton pendant deux minutes est intenable, et l'appui maintenu
 * empêche de relire l'écran pendant qu'on parle.
 */
@Composable
fun VoiceComposer(
    isRecording: Boolean,
    seconds: Int,
    maxSeconds: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    idleLabel: String = "Appuie pour parler"
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val reduceMotion = LocalReduceMotion.current

    // Au-delà de la limite, on envoie ce qui est déjà enregistré plutôt que de
    // tout jeter : perdre sa prise à la dernière seconde serait cruel.
    LaunchedEffect(seconds, isRecording) {
        if (isRecording && seconds >= maxSeconds) onStop()
    }

    val transition = rememberInfiniteTransition(label = "rec")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !reduceMotion) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulse"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        if (isRecording) {
            BubbleIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Annuler l'enregistrement",
                onClick = { tap(); onCancel() },
                tone = BubbleTone.DANGER
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Text(
                if (isRecording) formatSeconds(seconds) else idleLabel,
                style = if (isRecording) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                color = if (isRecording) palette.debunked
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isRecording) {
                ProgressTrack(
                    progress = seconds.toFloat() / maxSeconds,
                    height = 3.dp,
                    color = palette.debunked
                )
                Text(
                    "max ${formatSeconds(maxSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val fill = if (isRecording) palette.debunked else MaterialTheme.colorScheme.primary
        TapArea(
            onTap = {
                tap()
                if (isRecording) onStop() else onStart()
            },
            scaleDown = 0.9f,
            modifier = Modifier
                .size(52.dp)
                .scale(if (isRecording) pulse else 1f)
                .bubbleShell(CircleShape, fill, fill, palette.shadow, 6.dp)
        ) {
            BubbleGloss(CircleShape, 1f)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    if (isRecording) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                    if (isRecording) "Envoyer" else "Enregistrer",
                    tint = readableOn(fill)
                )
            }
        }
    }
}

private fun formatSeconds(s: Int) = "%d:%02d".format(s / 60, s % 60)

/**
 * Le bloc central d'une rumeur vocale.
 *
 * Le lecteur occupe toute la largeur : dans cette actualité, la voix n'est pas
 * une pièce jointe sous un texte, elle *est* la publication.
 */
@Composable
fun VoiceHero(
    url: String,
    duration: Int,
    modifier: Modifier = Modifier
) {
    BubbleCard(modifier = modifier.fillMaxWidth(), elevation = 3.dp) {
        Column {
            Row(
                modifier = Modifier.padding(
                    start = Space.md,
                    end = Space.md,
                    top = Space.sm
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                AskipGlyph(kind = GlyphKind.WAVE, size = 11.dp)
                Text(
                    "RUMEUR VOCALE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatSeconds(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AskipAudioPlayer(url = url, duration = duration, isMe = false)
        }
    }
}

/** Bandeau explicatif en tête d'une actualité qui a ses propres règles. */
@Composable
fun SectionIntro(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        Text(emoji, fontSize = 18.sp)
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Cercle d'enregistrement flottant, pour publier une rumeur vocale. */
@Composable
fun VoiceRecordFab(
    isRecording: Boolean,
    seconds: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        if (isRecording) {
            val pillShape = RoundedCornerShape(Radius.pill)
            TapArea(
                onTap = { tap(); onCancel() },
                scaleDown = 0.94f,
                modifier = Modifier.bubbleShell(
                    pillShape,
                    palette.bubble,
                    palette.debunked.copy(alpha = 0.5f),
                    palette.shadow,
                    3.dp
                )
            ) {
                BubbleGloss(pillShape, 0.4f)
                Row(
                    modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Annuler",
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        formatSeconds(seconds),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.debunked
                    )
                }
            }
        }
        val fabFill = if (isRecording) palette.debunked else MaterialTheme.colorScheme.primary
        TapArea(
            onTap = {
                tap()
                if (isRecording) onStop() else onStart()
            },
            scaleDown = 0.9f,
            modifier = Modifier
                .size(56.dp)
                .bubbleShell(CircleShape, fabFill, fabFill, palette.shadow, 8.dp)
        ) {
            BubbleGloss(CircleShape, 1f)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    if (isRecording) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                    if (isRecording) "Publier la rumeur vocale" else "Enregistrer une rumeur",
                    tint = readableOn(fabFill)
                )
            }
        }
    }
}

/** Petite pastille « vocal » sur une réponse audio. */
@Composable
fun VoiceCommentBubble(
    url: String,
    duration: Int,
    modifier: Modifier = Modifier
) {
    BubbleCard(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        elevation = 2.dp,
        gloss = 0.3f
    ) {
        AskipAudioPlayer(url = url, duration = duration, isMe = false)
    }
}
