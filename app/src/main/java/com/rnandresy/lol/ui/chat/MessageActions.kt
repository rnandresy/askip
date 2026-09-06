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
import com.rnandresy.lol.ui.components.BubbleGloss
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
 * Les emojis proposés sur un message.
 *
 * Six, pas plus : au-delà, la rangée ne tient plus sur un écran étroit et le
 * choix devient une corvée au lieu d'un réflexe.
 */
val MESSAGE_REACTIONS = listOf("♡", "◎", "◎", "◡", "✦", "✓")

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

/** La rangée d'emojis. Celui qu'on a déjà posé est rempli. */
@Composable
fun ReactionRow(
    selected: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MESSAGE_REACTIONS.forEach { emoji ->
            val picked = selected == emoji
            TapArea(
                onTap = { tap(); onPick(emoji) },
                scaleDown = 0.85f,
                modifier = Modifier
                    .size(38.dp)
                    .bubbleShell(
                        androidx.compose.foundation.shape.CircleShape,
                        if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else palette.bubble,
                        if (picked) MaterialTheme.colorScheme.primary else palette.bubbleBorder,
                        palette.shadow,
                        if (picked) 5.dp else 2.dp
                    )
            ) {
                BubbleGloss(androidx.compose.foundation.shape.CircleShape, if (picked) 0.6f else 0.3f)
                Text(emoji, fontSize = 17.sp, modifier = Modifier.align(Alignment.Center))
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
        counts.forEach { (emoji, count) ->
            val isMine = mine == emoji
            TapArea(
                onTap = { tap(); onToggle(emoji) },
                scaleDown = 0.88f,
                modifier = Modifier.bubbleShell(
                    shape,
                    if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else palette.bubble,
                    if (isMine) MaterialTheme.colorScheme.primary else palette.bubbleBorder,
                    palette.shadow,
                    if (isMine) 4.dp else 2.dp
                )
            ) {
                BubbleGloss(shape, if (isMine) 0.6f else 0.3f)
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(emoji, fontSize = 10.sp)
                    // Un seul auteur, c'est déjà dit par l'emoji : le « 1 »
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
