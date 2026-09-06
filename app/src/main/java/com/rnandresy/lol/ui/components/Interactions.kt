@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.theme.Motion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.REACTIONS
import com.rnandresy.lol.utils.REACTION_LABELS

// ═════════════════════════════════════════════════════════════════════════════
//  Le toucher
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Zone tappable qui se tasse sous le doigt.
 *
 * C'est le détail qui sépare une interface Android d'une interface iPhone : le
 * doigt doit sentir qu'il touche quelque chose de physique. L'ondulation
 * Material, qui se propage depuis le point de contact, est remplacée par un
 * léger enfoncement au ressort — plus discret, et identique partout dans l'app.
 */
@Composable
fun TapArea(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = Motion.spring(),
        label = "pressScale"
    )
    val tap = rememberTapFeedback()

    Box(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled, onTap, onLongPress) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        // `tryAwaitRelease` couvre aussi l'annulation : le doigt
                        // qui glisse hors de la zone doit relâcher l'enfoncement.
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() },
                    onLongPress = onLongPress?.let { action -> { tap(); action() } }
                )
            },
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  Sélecteur segmenté
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le sélecteur à la iOS : un rail sombre, une pastille claire qui glisse.
 *
 * Il remplace des rangées de puces empilées. Une seule ligne, et la position
 * de la pastille suffit à dire où l'on est — sans couleur ni gras.
 */
@Composable
fun <T> SlidingSegmented(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    glyphOf: ((T) -> GlyphKind)? = null
) {
    val index = items.indexOf(selected).coerceAtLeast(0)

    // La pastille glisse par le jeu des poids de part et d'autre : aucune
    // mesure manuelle, donc aucun décalage sur les écrans étroits.
    val before by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = Motion.spring(),
        label = "segIndicator"
    )
    val after = (items.size - 1).toFloat() - before

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(34.dp)) {
            if (before > 0.001f) Spacer(Modifier.weight(before))
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(Radius.xs),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {}
            if (after > 0.001f) Spacer(Modifier.weight(after))
        }

        Row(Modifier.fillMaxWidth().height(34.dp)) {
            items.forEach { item ->
                val isSelected = item == selected
                TapArea(
                    onTap = { onSelect(item) },
                    scaleDown = 0.94f,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.xs)
                    ) {
                        glyphOf?.let { AskipGlyph(kind = it(item), size = 12.dp) }
                        Text(
                            labelOf(item),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Réactions
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Un seul bouton au lieu de cinq.
 *
 * Le fil affichait les cinq émojis sur chaque carte : à dix rumeurs à l'écran,
 * cinquante émojis, et l'œil ne savait plus où se poser. Un appui réagit, un
 * appui long ouvre le choix. La fonction est intacte : elle demande un geste de
 * plus, ce qui est le juste prix d'une action qu'on fait rarement.
 */
@Composable
fun ReactionAction(
    myReaction: String?,
    total: Int,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val tap = rememberTapFeedback()

    LaunchedEffect(myReaction) {
        if (myReaction != null) {
            scale.animateTo(1.25f, Motion.pop())
            scale.animateTo(1f, Motion.spring())
        }
    }

    Box(modifier = modifier) {
        TapArea(
            onTap = {
                tap()
                onReact(myReaction ?: REACTIONS.first())
            },
            onLongPress = { showPicker = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
                modifier = Modifier.padding(Space.xs)
            ) {
                AskipGlyph(
                    kind = glyphForReaction(myReaction ?: "love"),
                    size = 16.dp,
                    // Sans réaction posée, la figure s'efface au gris du texte
                    // secondaire : elle invite sans prétendre être un état.
                    tint = if (myReaction == null)
                        MaterialTheme.colorScheme.onSurfaceVariant else null,
                    modifier = Modifier
                        .scale(scale.value)
                        .semantics {
                            contentDescription =
                                if (myReaction == null) "Réagir"
                                else "Ta réaction : ${REACTION_LABELS[myReaction] ?: myReaction}"
                        }
                )
                if (total > 0) {
                    Text(
                        "$total",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (myReaction != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showPicker,
            enter = fadeIn(Motion.spring()) + scaleIn(Motion.pop(), initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            ReactionPicker(
                current = myReaction,
                onPick = {
                    onReact(it)
                    showPicker = false
                },
                onDismiss = { showPicker = false }
            )
        }
    }
}

@Composable
private fun ReactionPicker(
    current: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tap = rememberTapFeedback()

    Surface(
        shape = RoundedCornerShape(Radius.pill),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            REACTIONS.forEach { key ->
                TapArea(
                    onTap = { tap(); onPick(key) },
                    scaleDown = 0.85f,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (key == current) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                ) {
                    AskipGlyph(
                        kind = glyphForReaction(key),
                        size = 19.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = REACTION_LABELS[key] ?: key
                            }
                    )
                }
            }
            // Le geste « appuyer ailleurs » referme aussi, mais une croix
            // explicite évite de piéger qui ne le connaît pas.
            //
            // Elle seule est élargie ici : les pastilles voisines portent le
            // surlignage de la sélection sur leur fond, donc agrandir leur
            // boîte agrandirait le dessin. La croix, elle, n'a pas de fond.
            TapArea(onTap = onDismiss, modifier = Modifier.size(MinTouchTarget)) {
                AskipGlyph(
                    kind = GlyphKind.CROSS,
                    size = 12.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics { contentDescription = "Fermer" }
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Feuilles glissantes
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Titre d'une feuille.
 *
 * Les fonctions secondaires vivent désormais dans des feuilles plutôt que sur
 * la carte : elles restent à un geste, sans encombrer le fil.
 */
@Composable
fun SheetHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.xl, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Ligne d'action dans une feuille : emoji, libellé, explication facultative. */
@Composable
fun SheetAction(
    glyph: GlyphKind,
    label: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tap = rememberTapFeedback()
    TapArea(
        onTap = { tap(); onClick() },
        enabled = enabled,
        scaleDown = 0.99f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AskipGlyph(kind = glyph, size = 17.dp)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) tint
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
