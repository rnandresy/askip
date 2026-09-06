package com.rnandresy.lol.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
    BELL,

    // ── Salons et formats ─────────────────────────────────────────────────

    /** Le livre — l'amphi, les cours, l'ENI. */
    BOOK,

    /** Le crayon — les examens, l'écriture, la modification. */
    PENCIL,

    /** Le ballon — le sport. */
    BALL,

    /** Le masque de théâtre — le drama. */
    DRAMA,

    /** Le cadenas — la capsule scellée, ce qui attend son heure. */
    LOCK,

    /** La clé — ce qui ouvre la capsule. */
    KEY,

    /** La balance — la Page de Vérité, le serment. */
    SCALE,

    /** Les barres — le sondage, les statistiques. */
    CHART,

    /** Les maillons — le téléphone arabe. */
    LINK,

    /** Le crâne — ce qui dépasse l'entendement. */
    SKULL,

    // ── Médias ────────────────────────────────────────────────────────────

    /** Le cadre — une photo. */
    PHOTO,

    /** Le triangle — une vidéo. */
    PLAY,

    /** L'onde — un message vocal. */
    WAVE,

    /** La feuille — un fichier joint. */
    DOC,

    /** La loupe — la recherche. */
    SEARCH,

    /** Le billet — un pari. */
    TICKET,

    // ── Actions ───────────────────────────────────────────────────────────

    /** Le drapeau — signaler à l'administration. */
    FLAG,

    /** Les deux feuillets — copier. */
    COPY,

    /** La flèche coudée — répondre. */
    REPLY,

    /** Deux silhouettes — les membres, un groupe. */
    PEOPLE,

    /** La croix pleine — ajouter, créer. */
    PLUS,

    /**
     * Le triangle d'attention — une erreur, un avertissement.
     *
     * Distinct de [FLAG], qui veut dire « signaler à l'administration ».
     * Confondre les deux revenait à demander de dénoncer un mot de passe raté.
     */
    ALERT
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

    // `drawWithCache` plutôt que `Canvas` : le bloc ci-dessous ne tourne qu'au
    // changement de taille, de figure ou de couleur. Avec un simple dessin, le
    // chemin et les deux dégradés étaient reconstruits à chaque frame — étoiles
    // et fleurs refaisaient leur trigonométrie soixante fois par seconde, pour
    // chacune des dizaines de figures d'un écran.
    Spacer(
        modifier
            .size(size)
            .drawWithCache {
                val h = this.size.height

                // Les traits se dessinent au pinceau, pas au remplissage : une
                // coche pleine n'aurait aucune allure.
                if (kind == GlyphKind.CHECK || kind == GlyphKind.CROSS) {
                    val chemin = strokePathFor(kind, this.size)
                    val trait = Stroke(
                        width = this.size.width * 0.17f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                    val ombre = Color.Black.copy(alpha = 0.14f)
                    val plein = lerp(base, Color.White, 0.10f)
                    val decal = h * 0.06f
                    return@drawWithCache onDrawBehind {
                        // Un trait plein paraît plat : on double d'une ombre
                        // légère au-dessous.
                        translate(top = decal) { drawPath(chemin, ombre, style = trait) }
                        drawPath(chemin, plein, style = trait)
                    }
                }

                val chemin = pathFor(kind, this.size)
                val volume = volumeBrush(base, this.size)
                val reflet = highlightBrush(this.size)
                val ombre = Color.Black.copy(alpha = 0.16f)
                val decal = h * 0.05f
                onDrawBehind {
                    translate(top = decal) { drawPath(chemin, ombre) }
                    drawPath(chemin, volume)
                    drawPath(chemin, reflet)
                }
            }
    )
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
    GlyphKind.CROSS, GlyphKind.SKULL -> Color(0xFFCB5A63)
    GlyphKind.BOOK, GlyphKind.PENCIL -> Color(0xFF7E8FB8)
    GlyphKind.BALL -> Color(0xFF5FA97E)
    GlyphKind.DRAMA -> Color(0xFFB07BC4)
    GlyphKind.LOCK, GlyphKind.KEY -> Color(0xFFC49A5A)
    GlyphKind.SCALE -> Color(0xFF8D9AAE)
    GlyphKind.CHART -> Color(0xFF6E93C4)
    GlyphKind.LINK -> Color(0xFF8D9AAE)
    GlyphKind.TICKET -> scoop
    GlyphKind.FLAG, GlyphKind.ALERT -> Color(0xFFCB5A63)
    GlyphKind.PHOTO, GlyphKind.PLAY, GlyphKind.WAVE, GlyphKind.DOC,
    GlyphKind.SEARCH, GlyphKind.BUBBLE, GlyphKind.VEIL, GlyphKind.BELL,
    GlyphKind.COPY, GlyphKind.REPLY, GlyphKind.PEOPLE,
    GlyphKind.PLUS -> Color(0xFF8D9AAE)
}

