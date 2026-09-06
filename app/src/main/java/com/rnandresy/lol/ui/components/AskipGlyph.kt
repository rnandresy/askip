package com.rnandresy.lol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
//  Les signes de l'app
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Les figures dessinées de l'app.
 *
 * Elles remplacent les emojis, et sont **dessinées** plutôt qu'écrites. Trois
 * raisons, dans cet ordre :
 *
 * 1. Un emoji est un caractère : sa taille dépend de la police du système, son
 *    dessin change d'un téléphone à l'autre, et sa couleur est figée — un
 *    emoji jaune reste jaune sur les neuf thèmes.
 * 2. Une image importée fige la résolution et pèse dans l'APK.
 * 3. Un chemin vectoriel reste net à toutes les tailles, prend la couleur qu'on
 *    lui donne, et ne coûte rien.
 *
 * Le relief vient de trois couches, toujours dans cet ordre : une ombre portée
 * décalée vers le bas, un dégradé du clair vers le sombre en diagonale, et un
 * reflet posé en haut à gauche. C'est la recette des icônes en volume — et
 * comme le dégradé est calculé à partir de la couleur donnée, chaque figure
 * s'accorde au thème sans qu'on ait à la redessiner.
 */
enum class GlyphKind {
    /** L'étoile à cinq branches — trophées, missions, la marque de l'app. */
    STAR,

    /** L'éclat à quatre branches — ce qui brille, ce qui est neuf. */
    SPARKLE,

    /** La goutte de feu — la chaleur, les séries. */
    FLAME,

    /** Le cœur — la réaction la plus commune. */
    HEART,

    /** La fleur de cerisier — la signature du thème Sakura. */
    FLOWER,

    /** Le croissant — la nuit, le sommeil, le calme. */
    MOON,

    /** Le disque rayonnant — le jour, le sujet quotidien. */
    SUN,

    /** Le losange taillé — le Scoop, ce qui est rare. */
    GEM,

    /** Le cercle voilé — l'anonymat, la confession, le masque. */
    VEIL,

    /** La coche — le verdict crédible, l'accord. */
    CHECK,

    /** La croix — le verdict bidon, le refus. */
    CROSS,

    /** La bulle — la conversation, les commentaires. */
    BUBBLE,

    /** La couronne — l'administration du campus. */
    CROWN,

    /** La cloche — les notifications. */
    BELL
}

/**
 * Une figure, dessinée en volume.
 *
 * [tint] par défaut prend la couleur d'accent du thème. [size] est
 * volontairement petit : ces figures accompagnent du texte, elles ne le
 * remplacent pas.
 */
@Composable
fun AskipGlyph(
    kind: GlyphKind,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color? = null
) {
    val palette = LocalAskipPalette.current
    val base = tint ?: defaultTintFor(kind, palette.scoop, palette.streak, palette.petal)

    Canvas(modifier.size(size)) {
        drawGlyph(kind, base)
    }
}

