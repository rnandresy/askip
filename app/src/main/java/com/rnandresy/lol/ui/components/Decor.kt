package com.rnandresy.lol.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.LocalReduceMotion
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ═════════════════════════════════════════════════════════════════════════════
//  Le reflet des bulles
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le dégradé posé sur le haut d'une bulle.
 *
 * C'est ce voile clair, concentré sur le premier tiers, qui donne à un bouton
 * l'aspect d'une pastille de verre plutôt que d'un rectangle coloré. Il est
 * volontairement très court : au-delà du tiers, le reflet devient un dégradé
 * et le bouton perd son relief.
 */
@Composable
fun glossBrush(intensity: Float = 1f): Brush {
    val gloss = LocalAskipPalette.current.gloss
    return Brush.verticalGradient(
        0f to gloss.copy(alpha = gloss.alpha * intensity),
        0.35f to gloss.copy(alpha = gloss.alpha * 0.25f * intensity),
        0.36f to Color.Transparent,
        1f to Color.Transparent
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  Poussière d'étoiles
// ═════════════════════════════════════════════════════════════════════════════

private data class Speck(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float
)

/**
 * Une poussière lumineuse qui scintille lentement.
 *
 * Posée derrière les écrans d'accueil et sous les grands titres. Les positions
 * sont tirées une fois pour toutes à partir d'une graine fixe : sans ça, la
 * poussière se redistribuerait à chaque recomposition et scintillerait de
 * façon désagréable.
 */
@Composable
fun StarDust(
    modifier: Modifier = Modifier,
    count: Int = 22,
    seed: Int = 7,
    tint: Color? = null
) {
    val palette = LocalAskipPalette.current
    val color = tint ?: palette.sparkle
    val reduceMotion = LocalReduceMotion.current

    val specks = remember(count, seed) {
        val rng = Random(seed)
        List(count) {
            Speck(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                radius = 0.8f + rng.nextFloat() * 1.9f,
                phase = rng.nextFloat() * 2f * PI.toFloat(),
                speed = 0.6f + rng.nextFloat() * 0.8f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "dust")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dustPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        specks.forEach { speck ->
            // sin() ramène l'éclat entre 0,15 et 1 : aucune poussière ne
            // disparaît complètement, ce qui éviterait l'effet « clignotant ».
            val glow = 0.15f + 0.85f * ((sin(t * speck.speed + speck.phase) + 1f) / 2f)
            drawCircle(
                color = color.copy(alpha = 0.55f * glow),
                radius = speck.radius,
                center = Offset(speck.x * size.width, speck.y * size.height)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Pétales de cerisier
// ═════════════════════════════════════════════════════════════════════════════

private data class Petal(
    val x: Float,
    val startY: Float,
    val size: Float,
    val sway: Float,
    val spin: Float,
    val fall: Float
)

/** Le contour d'un pétale : deux courbes symétriques, pointe en haut. */
private fun petalPath(w: Float, h: Float): Path = Path().apply {
    moveTo(w / 2f, 0f)
    cubicTo(w, h * 0.18f, w, h * 0.72f, w / 2f, h)
    cubicTo(0f, h * 0.72f, 0f, h * 0.18f, w / 2f, 0f)
    close()
}

/**
 * Des pétales qui tombent en se balançant.
 *
 * Chaque pétale descend à sa vitesse, dérive horizontalement selon un sinus et
 * tourne sur lui-même : trois mouvements décorrélés, ce qui suffit à faire
 * oublier qu'ils ne sont qu'une douzaine.
 *
 * Entièrement coupé si « Réduire les animations » est actif — un décor n'a
 * jamais le droit de gêner quelqu'un.
 */
@Composable
fun SakuraFall(
    modifier: Modifier = Modifier,
    count: Int = 12,
    seed: Int = 3,
    alpha: Float = 0.55f
) {
    val palette = LocalAskipPalette.current
    if (LocalReduceMotion.current) return

    val petals = remember(count, seed) {
        val rng = Random(seed)
        List(count) {
            Petal(
                x = rng.nextFloat(),
                startY = rng.nextFloat(),
                size = 7f + rng.nextFloat() * 9f,
                sway = 0.4f + rng.nextFloat() * 1.4f,
                spin = (if (rng.nextBoolean()) 1f else -1f) * (0.5f + rng.nextFloat()),
                fall = 0.5f + rng.nextFloat() * 0.7f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "sakura")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sakuraFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        petals.forEach { petal -> drawPetal(petal, t, palette.petal, alpha) }
    }
}

private fun DrawScope.drawPetal(petal: Petal, t: Float, color: Color, alpha: Float) {
    // Le modulo fait repartir le pétale en haut dès qu'il sort par le bas,
    // sans jamais interrompre l'animation.
    val progress = (petal.startY + t * petal.fall) % 1f
    val y = progress * (size.height + 40f) - 20f
    val drift = sin(progress * 2f * PI.toFloat() * petal.sway) * size.width * 0.06f
    val x = petal.x * size.width + drift

    val w = petal.size
    val h = petal.size * 1.35f

    // Les pétales s'estompent en haut et en bas : ils entrent et sortent du
    // cadre sans apparition brutale.
    val edgeFade = when {
        progress < 0.08f -> progress / 0.08f
        progress > 0.92f -> (1f - progress) / 0.08f
        else -> 1f
    }

    translate(left = x, top = y) {
        rotate(degrees = t * 360f * petal.spin, pivot = Offset(w / 2f, h / 2f)) {
            drawPath(
                path = petalPath(w, h),
                color = color.copy(alpha = alpha * edgeFade)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Halo
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Un halo diffus, pour poser un point lumineux derrière un titre ou un avatar.
 * Sans lui, les grands aplats crème paraissent plats.
 */
@Composable
fun softGlow(color: Color, strength: Float = 0.28f): Brush = Brush.radialGradient(
    0f to color.copy(alpha = strength),
    0.6f to color.copy(alpha = strength * 0.35f),
    1f to Color.Transparent
)


// ═════════════════════════════════════════════════════════════════════════════
//  Barres
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le liseré d'une barre : en bas pour celle du haut, en haut pour celle du bas.
 *
 * Sans lui, la barre et le contenu qui défile dessous se confondent — surtout
 * sur les thèmes clairs, où les deux fonds sont presque de la même teinte.
 */
@Composable
fun Modifier.barEdge(top: Boolean = false): Modifier {
    val line = LocalAskipPalette.current.bubbleBorder
    return this.drawWithContent {
        drawContent()
        val y = if (top) 0f else size.height
        drawLine(
            color = line,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * Le fond de l'app : la couleur du thème, sa poussière d'étoiles, ses pétales.
 *
 * Posé une seule fois derrière toute la navigation, il évite d'avoir à
 * saupoudrer chaque écran — et garantit que le décor est le même partout.
 * Les pétales ne tombent que sur les thèmes clairs : sur fond noir ils
 * mangent le texte au lieu de l'habiller.
 */
@Composable
fun AskipBackdrop(modifier: Modifier = Modifier) {
    val palette = LocalAskipPalette.current
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StarDust(count = 26, seed = 3)
        if (palette.isLight) SakuraFall(count = 10, seed = 17, alpha = 0.30f)
    }
}