// ═════════════════════════════════════════════════════════════════════════════
//  Le rendu
// ═════════════════════════════════════════════════════════════════════════════

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
private fun strokePathFor(kind: GlyphKind, s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
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
    GlyphKind.BOOK -> bookPath(s)
    GlyphKind.PENCIL -> pencilPath(s)
    GlyphKind.BALL -> ballPath(s)
    GlyphKind.DRAMA -> dramaPath(s)
    GlyphKind.LOCK -> lockPath(s)
    GlyphKind.KEY -> keyPath(s)
    GlyphKind.SCALE -> scalePath(s)
    GlyphKind.CHART -> chartPath(s)
    GlyphKind.LINK -> linkPath(s)
    GlyphKind.SKULL -> skullPath(s)
    GlyphKind.PHOTO -> photoPath(s)
    GlyphKind.PLAY -> playPath(s)
    GlyphKind.WAVE -> wavePath(s)
    GlyphKind.DOC -> docPath(s)
    GlyphKind.SEARCH -> searchPath(s)
    GlyphKind.TICKET -> ticketPath(s)
    GlyphKind.FLAG -> flagPath(s)
    GlyphKind.COPY -> copyPath(s)
    GlyphKind.REPLY -> replyPath(s)
    GlyphKind.PEOPLE -> peoplePath(s)
    GlyphKind.PLUS -> plusPath(s)
    GlyphKind.ALERT -> alertPath(s)
    GlyphKind.CHECK, GlyphKind.CROSS -> Path()
}

/**
 * La croix d'ajout : deux barres arrondies qui se croisent.
 *
 * Les deux rectangles se recouvrent au centre ; le remplissage par défaut les
 * fond en une seule masse, sans découpe à faire.
 */
private fun plusPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val e = w * 0.18f
    val r = e * 0.5f
    return Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.09f, top = h * 0.5f - e / 2f,
                right = w * 0.91f, bottom = h * 0.5f + e / 2f,
                radiusX = r, radiusY = r
            )
        )
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.5f - e / 2f, top = h * 0.09f,
                right = w * 0.5f + e / 2f, bottom = h * 0.91f,
                radiusX = r, radiusY = r
            )
        )
    }
}

/**
 * Le triangle d'attention.
 *
 * La barre et le point sont soustraits du triangle plutôt que posés dessus :
 * évidés, ils restent nets quelle que soit la couleur derrière, et le dégradé
 * de volume ne les traverse pas.
 */
private fun alertPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val corps = Path().apply {
        moveTo(w * 0.50f, h * 0.05f)
        lineTo(w * 0.99f, h * 0.92f)
        lineTo(w * 0.01f, h * 0.92f)
        close()
    }
    val creux = Path().apply {
        addRect(Rect(w * 0.43f, h * 0.38f, w * 0.57f, h * 0.67f))
        addRect(Rect(w * 0.42f, h * 0.73f, w * 0.58f, h * 0.88f))
    }
    return Path().apply { op(corps, creux, PathOperation.Difference) }
}

/** Un drapeau et sa hampe. */
private fun flagPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        // La hampe.
        moveTo(w * 0.10f, h * 0.04f)
        lineTo(w * 0.22f, h * 0.04f)
        lineTo(w * 0.22f, h * 0.96f)
        lineTo(w * 0.10f, h * 0.96f)
        close()
        // Le pan, creusé en bas comme une flamme.
        moveTo(w * 0.22f, h * 0.10f)
        lineTo(w * 0.92f, h * 0.10f)
        lineTo(w * 0.74f, h * 0.34f)
        lineTo(w * 0.92f, h * 0.58f)
        lineTo(w * 0.22f, h * 0.58f)
        close()
    }
}

/** Deux feuillets décalés. */
private fun copyPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val r = w * 0.10f
    val derriere = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.06f, top = h * 0.06f,
                right = w * 0.66f, bottom = h * 0.66f,
                radiusX = r, radiusY = r
            )
        )
    }
    val devant = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.34f, top = h * 0.34f,
                right = w * 0.94f, bottom = h * 0.94f,
                radiusX = r, radiusY = r
            )
        )
    }
    // Le feuillet du dessous est évidé là où l'autre le recouvre : sans ça les
    // deux se fondent en une seule forme.
    val creux = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.26f, top = h * 0.26f,
                right = w * 0.94f, bottom = h * 0.94f,
                radiusX = r, radiusY = r
            )
        )
    }
    val visible = Path().apply { op(derriere, creux, PathOperation.Difference) }
    return Path().apply { op(visible, devant, PathOperation.Union) }
}