/** La couleur naturelle d'une figure, quand on ne lui en impose pas. */
private fun defaultTintFor(
    kind: GlyphKind,
    scoop: Color,
    streak: Color,
    petal: Color
): Color = when (kind) {
    GlyphKind.FLAME -> streak
    GlyphKind.GEM -> scoop
    GlyphKind.FLOWER -> petal
    GlyphKind.HEART -> Color(0xFFE8607F)
    GlyphKind.STAR, GlyphKind.SPARKLE, GlyphKind.CROWN, GlyphKind.SUN -> Color(0xFFE7B44C)
    GlyphKind.MOON -> Color(0xFF9AA7D4)
    GlyphKind.CHECK -> Color(0xFF52A06E)
    GlyphKind.CROSS -> Color(0xFFCB5A63)
    GlyphKind.BUBBLE, GlyphKind.VEIL, GlyphKind.BELL -> Color(0xFF8D9AAE)
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le rendu
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Pose la figure et son relief.
 *
 * L'ombre est dessinée avant la forme, décalée d'un vingtième de la hauteur :
 * assez pour décoller du fond, trop peu pour qu'on la remarque comme une ombre.
 */
private fun DrawScope.drawGlyph(kind: GlyphKind, base: Color) {
    val path = pathFor(kind, size)

    // Les traits se dessinent au pinceau, pas au remplissage : une coche pleine
    // n'aurait aucune allure.
    if (kind == GlyphKind.CHECK || kind == GlyphKind.CROSS) {
        drawStrokeGlyph(kind, base)
        return
    }

    translate(top = size.height * 0.05f) {
        drawPath(path, Color.Black.copy(alpha = 0.16f))
    }

    drawPath(path, volumeBrush(base, size))
    drawPath(path, highlightBrush(size))
}

/**
 * Le dégradé qui donne le volume.
 *
 * Il part d'une version éclaircie en haut à gauche pour aller vers une version
 * assombrie en bas à droite — la lumière vient toujours du même coin dans toute
 * l'app, sinon les figures paraissent éclairées au hasard quand elles se
 * côtoient.
 */
private fun volumeBrush(base: Color, size: Size): Brush = Brush.linearGradient(
    0f to lerp(base, Color.White, 0.42f),
    0.45f to base,
    1f to lerp(base, Color.Black, 0.30f),
    start = Offset(size.width * 0.15f, 0f),
    end = Offset(size.width * 0.9f, size.height)
)

/** Le reflet : un voile clair sur le tiers supérieur, qui s'éteint vite. */
private fun highlightBrush(size: Size): Brush = Brush.radialGradient(
    0f to Color.White.copy(alpha = 0.55f),
    0.55f to Color.White.copy(alpha = 0.10f),
    1f to Color.Transparent,
    center = Offset(size.width * 0.33f, size.height * 0.26f),
    radius = size.minDimension * 0.55f
)

/** Les figures faites d'un trait : coche et croix. */
private fun DrawScope.drawStrokeGlyph(kind: GlyphKind, base: Color) {
    val w = size.width
    val h = size.height
    val trait = Stroke(
        width = w * 0.17f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    val chemin = Path().apply {
        if (kind == GlyphKind.CHECK) {
            moveTo(w * 0.20f, h * 0.54f)
            lineTo(w * 0.42f, h * 0.75f)
            lineTo(w * 0.81f, h * 0.26f)
        } else {
            moveTo(w * 0.24f, h * 0.24f)
            lineTo(w * 0.76f, h * 0.76f)
            moveTo(w * 0.76f, h * 0.24f)
            lineTo(w * 0.24f, h * 0.76f)
        }
    }

    // Un trait plein paraît plat : on double d'une ombre légère au-dessous.
    translate(top = h * 0.06f) {
        drawPath(chemin, Color.Black.copy(alpha = 0.14f), style = trait)
    }
    drawPath(chemin, lerp(base, Color.White, 0.10f), style = trait)
}

// ═════════════════════════════════════════════════════════════════════════════
//  Les tracés
// ═════════════════════════════════════════════════════════════════════════════

private fun pathFor(kind: GlyphKind, s: Size): Path = when (kind) {
    GlyphKind.STAR -> starPath(s, branches = 5, creux = 0.42f)
    GlyphKind.SPARKLE -> sparklePath(s)
    GlyphKind.FLAME -> flamePath(s)
    GlyphKind.HEART -> heartPath(s)
    GlyphKind.FLOWER -> flowerPath(s)
    GlyphKind.MOON -> moonPath(s)
    GlyphKind.SUN -> starPath(s, branches = 8, creux = 0.62f)
    GlyphKind.GEM -> gemPath(s)
    GlyphKind.VEIL -> veilPath(s)
    GlyphKind.BUBBLE -> bubblePath(s)
    GlyphKind.CROWN -> crownPath(s)
    GlyphKind.BELL -> bellPath(s)
    GlyphKind.CHECK, GlyphKind.CROSS -> Path()
}

/** Une étoile régulière. [creux] est le rayon intérieur, en part du rayon. */
private fun starPath(s: Size, branches: Int, creux: Float): Path {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val r = s.minDimension / 2f * 0.96f
    val pas = PI / branches

    return Path().apply {
        for (i in 0 until branches * 2) {
            val rayon = if (i % 2 == 0) r else r * creux
            // On démarre à midi : une étoile pointe vers le haut.
            val angle = -PI / 2f + i * pas
            val x = cx + (rayon * cos(angle)).toFloat()
            val y = cy + (rayon * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** L'éclat à quatre branches, aux flancs creusés. */
private fun sparklePath(s: Size): Path {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val r = s.minDimension / 2f
    val c = r * 0.30f

    return Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx + c, cy - c, cx + r, cy)
        quadraticTo(cx + c, cy + c, cx, cy + r)
        quadraticTo(cx - c, cy + c, cx - r, cy)
        quadraticTo(cx - c, cy - c, cx, cy - r)
        close()
    }
}

/** La goutte de feu : une pointe en haut, un ventre en bas. */
private fun flamePath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.04f)
        cubicTo(w * 0.74f, h * 0.30f, w * 0.94f, h * 0.46f, w * 0.86f, h * 0.68f)
        cubicTo(w * 0.79f, h * 0.90f, w * 0.60f, h * 0.98f, w * 0.50f, h * 0.98f)
        cubicTo(w * 0.40f, h * 0.98f, w * 0.21f, h * 0.90f, w * 0.14f, h * 0.68f)
        cubicTo(w * 0.06f, h * 0.46f, w * 0.26f, h * 0.30f, w * 0.50f, h * 0.04f)
        close()
    }
}

/** Le cœur, deux lobes et une pointe. */
private fun heartPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.94f)
        cubicTo(w * 0.10f, h * 0.66f, w * 0.02f, h * 0.40f, w * 0.16f, h * 0.20f)
        cubicTo(w * 0.30f, h * 0.02f, w * 0.46f, h * 0.10f, w * 0.50f, h * 0.26f)
        cubicTo(w * 0.54f, h * 0.10f, w * 0.70f, h * 0.02f, w * 0.84f, h * 0.20f)
        cubicTo(w * 0.98f, h * 0.40f, w * 0.90f, h * 0.66f, w * 0.50f, h * 0.94f)
        close()
    }
}

