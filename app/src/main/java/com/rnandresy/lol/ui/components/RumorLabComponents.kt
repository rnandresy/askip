@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Bet
import com.rnandresy.lol.model.ChainLink
import com.rnandresy.lol.model.MentionReply
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.LocalReduceMotion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.BET_BASE_REWARD
import com.rnandresy.lol.utils.BET_STAKES
import com.rnandresy.lol.utils.BET_TOKEN_EMOJI
import com.rnandresy.lol.utils.CHAIN_MAX_LENGTH
import com.rnandresy.lol.utils.CHAIN_MAX_LINKS
import com.rnandresy.lol.utils.RumorEngine
import com.rnandresy.lol.utils.SEAL_KEY_EMOJI
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════════════════
//  La Météo du campus
// ═════════════════════════════════════════════════════════════════════════════

/**
 * L'ambiance du campus sur 24 h, en une ligne.
 *
 * C'est un signal *partagé* : tout le monde voit la même météo au même moment,
 * ce qui donne à l'app une humeur du jour plutôt qu'un fil interchangeable.
 * Entièrement calculé en local — aucune lecture Firestore supplémentaire.
 */
@Composable
fun CampusWeatherBar(
    weather: RumorEngine.CampusWeather,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val reduceMotion = LocalReduceMotion.current
    val stormy = weather.weather == RumorEngine.Weather.STORMY ||
        weather.weather == RumorEngine.Weather.CHAOS

    // Le fond pulse doucement quand ça chauffe — jamais quand la personne
    // a demandé moins de mouvement.
    val transition = rememberInfiniteTransition(label = "weather")
    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = if (stormy && !reduceMotion) 0.20f else 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weatherPulse"
    )

    val accent = when (weather.weather) {
        RumorEngine.Weather.CHAOS -> palette.heatHigh
        RumorEngine.Weather.STORMY -> palette.heatLow
        RumorEngine.Weather.BREEZY -> palette.contested
        RumorEngine.Weather.CLEAR -> palette.confirmed
        RumorEngine.Weather.CALM -> palette.unknown
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = pulse), Color.Transparent)
                )
            )
            .padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Text(weather.weather.emoji, fontSize = 18.sp)
        Column(Modifier.weight(1f)) {
            Text(
                weather.weather.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                weather.weather.blurb,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "${weather.postsToday} rumeurs · ${weather.verdictsToday} votes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  La Capsule scellée
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Une rumeur qu'on ne peut pas encore lire.
 *
 * Deux façons de l'ouvrir : attendre l'heure, ou réunir assez de clés. Le
 * verrou est appliqué par les règles Firestore, pas ici : cet écran ne fait que
 * montrer un contenu que le serveur a bien voulu donner.
 */
@Composable
fun SealedCapsule(
    post: Post,
    currentUid: String,
    revealedContent: String?,
    onGiveKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val isAuthor = post.userId == currentUid
    val gaveKey = currentUid in post.keysBy

    // Recalculé chaque seconde : un compte à rebours figé ne donne aucune envie
    // de revenir.
    val remaining by produceState(post.msUntilUnseal(), post.sealedUntil) {
        while (value > 0) {
            delay(1000)
            value = post.msUntilUnseal()
        }
    }
    // `keysMissing()` vaut 0 quand aucune clé n'est demandée : sans la garde
    // sur `keysNeeded`, une capsule sans option de clé s'afficherait ouverte.
    val open = remaining <= 0L || (post.keysNeeded > 0 && post.keysMissing() == 0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (open) palette.confirmed.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Text(if (open) "⌂" else "⌂", fontSize = 20.sp)
                Text(
                    if (open) "Capsule ouverte" else "Capsule scellée",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (open) palette.confirmed else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (!open) {
                    Text(
                        formatCountdown(remaining),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.contested
                    )
                }
            }

            when {
                revealedContent != null -> {
                    Text(
                        revealedContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isAuthor && !open) {
                        Text(
                            "Toi seul peux la relire avant l'heure.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                open -> Text(
                    "Ouverture en cours…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> {
                    // Faux texte flouté : on montre qu'il y a quelque chose,
                    // sans que l'app ait jamais eu le contenu entre les mains.
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.alpha(0.35f)
                    ) {
                        RedactedBar(1f)
                        RedactedBar(0.82f)
                        RedactedBar(0.55f)
                    }
                }
            }

            if (!open && post.keysNeeded > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    ProgressTrack(
                        progress = post.keysBy.size.toFloat() / post.keysNeeded,
                        height = 4.dp,
                        color = palette.contested
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${post.keysBy.size}/${post.keysNeeded} clés — " +
                                "encore ${post.keysMissing()} pour forcer l'ouverture",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (!gaveKey && !isAuthor) {
                            TextButton(onClick = onGiveKey) {
                                Text(
                                    "$SEAL_KEY_EMOJI Ma clé",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (gaveKey) {
                            Text(
                                "$SEAL_KEY_EMOJI donnée",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.contested
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedactedBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(12.dp)
            .clip(RoundedCornerShape(Radius.xs))
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

/** j / h / min / s selon ce qui reste — on ne montre jamais « 0 j 0 h 4 min ». */
private fun formatCountdown(ms: Long): String {
    val totalSec = ms / 1000
    val days = totalSec / 86_400
    val hours = (totalSec % 86_400) / 3_600
    val minutes = (totalSec % 3_600) / 60
    val seconds = totalSec % 60
    return when {
        days > 0 -> "${days}j ${hours}h"
        hours > 0 -> "${hours}h ${minutes}min"
        minutes > 0 -> "${minutes}min ${seconds}s"
        else -> "${seconds}s"
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le Téléphone arabe
// ═════════════════════════════════════════════════════════════════════════════

/** Aperçu compact dans le fil : combien de mains, et la dernière phrase. */
@Composable
fun ChainPreview(post: Post, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    val full = post.chainCount >= CHAIN_MAX_LINKS

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text("⋯", fontSize = 12.sp)
                Text(
                    if (full) "CHAÎNE COMPLÈTE" else "TÉLÉPHONE ARABE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (full) palette.confirmed
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                ChainDots(post.chainCount)
            }
            if (post.chainLastContent.isNotBlank()) {
                Text(
                    "…${post.chainLastContent}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "dernier maillon : ${post.chainLastAuthor.ifBlank { "quelqu'un ◌" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Les sept emplacements de la chaîne, remplis au fur et à mesure. */
@Composable
private fun ChainDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(CHAIN_MAX_LINKS) { i ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < count) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

/**
 * La chaîne complète, en cascade.
 *
 * Chaque maillon est décalé d'un cran vers la droite : on *voit* la rumeur
 * s'éloigner de son point de départ, ce qui est exactement le propos.
 */
@Composable
fun ChainThread(
    origin: String,
    originAuthor: String,
    links: List<ChainLink>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        ChainBubble(
            index = 1,
            author = originAuthor.ifBlank { "Quelqu'un ◌" },
            content = origin,
            isOrigin = true
        )
        links.forEachIndexed { i, link ->
            ChainBubble(
                index = i + 2,
                author = link.username.ifBlank { "Quelqu'un ◌" },
                content = link.content,
                isOrigin = false
            )
        }
    }
}

@Composable
private fun ChainBubble(
    index: Int,
    author: String,
    content: String,
    isOrigin: Boolean
) {
    // Le décalage s'arrête à 4 crans : au-delà, la bulle deviendrait illisible
    // sur un petit écran.
    val indent = (index - 1).coerceAtMost(4) * 12
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(indent.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isOrigin) Radius.md else 2.dp,
                topEnd = Radius.md,
                bottomStart = Radius.md,
                bottomEnd = Radius.md
            ),
            color = if (isOrigin) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Text(
                        "#$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        author,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Le champ pour ajouter son maillon. Court exprès : une phrase, pas un roman. */
@Composable
fun AddChainLinkField(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= CHAIN_MAX_LENGTH) text = it },
            placeholder = { Text("Et ensuite ? Ajoute ta phrase…") },
            shape = RoundedCornerShape(Radius.md),
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${text.length}/$CHAIN_MAX_LENGTH · un seul maillon par personne",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BubbleButton(
                text = "Ajouter",
                onClick = { onSend(text); text = "" },
                enabled = text.isNotBlank(),
                tone = BubbleTone.PRIMARY,
                size = BubbleSize.SMALL
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le droit de réponse
// ═════════════════════════════════════════════════════════════════════════════

/**
 * La réponse de la personne citée, épinglée en tête de la rumeur.
 *
 * Aucune plateforme ne garantit cette place au sujet d'une publication : sa
 * version arrive toujours en commentaire, en bas, longtemps après. Ici elle
 * passe devant, et elle se voit.
 */
@Composable
fun RightOfReplyCard(reply: MentionReply, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = palette.confirmed.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, palette.confirmed.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text("⚖", fontSize = 12.sp)
                Text(
                    "DROIT DE RÉPONSE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = palette.confirmed
                )
                Spacer(Modifier.weight(1f))
                Text(
                    reply.username,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                reply.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Le bandeau proposé à la personne citée, avec son champ de réponse. */
@Composable
fun RightOfReplyPrompt(
    alreadyReplied: Boolean,
    onPublish: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = palette.contested.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, palette.contested.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚖", fontSize = 16.sp)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Cette rumeur te cite",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (alreadyReplied) "Ta réponse est épinglée en tête."
                        else "Tu as un droit de réponse, épinglé au-dessus de tout.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        if (alreadyReplied) onRemove() else expanded = !expanded
                    }
                ) {
                    Text(
                        when {
                            alreadyReplied -> "Retirer"
                            expanded -> "Annuler"
                            else -> "Répondre"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded && !alreadyReplied,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Ta version des faits…") },
                        shape = RoundedCornerShape(Radius.md),
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BubbleButton(
                        text = "Publier ma réponse",
                        onClick = {
                            onPublish(text)
                            text = ""
                            expanded = false
                        },
                        enabled = text.isNotBlank(),
                        tone = BubbleTone.PRIMARY,
                        fillWidth = true
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le Pari
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Miser des jetons sur le verdict à venir.
 *
 * La cote se calcule en direct : parier contre la foule, tôt, rapporte le plus.
 * On affiche le gain potentiel avant de valider, sinon le pari serait un tirage
 * au sort déguisé.
 */
@Composable
fun BetPanel(
    post: Post,
    myBet: Bet?,
    tokensLeft: Int,
    isMyPost: Boolean,
    onPlaceBet: (Boolean, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current

    if (myBet != null) {
        BetTicket(myBet, modifier)
        return
    }
    if (isMyPost) return

    var side by remember { mutableStateOf<Boolean?>(null) }
    var stake by remember { mutableStateOf(BET_STAKES.first()) }

    val ratio = side?.let {
        RumorEngine.sideRatio(post.credibleBy.size, post.fakeBy.size, it)
    } ?: 0.5f
    val odds = RumorEngine.betOdds(ratio, post.verdictVotes())
    val gain = (BET_BASE_REWARD * stake * odds).toLong()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(BET_TOKEN_EMOJI, fontSize = 14.sp)
                Spacer(Modifier.width(Space.xs))
                Text(
                    "PARIER SUR LE VERDICT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$tokensLeft jeton${if (tokensLeft > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (tokensLeft > 0) palette.scoop
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (tokensLeft <= 0) {
                Text(
                    "Plus de jetons aujourd'hui. Ils reviennent à minuit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                SideChoice(
                    label = "✓ Ce sera crédible",
                    selected = side == true,
                    accent = palette.confirmed,
                    modifier = Modifier.weight(1f),
                    onClick = { side = true }
                )
                SideChoice(
                    label = "✕ Ce sera bidon",
                    selected = side == false,
                    accent = palette.debunked,
                    modifier = Modifier.weight(1f),
                    onClick = { side = false }
                )
            }

            AnimatedVisibility(
                visible = side != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        Text(
                            "Mise",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BET_STAKES.filter { it <= tokensLeft }.forEach { s ->
                            StakeChip(
                                value = s,
                                selected = stake == s,
                                onClick = { stake = s }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Cote ×${"%.1f".format(odds)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.scoop
                            )
                            Text(
                                "gain potentiel : +$gain clout",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BubbleButton(
                            text = "Parier",
                            emoji = "◈",
                            onClick = { side?.let { onPlaceBet(it, stake) } },
                            enabled = stake <= tokensLeft,
                            tone = BubbleTone.PRIMARY,
                            size = BubbleSize.SMALL
                        )
                    }

                    Text(
                        "Plus tu paries tôt et à contre-courant, plus la cote monte.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SideChoice(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val shape = RoundedCornerShape(Radius.sm)

    TapArea(
        onTap = { tap(); onClick() },
        scaleDown = 0.94f,
        modifier = modifier.bubbleShell(
            shape,
            if (selected) accent.copy(alpha = 0.18f) else palette.bubble,
            if (selected) accent else palette.bubbleBorder,
            palette.shadow,
            if (selected) 5.dp else 2.dp
        )
    ) {
        BubbleGloss(shape, if (selected) 0.6f else 0.3f)
        Text(
            label,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Space.sm, vertical = Space.sm),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StakeChip(value: Int, selected: Boolean, onClick: () -> Unit) {
    BubbleChip(
        label = "×$value",
        emoji = BET_TOKEN_EMOJI,
        filled = selected,
        accent = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    )
}

/** Le ticket : ce sur quoi tu as misé, et ce que ça a donné. */
@Composable
fun BetTicket(bet: Bet, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    val accent = when {
        !bet.settled -> palette.scoop
        bet.won -> palette.confirmed
        else -> palette.debunked
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                when {
                    !bet.settled -> BET_TOKEN_EMOJI
                    bet.won -> "✦"
                    else -> "☠"
                },
                fontSize = 18.sp
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "${bet.sideEmoji()} Tu as parié « ${bet.sideLabel()} » " +
                        "· $BET_TOKEN_EMOJI×${bet.stake}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        !bet.settled ->
                            "cote ×${"%.1f".format(bet.odds)} · " +
                                "+${bet.potentialGain()} clout si tu as raison"
                        bet.won -> "Gagné : +${bet.payout} clout"
                        else -> "Perdu : ${bet.payout} clout"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!bet.settled) {
                Text(
                    "en cours",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }
    }
}

/** Pastille compacte, pour signaler un pari en cours depuis le fil. */
@Composable
fun BetBadge(bet: Bet, modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    val accent = when {
        !bet.settled -> palette.scoop
        bet.won -> palette.confirmed
        else -> palette.debunked
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.xs),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(bet.sideEmoji(), fontSize = 9.sp)
            Text(
                if (bet.settled) {
                    if (bet.won) "+${bet.payout}" else "${bet.payout}"
                } else {
                    "×${"%.1f".format(bet.odds)}"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                color = accent
            )
        }
    }
}