/** Une flèche qui repart vers la gauche après un coude. */
private fun replyPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        // La pointe.
        moveTo(w * 0.04f, h * 0.44f)
        lineTo(w * 0.38f, h * 0.14f)
        lineTo(w * 0.38f, h * 0.74f)
        close()
        // Le corps coudé.
        moveTo(w * 0.30f, h * 0.32f)
        lineTo(w * 0.62f, h * 0.32f)
        cubicTo(w * 0.96f, h * 0.32f, w * 0.96f, h * 0.92f, w * 0.62f, h * 0.92f)
        lineTo(w * 0.48f, h * 0.92f)
        lineTo(w * 0.48f, h * 0.78f)
        lineTo(w * 0.62f, h * 0.78f)
        cubicTo(w * 0.78f, h * 0.78f, w * 0.78f, h * 0.46f, w * 0.62f, h * 0.46f)
        lineTo(w * 0.30f, h * 0.46f)
        close()
    }
}

/** Deux silhouettes, la seconde en retrait. */
private fun peoplePath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        // Celle de derrière, plus petite et décalée.
        addOval(Rect(w * 0.52f, h * 0.14f, w * 0.84f, h * 0.46f))
        moveTo(w * 0.50f, h * 0.92f)
        cubicTo(w * 0.62f, h * 0.54f, w * 0.98f, h * 0.58f, w * 0.98f, h * 0.92f)
        close()
        // Celle de devant.
        addOval(Rect(w * 0.14f, h * 0.08f, w * 0.54f, h * 0.48f))
        moveTo(w * 0.02f, h * 0.94f)
        cubicTo(w * 0.02f, h * 0.56f, w * 0.66f, h * 0.56f, w * 0.66f, h * 0.94f)
        close()
    }
}

/** Un livre ouvert : deux pages et une reliure creusée. */
private fun bookPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.26f)
        cubicTo(w * 0.34f, h * 0.12f, w * 0.16f, h * 0.14f, w * 0.05f, h * 0.18f)
        lineTo(w * 0.05f, h * 0.82f)
        cubicTo(w * 0.16f, h * 0.78f, w * 0.34f, h * 0.76f, w * 0.50f, h * 0.90f)
        cubicTo(w * 0.66f, h * 0.76f, w * 0.84f, h * 0.78f, w * 0.95f, h * 0.82f)
        lineTo(w * 0.95f, h * 0.18f)
        cubicTo(w * 0.84f, h * 0.14f, w * 0.66f, h * 0.12f, w * 0.50f, h * 0.26f)
        close()
    }
}

/** Un crayon en diagonale, pointe en bas à gauche. */
private fun pencilPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.06f, h * 0.94f)
        lineTo(w * 0.24f, h * 0.86f)
        lineTo(w * 0.90f, h * 0.20f)
        lineTo(w * 0.78f, h * 0.08f)
        lineTo(w * 0.12f, h * 0.74f)
        close()
    }
}

/** Un ballon : un disque et deux méridiens creusés. */
private fun ballPath(s: Size): Path {
    val plein = Path().apply { addOval(Rect(0f, 0f, s.width, s.height)) }
    val meridien = Path().apply {
        addOval(
            Rect(
                s.width * 0.34f, -s.height * 0.06f,
                s.width * 0.66f, s.height * 1.06f
            )
        )
    }
    return Path().apply { op(plein, meridien, PathOperation.Difference) }
}

/** Le masque de théâtre : un ovale et deux yeux évidés. */
private fun dramaPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val visage = Path().apply {
        moveTo(w * 0.50f, h * 0.04f)
        cubicTo(w * 0.92f, h * 0.04f, w * 0.94f, h * 0.44f, w * 0.82f, h * 0.72f)
        cubicTo(w * 0.72f, h * 0.96f, w * 0.28f, h * 0.96f, w * 0.18f, h * 0.72f)
        cubicTo(w * 0.06f, h * 0.44f, w * 0.08f, h * 0.04f, w * 0.50f, h * 0.04f)
        close()
    }
    val yeux = Path().apply {
        addOval(Rect(w * 0.26f, h * 0.32f, w * 0.44f, h * 0.50f))
        addOval(Rect(w * 0.56f, h * 0.32f, w * 0.74f, h * 0.50f))
    }
    return Path().apply { op(visage, yeux, PathOperation.Difference) }
}

