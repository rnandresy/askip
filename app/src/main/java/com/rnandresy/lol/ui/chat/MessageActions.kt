package com.rnandresy.lol.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.BubbleGloss
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.SheetAction
import com.rnandresy.lol.ui.components.SheetHeader
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.bubbleShell
import com.rnandresy.lol.ui.components.readableOn
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space

/**
 * Une réaction : ce qu'on écrit en base, et ce qu'on dessine.
 *
 * La clé est un mot, pas un signe. Un signe stocké tel quel dépend de son
 * encodage, ne se lit pas dans la console Firebase, et interdit de changer le
 * dessin sans réécrire toutes les réactions déjà posées.
 */
data class MessageReaction(
    val key: String,
    val glyph: GlyphKind,
    val label: String
)

/**
 * Les six réactions proposées.
 *
 * Six, pas plus : au-delà, la rangée ne tient plus sur un écran étroit et le
 * choix devient une corvée au lieu d'un réflexe.
 */
val MESSAGE_REACTIONS = listOf(
    MessageReaction("love", GlyphKind.HEART, "j'aime"),
    MessageReaction("fire", GlyphKind.FLAME, "ça chauffe"),
    MessageReaction("wow", GlyphKind.SPARKLE, "waouh"),
    MessageReaction("star", GlyphKind.STAR, "brillant"),
    MessageReaction("night", GlyphKind.MOON, "ça me touche"),
    MessageReaction("ok", GlyphKind.CHECK, "d'accord")
)

/** Retrouve une réaction depuis ce qui est stocké. */
fun reactionFor(key: String): MessageReaction? =
    MESSAGE_REACTIONS.firstOrNull { it.key == key }

/** Copie [text] dans le presse-papiers. */
fun copyToClipboard(context: Context, text: String) {
    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clip?.setPrimaryClip(ClipData.newPlainText("Askip", text))
}

/**
 * La feuille qui s'ouvre sur appui long d'un message.
 *
 * Elle porte d'abord la rangée de réactions — c'est le geste le plus fréquent,
 * il doit être le plus haut sous le pouce — puis répondre, copier, supprimer.
 *
 * [canDelete] et [copyable] gouvernent ce qui s'affiche : proposer « Copier »
 * sur une photo, ou « Supprimer » sur le message de quelqu'un d'autre, ne
 * mènerait qu'à un refus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
    myReaction: String?,
    canDelete: Boolean,
    copyable: Boolean,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(bottom = Space.xxl)) {
            SheetHeader("Ce message")

            ReactionRow(
                selected = myReaction,
                onPick = { onReact(it); onDismiss() },
                modifier = Modifier.padding(
                    horizontal = Space.xl,
                    vertical = Space.sm
                )
            )

            SheetAction(emoji = "↩", label = "Répondre") { onReply(); onDismiss() }

            if (copyable) {
                SheetAction(emoji = "⧉", label = "Copier le texte") {
                    onCopy(); onDismiss()
                }
            }

            if (canDelete) {
                SheetAction(
                    emoji = "✕",
                    label = "Supprimer",
                    tint = MaterialTheme.colorScheme.error
                ) { onDelete(); onDismiss() }
            }
        }
    }
}

/** La rangée de réactions. Celle qu'on a déjà posée porte un liseré d'accent. */
@Composable
fun ReactionRow(
    selected: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val cercle = androidx.compose.foundation.shape.CircleShape

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MESSAGE_REACTIONS.forEach { reaction ->
            val picked = selected == reaction.key
            TapArea(
                onTap = { tap(); onPick(reaction.key) },
                scaleDown = 0.85f,
                modifier = Modifier
                    .size(34.dp)
                    .bubbleShell(
                        cercle,
                        if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else palette.bubble,
                        if (picked) MaterialTheme.colorScheme.primary else palette.bubbleBorder,
                        palette.shadow,
                        if (picked) 5.dp else 2.dp
                    )
                    .semantics { contentDescription = reaction.label }
            ) {
                BubbleGloss(cercle, if (picked) 0.6f else 0.3f)
                AskipGlyph(
                    kind = reaction.glyph,
                    size = 16.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * Les réactions posées, sous la bulle.
 *
 * Chaque pastille est cliquable : elle repose ou retire la même réaction, donc
 * on peut se joindre à un emoji déjà là sans rouvrir la feuille.
 */
@Composable
fun ReactionStrip(
    counts: List<Pair<String, Int>>,
    mine: String?,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (counts.isEmpty()) return
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()
    val shape = RoundedCornerShape(Radius.pill)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        counts.forEach { (key, count) ->
            // Une réaction dont on ne connaît plus la clé — jeu de réactions
            // changé depuis — est ignorée plutôt que dessinée de travers.
            val reaction = reactionFor(key) ?: return@forEach
            val isMine = mine == key

            TapArea(
                onTap = { tap(); onToggle(key) },
                scaleDown = 0.88f,
                modifier = Modifier
                    .bubbleShell(
                        shape,
                        if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else palette.bubble,
                        if (isMine) MaterialTheme.colorScheme.primary else palette.bubbleBorder,
                        palette.shadow,
                        if (isMine) 4.dp else 2.dp
                    )
                    .semantics { contentDescription = "${reaction.label}, $count" }
            ) {
                BubbleGloss(shape, if (isMine) 0.6f else 0.3f)
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AskipGlyph(kind = reaction.glyph, size = 11.dp)
                    // Un seul auteur, c'est déjà dit par la figure : le « 1 »
                    // n'ajoute rien et alourdit la ligne.
                    if (count > 1) {
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * La citation, à l'intérieur de la bulle qui répond.
 *
 * Une barre verticale et deux lignes au plus : assez pour reconnaître le
 * message visé, pas assez pour voler la vedette à la réponse.
 */
@Composable
fun ReplyQuote(
    username: String,
    content: String,
    onSurface: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(onSurface.copy(alpha = 0.12f))
            .padding(horizontal = Space.sm, vertical = Space.xs)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(onSurface.copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(Space.sm))
        Column {
            Text(
                username.ifBlank { "Message" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = onSurface.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                content,
                style = MaterialTheme.typography.labelSmall,
                color = onSurface.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Le bandeau au-dessus du champ de saisie quand on répond.
 *
 * Sans lui, on ne saurait plus à quoi on répond au moment d'écrire — et une
 * citation partie par erreur ne se rattrape pas.
 */
@Composable
fun ReplyBanner(
    username: String,
    content: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(palette.bubble)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(
                "Réponse à ${username.ifBlank { "un message" }}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                content,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TapArea(
            onTap = { tap(); onCancel() },
            scaleDown = 0.85f,
            modifier = Modifier.size(26.dp)
        ) {
            Text(
                "✕",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/** Le relief commun à toutes les bulles de message. */
@Composable
fun messageBubbleShape(isMe: Boolean) = RoundedCornerShape(
    topStart = Radius.md,
    topEnd = Radius.md,
    bottomStart = if (isMe) Radius.md else 4.dp,
    bottomEnd = if (isMe) 4.dp else Radius.md
)

/** La couleur d'encre lisible sur une bulle de message. */
@Composable
fun messageInk(isMe: Boolean): Color =
    if (isMe) readableOn(MaterialTheme.colorScheme.primary)
    else MaterialTheme.colorScheme.onSurface
