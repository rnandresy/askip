package com.rnandresy.lol.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Motion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space

// ═════════════════════════════════════════════════════════════════════════════
//  Lisibilité
// ═════════════════════════════════════════════════════════════════════════════

/**
 * La couleur d'écriture qui reste lisible sur [background], quel que soit le
 * thème.
 *
 * Le rose du thème Sakura, le vert de Matrice et le blanc de Noir & Blanc
 * n'appellent pas le même texte. Plutôt que de maintenir une table de
 * correspondances par thème — qu'on oublierait d'étendre au thème suivant —
 * on tranche sur la luminance. Le seuil est à 0,55 et non 0,5 : l'œil perçoit
 * le texte sombre comme lisible un peu plus tôt que le blanc.
 */
fun readableOn(background: Color): Color =
    if (background.luminance() > 0.55f) Color(0xFF1B1B1B) else Color.White

// ═════════════════════════════════════════════════════════════════════════════
//  La bulle
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le relief commun à tous les éléments cliquables de l'app.
 *
 * Quatre couches, toujours dans cet ordre : l'ombre portée détache du fond, le
 * remplissage donne la couleur, le liseré dessine le contour, et le reflet posé
 * par-dessus fait la bulle. Retirer une seule des quatre suffit à faire
 * retomber le bouton à plat.
 */
fun Modifier.bubbleShell(
    shape: Shape,
    fill: Color,
    border: Color,
    shadowColor: Color,
    elevation: Dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = shadowColor,
        spotColor = shadowColor
    )
    .clip(shape)
    .background(fill)
    .border(1.dp, border, shape)

/** Le reflet, à poser en dernier à l'intérieur d'une bulle. */
@Composable
fun BoxScope.BubbleGloss(shape: Shape, intensity: Float = 1f) {
    Box(
        Modifier
            .matchParentSize()
            .clip(shape)
            .background(glossBrush(intensity))
    )
}

enum class BubbleTone {
    /** Action principale : remplie de la couleur d'accent. */
    PRIMARY,

    /** Action courante : remplie de la couleur de surface, liseré discret. */
    SOFT,

    /** Action tertiaire : transparente, seul le liseré la dessine. */
    GHOST,

    /** Action destructrice. */
    DANGER
}

/**
 * Les trois tailles de bulle.
 *
 * Volontairement menues : une commande secondaire ne doit pas peser autant que
 * ce qu'elle commande. `LARGE` reste réservée aux boutons qui portent tout un
 * écran — se connecter, publier.
 */
enum class BubbleSize(val height: Dp, val padding: Dp, val textSize: Int) {
    SMALL(28.dp, Space.sm, 11),
    MEDIUM(36.dp, Space.lg, 13),
    LARGE(46.dp, Space.xl, 14)
}

private data class BubbleColors(
    val fill: Color,
    val content: Color,
    val border: Color,
    val elevation: Dp,
    val gloss: Float
)

@Composable
private fun colorsFor(tone: BubbleTone, enabled: Boolean): BubbleColors {
    val palette = LocalAskipPalette.current
    val scheme = MaterialTheme.colorScheme

    val colors = when (tone) {
        BubbleTone.PRIMARY -> BubbleColors(
            fill = scheme.primary,
            content = readableOn(scheme.primary),
            border = scheme.primary,
            elevation = 6.dp,
            gloss = 1f
        )
        BubbleTone.SOFT -> BubbleColors(
            fill = palette.bubble,
            content = scheme.onSurface,
            border = palette.bubbleBorder,
            elevation = 3.dp,
            gloss = 0.7f
        )
        BubbleTone.GHOST -> BubbleColors(
            fill = Color.Transparent,
            content = scheme.onSurfaceVariant,
            border = palette.bubbleBorder,
            elevation = 0.dp,
            gloss = 0f
        )
        BubbleTone.DANGER -> BubbleColors(
            fill = scheme.error,
            content = readableOn(scheme.error),
            border = scheme.error,
            elevation = 6.dp,
            gloss = 1f
        )
    }
    // Désactivée, la bulle perd son relief en plus de son opacité : sans ça,
    // une bulle grisée mais toujours ombrée continue d'appeler le doigt.
    return if (enabled) colors else colors.copy(elevation = 0.dp, gloss = 0f)
}

/**
 * Le bouton de l'app.
 *
 * Une bulle avec un emoji optionnel devant. Utilisé de l'écran de connexion
 * jusqu'au fil : c'est la répétition de cette même forme qui donne son unité à
 * l'ensemble.
 */
@Composable
fun BubbleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: GlyphKind? = null,
    icon: ImageVector? = null,
    tone: BubbleTone = BubbleTone.PRIMARY,
    size: BubbleSize = BubbleSize.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = false
) {
    val c = colorsFor(tone, enabled && !loading)
    val shape = RoundedCornerShape(Radius.pill)
    val tap = rememberTapFeedback()

    val opacity by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = Motion.spring(),
        label = "bubbleAlpha"
    )

    TapArea(
        onTap = { tap(); onClick() },
        enabled = enabled && !loading,
        modifier = modifier
            .alpha(opacity)
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .bubbleShell(shape, c.fill, c.border, LocalAskipPalette.current.shadow, c.elevation)
    ) {
        if (c.gloss > 0f) BubbleGloss(shape, c.gloss)

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = size.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = c.content,
                    strokeWidth = 2.dp
                )
                glyph != null -> AskipGlyph(
                    kind = glyph,
                    size = (size.textSize + 2).dp,
                    // Sur une bulle pleine, la figure garde sa couleur propre
                    // et se perdrait dans le fond : elle prend celle du texte.
                    tint = if (tone == BubbleTone.PRIMARY || tone == BubbleTone.DANGER)
                        c.content else null
                )
                icon != null -> Icon(icon, null, Modifier.size(18.dp), tint = c.content)
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = size.textSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.content,
                maxLines = 1
            )
        }
    }
}