/** Cinq pétales autour d'un centre. */
private fun flowerPath(s: Size): Path {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val r = s.minDimension / 2f

    return Path().apply {
        for (i in 0 until 5) {
            val angle = -PI / 2f + i * 2 * PI / 5
            val px = cx + (r * 0.52f * cos(angle)).toFloat()
            val py = cy + (r * 0.52f * sin(angle)).toFloat()
            addOval(
                Rect(
                    left = px - r * 0.46f,
                    top = py - r * 0.46f,
                    right = px + r * 0.46f,
                    bottom = py + r * 0.46f
                )
            )
        }
    }
}

/**
 * Le croissant : un disque dont on retire un second, décalé.
 *
 * `PathOperation.Difference` fait le travail — dessiner un croissant à la main
 * demanderait deux arcs dont les tangentes ne se rejoindraient jamais tout à
 * fait.
 */
private fun moonPath(s: Size): Path {
    val r = s.minDimension / 2f
    val plein = Path().apply {
        addOval(Rect(0f, 0f, s.width, s.height))
    }
    val creux = Path().apply {
        addOval(
            Rect(
                left = s.width * 0.28f,
                top = -r * 0.16f,
                right = s.width * 1.32f,
                bottom = s.height + r * 0.16f
            )
        )
    }
    return Path().apply { op(plein, creux, PathOperation.Difference) }
}

/** Le losange taillé, plus haut que large. */
private fun gemPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.03f)
        lineTo(w * 0.94f, h * 0.38f)
        lineTo(w * 0.50f, h * 0.97f)
        lineTo(w * 0.06f, h * 0.38f)
        close()
    }
}

/** Un anneau : le masque, ce qui est là sans se montrer. */
private fun veilPath(s: Size): Path {
    val exterieur = Path().apply { addOval(Rect(0f, 0f, s.width, s.height)) }
    val interieur = Path().apply {
        addOval(
            Rect(
                s.width * 0.30f, s.height * 0.30f,
                s.width * 0.70f, s.height * 0.70f
            )
        )
    }
    return Path().apply { op(exterieur, interieur, PathOperation.Difference) }
}

/** La bulle de conversation, avec sa pointe en bas à gauche. */
private fun bubblePath(s: Size): Path {
    val w = s.width
    val h = s.height
    val r = w * 0.26f
    return Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 0f, top = 0f, right = w, bottom = h * 0.78f,
                radiusX = r, radiusY = r
            )
        )
        moveTo(w * 0.24f, h * 0.72f)
        lineTo(w * 0.20f, h * 0.99f)
        lineTo(w * 0.46f, h * 0.76f)
        close()
    }
}

/** Trois pointes sur un socle. */
private fun crownPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.06f, h * 0.78f)
        lineTo(w * 0.06f, h * 0.28f)
        lineTo(w * 0.28f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.16f)
        lineTo(w * 0.72f, h * 0.52f)
        lineTo(w * 0.94f, h * 0.28f)
        lineTo(w * 0.94f, h * 0.78f)
        close()
    }
}

/** La cloche, son battant compris. */
private fun bellPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        cubicTo(w * 0.76f, h * 0.06f, w * 0.80f, h * 0.30f, w * 0.80f, h * 0.52f)
        cubicTo(w * 0.80f, h * 0.66f, w * 0.90f, h * 0.72f, w * 0.90f, h * 0.78f)
        lineTo(w * 0.10f, h * 0.78f)
        cubicTo(w * 0.10f, h * 0.72f, w * 0.20f, h * 0.66f, w * 0.20f, h * 0.52f)
        cubicTo(w * 0.20f, h * 0.30f, w * 0.24f, h * 0.06f, w * 0.50f, h * 0.06f)
        close()
        addOval(
            Rect(
                w * 0.40f, h * 0.80f,
                w * 0.60f, h * 0.99f
            )
        )
    }
}

/**
 * La figure qui va avec une réaction du fil.
 *
 * La correspondance vit ici, dans l'interface, et non à côté des clés dans
 * `utils` : la couche des données n'a pas à connaître les dessins, c'est ce qui
 * permet de changer l'un sans toucher à l'autre.
 */
fun glyphForReaction(key: String): GlyphKind = when (key) {
    "love" -> GlyphKind.HEART
    "fire" -> GlyphKind.FLAME
    "lol" -> GlyphKind.SPARKLE
    "shock" -> GlyphKind.STAR
    "eyes" -> GlyphKind.MOON
    else -> GlyphKind.HEART
}