/** Un cadenas : anse et corps. */
private fun lockPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val corps = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.14f, top = h * 0.44f,
                right = w * 0.86f, bottom = h * 0.96f,
                radiusX = w * 0.14f, radiusY = w * 0.14f
            )
        )
    }
    // L'anse est un anneau dont on ne garde que le haut.
    val anseExt = Path().apply {
        addOval(Rect(w * 0.24f, h * 0.06f, w * 0.76f, h * 0.62f))
    }
    val anseInt = Path().apply {
        addOval(Rect(w * 0.38f, h * 0.20f, w * 0.62f, h * 0.62f))
    }
    val anse = Path().apply { op(anseExt, anseInt, PathOperation.Difference) }
    return Path().apply { op(corps, anse, PathOperation.Union) }
}

/** Une clé : un anneau percé et un panneton. */
private fun keyPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val anneauExt = Path().apply {
        addOval(Rect(w * 0.04f, h * 0.16f, w * 0.56f, h * 0.68f))
    }
    val anneauInt = Path().apply {
        addOval(Rect(w * 0.18f, h * 0.30f, w * 0.42f, h * 0.54f))
    }
    val anneau = Path().apply { op(anneauExt, anneauInt, PathOperation.Difference) }
    val tige = Path().apply {
        moveTo(w * 0.48f, h * 0.36f)
        lineTo(w * 0.96f, h * 0.36f)
        lineTo(w * 0.96f, h * 0.50f)
        lineTo(w * 0.86f, h * 0.50f)
        lineTo(w * 0.86f, h * 0.64f)
        lineTo(w * 0.74f, h * 0.64f)
        lineTo(w * 0.74f, h * 0.50f)
        lineTo(w * 0.48f, h * 0.50f)
        close()
    }
    return Path().apply { op(anneau, tige, PathOperation.Union) }
}

/** Une balance : un fléau, deux plateaux, un pied. */
private fun scalePath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        // Le fléau, le mât et le socle, d'un seul trait. Mât aminci et socle
        // resserré : les deux dominaient au point que la silhouette lisait
        // « T » posé sur une dalle.
        moveTo(w * 0.04f, h * 0.18f)
        lineTo(w * 0.96f, h * 0.18f)
        lineTo(w * 0.96f, h * 0.27f)
        lineTo(w * 0.54f, h * 0.27f)
        lineTo(w * 0.54f, h * 0.76f)
        lineTo(w * 0.70f, h * 0.76f)
        lineTo(w * 0.70f, h * 0.88f)
        lineTo(w * 0.30f, h * 0.88f)
        lineTo(w * 0.30f, h * 0.76f)
        lineTo(w * 0.46f, h * 0.76f)
        lineTo(w * 0.46f, h * 0.27f)
        lineTo(w * 0.04f, h * 0.27f)
        close()
        // Les coupes, en demi-cercles. Un triangle pointe vers le bas se lit
        // comme une pointe de flèche, et collé au fléau il s'y fondait ; une
        // coupe se lit comme un plateau, et tient dès 15 dp.
        val r = w * 0.19f
        arcTo(Rect(w * 0.02f, h * 0.42f - r, w * 0.40f, h * 0.42f + r), 0f, 180f, true)
        close()
        arcTo(Rect(w * 0.60f, h * 0.42f - r, w * 0.98f, h * 0.42f + r), 0f, 180f, true)
        close()
    }
}

/** Trois barres de hauteurs croissantes. */
private fun chartPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val r = w * 0.06f
    return Path().apply {
        listOf(
            Triple(0.06f, 0.58f, 0.26f),
            Triple(0.38f, 0.34f, 0.58f),
            Triple(0.70f, 0.14f, 0.90f)
        ).forEach { (gauche, haut, droite) ->
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * gauche, top = h * haut,
                    right = w * droite, bottom = h * 0.94f,
                    radiusX = r, radiusY = r
                )
            )
        }
    }
}

/** Deux anneaux entrelacés. */
private fun linkPath(s: Size): Path {
    val w = s.width
    val h = s.height
    fun anneau(g: Float, d: Float): Path {
        val ext = Path().apply { addOval(Rect(w * g, h * 0.26f, w * d, h * 0.74f)) }
        val int = Path().apply {
            addOval(
                Rect(
                    w * (g + 0.10f), h * 0.38f,
                    w * (d - 0.10f), h * 0.62f
                )
            )
        }
        return Path().apply { op(ext, int, PathOperation.Difference) }
    }
    return Path().apply {
        op(anneau(0.02f, 0.58f), anneau(0.42f, 0.98f), PathOperation.Union)
    }
}