/**
 * La plus petite zone qu'un doigt atteint sans viser.
 *
 * 48 dp, la valeur retenue par Android comme par Material. Ce n'est pas la
 * taille du dessin : les bulles de l'app sont volontairement petites, et le
 * restent. C'est la surface qui écoute le doigt autour.
 */
val MinTouchTarget = 48.dp

/**
 * Bulle ronde ne contenant qu'une icône.
 *
 * [diameter] est ce qu'on voit ; [minTouch] est ce qu'on touche. Les deux sont
 * séparés à dessein : une bulle de 26 dp reste jolie, mais un doigt la rate une
 * fois sur trois. Le dessin est donc centré dans une zone plus large, qui porte
 * seule le geste.
 *
 * Élargir cette zone élargit aussi l'encombrement en mise en page. Là où la
 * place manque — une croix posée sur un avatar, par exemple — passer un
 * [minTouch] plus petit vaut mieux que de laisser la bulle avaler son voisin.
 */
@Composable
fun BubbleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: BubbleTone = BubbleTone.SOFT,
    diameter: Dp = 32.dp,
    badge: Int = 0,
    enabled: Boolean = true,
    minTouch: Dp = MinTouchTarget
) {
    val c = colorsFor(tone, enabled)
    val shape = CircleShape
    val tap = rememberTapFeedback()
    val palette = LocalAskipPalette.current
    val zone = if (diameter >= minTouch) diameter else minTouch

    Box(
        modifier = modifier.alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center
    ) {
        // Le geste est porté par la zone élargie, pas par la bulle : posé sur
        // la bulle, il aurait laissé la marge gagnée inerte — plus grande à la
        // mesure, toujours aussi difficile à atteindre.
        TapArea(
            onTap = { tap(); onClick() },
            enabled = enabled,
            scaleDown = 0.9f,
            modifier = Modifier.size(zone)
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(diameter)
                    .bubbleShell(shape, c.fill, c.border, palette.shadow, c.elevation)
            ) {
                if (c.gloss > 0f) BubbleGloss(shape, c.gloss)
                Icon(
                    icon,
                    contentDescription,
                    Modifier
                        .align(Alignment.Center)
                        .size(diameter * 0.48f),
                    tint = c.content
                )
            }
        }
        // La pastille se cale sur le coin du dessin, jamais sur celui de la
        // zone de toucher : sinon elle flotterait à l'écart de sa bulle.
        if (badge != 0) {
            Box(Modifier.size(diameter), contentAlignment = Alignment.TopEnd) {
                BubbleBadge(count = badge)
            }
        }
    }
}

/**
 * La pastille de compteur.
 *
 * [count] négatif affiche un simple point : pour « il y a quelque chose », sans
 * nombre à montrer.
 */
@Composable
fun BubbleBadge(
    count: Int,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.error
) {
    val palette = LocalAskipPalette.current
    val shape = CircleShape
    val dotOnly = count < 0

    Box(
        modifier = modifier
            .then(
                if (dotOnly) Modifier.size(10.dp)
                else Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            )
            .bubbleShell(shape, tint, tint, palette.shadow, 3.dp),
        contentAlignment = Alignment.Center
    ) {
        BubbleGloss(shape, 0.8f)
        if (!dotOnly) {
            Text(
                if (count > 99) "99+" else "$count",
                modifier = Modifier.padding(horizontal = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = readableOn(tint)
            )
        }
    }
}

/**
 * La puce d'information : verdict, tag, statut.
 *
 * Même relief que les boutons, en plus petit — c'est ce qui fait que les
 * badges appartiennent visiblement à la même famille.
 */
@Composable
fun BubbleChip(
    label: String,
    modifier: Modifier = Modifier,
    glyph: GlyphKind? = null,
    accent: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalAskipPalette.current
    val shape = RoundedCornerShape(Radius.pill)
    val fill = if (filled) accent else palette.bubble
    val content = if (filled) readableOn(accent) else accent
    val border = if (filled) accent else accent.copy(alpha = 0.35f)

    val body: @Composable BoxScope.() -> Unit = {
        BubbleGloss(shape, if (filled) 0.9f else 0.5f)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (glyph != null) {
                AskipGlyph(kind = glyph, size = 11.dp, tint = if (filled) content else null)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1
            )
        }
    }

    val shell = modifier
        .alpha(if (enabled) 1f else 0.45f)
        .bubbleShell(shape, fill, border, palette.shadow, if (enabled) 2.dp else 0.dp)

    if (onClick != null) {
        val tap = rememberTapFeedback()
        TapArea(
            onTap = { tap(); onClick() },
            enabled = enabled,
            modifier = shell,
            content = body
        )
    } else {
        Box(modifier = shell, content = body)
    }
}

/**
 * Une surface en bulle, pour les cartes et les panneaux.
 * Même grammaire que les boutons, avec un rayon plus large.
 */
@Composable
fun BubbleCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.lg),
    fill: Color = LocalAskipPalette.current.bubble,
    elevation: Dp = 4.dp,
    gloss: Float = 0.45f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalAskipPalette.current
    val shell = modifier.bubbleShell(shape, fill, palette.bubbleBorder, palette.shadow, elevation)

    val body: @Composable BoxScope.() -> Unit = {
        if (gloss > 0f) BubbleGloss(shape, gloss)
        content()
    }

    if (onClick != null) {
        val tap = rememberTapFeedback()
        TapArea(
            onTap = { tap(); onClick() },
            scaleDown = 0.99f,
            modifier = shell,
            content = body
        )
    } else {
        Box(modifier = shell, content = body)
    }
}