/** Un crâne : une calotte, deux orbites, une mâchoire. */
private fun skullPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val tete = Path().apply {
        moveTo(w * 0.50f, h * 0.04f)
        cubicTo(w * 0.90f, h * 0.04f, w * 0.94f, h * 0.42f, w * 0.86f, h * 0.62f)
        lineTo(w * 0.72f, h * 0.62f)
        lineTo(w * 0.72f, h * 0.90f)
        lineTo(w * 0.28f, h * 0.90f)
        lineTo(w * 0.28f, h * 0.62f)
        lineTo(w * 0.14f, h * 0.62f)
        cubicTo(w * 0.06f, h * 0.42f, w * 0.10f, h * 0.04f, w * 0.50f, h * 0.04f)
        close()
    }
    val orbites = Path().apply {
        addOval(Rect(w * 0.22f, h * 0.30f, w * 0.44f, h * 0.52f))
        addOval(Rect(w * 0.56f, h * 0.30f, w * 0.78f, h * 0.52f))
    }
    return Path().apply { op(tete, orbites, PathOperation.Difference) }
}

/** Un cadre photo : un rectangle percé d'un disque. */
private fun photoPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val cadre = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.04f, top = h * 0.14f,
                right = w * 0.96f, bottom = h * 0.86f,
                radiusX = w * 0.12f, radiusY = w * 0.12f
            )
        )
    }
    val objectif = Path().apply {
        addOval(Rect(w * 0.36f, h * 0.34f, w * 0.64f, h * 0.66f))
    }
    return Path().apply { op(cadre, objectif, PathOperation.Difference) }
}

/** Un triangle de lecture. */
private fun playPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.18f, h * 0.08f)
        lineTo(w * 0.92f, h * 0.50f)
        lineTo(w * 0.18f, h * 0.92f)
        close()
    }
}

/** Quatre barres verticales, comme un niveau sonore. */
private fun wavePath(s: Size): Path {
    val w = s.width
    val h = s.height
    val r = w * 0.06f
    return Path().apply {
        listOf(
            0.34f to 0.66f,
            0.16f to 0.84f,
            0.24f to 0.76f,
            0.42f to 0.58f
        ).forEachIndexed { i, (haut, bas) ->
            val g = 0.08f + i * 0.22f
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * g, top = h * haut,
                    right = w * (g + 0.12f), bottom = h * bas,
                    radiusX = r, radiusY = r
                )
            )
        }
    }
}

/** Une feuille au coin replié. */
private fun docPath(s: Size): Path {
    val w = s.width
    val h = s.height
    return Path().apply {
        moveTo(w * 0.14f, h * 0.06f)
        lineTo(w * 0.62f, h * 0.06f)
        lineTo(w * 0.86f, h * 0.30f)
        lineTo(w * 0.86f, h * 0.94f)
        lineTo(w * 0.14f, h * 0.94f)
        close()
        // Le pli du coin, en creux.
        moveTo(w * 0.62f, h * 0.06f)
        lineTo(w * 0.62f, h * 0.30f)
        lineTo(w * 0.86f, h * 0.30f)
        close()
    }
}

/** Une loupe : un anneau et son manche. */
private fun searchPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val ext = Path().apply { addOval(Rect(w * 0.06f, h * 0.06f, w * 0.72f, h * 0.72f)) }
    val int = Path().apply { addOval(Rect(w * 0.20f, h * 0.20f, w * 0.58f, h * 0.58f)) }
    val anneau = Path().apply { op(ext, int, PathOperation.Difference) }
    val manche = Path().apply {
        moveTo(w * 0.58f, h * 0.68f)
        lineTo(w * 0.72f, h * 0.54f)
        lineTo(w * 0.96f, h * 0.78f)
        lineTo(w * 0.82f, h * 0.92f)
        close()
    }
    return Path().apply { op(anneau, manche, PathOperation.Union) }
}

/** Un billet aux flancs échancrés. */
private fun ticketPath(s: Size): Path {
    val w = s.width
    val h = s.height
    val corps = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = w * 0.04f, top = h * 0.22f,
                right = w * 0.96f, bottom = h * 0.78f,
                radiusX = w * 0.10f, radiusY = w * 0.10f
            )
        )
    }
    val encoches = Path().apply {
        addOval(Rect(w * 0.42f, h * 0.10f, w * 0.58f, h * 0.30f))
        addOval(Rect(w * 0.42f, h * 0.70f, w * 0.58f, h * 0.90f))
    }
    return Path().apply { op(corps, encoches, PathOperation.Difference) }
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
