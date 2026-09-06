package com.rnandresy.lol.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.utils.imageAtWidth

/**
 * Convertir une largeur d'affichage en demande de livraison.
 *
 * `Dp` est une mesure d'écran, Cloudinary compte en pixels : le pont se fait
 * ici, une fois, plutôt qu'à chaque appel d'image.
 */

/** L'URL de [url], demandée à la largeur qu'occupera vraiment l'image. */
@Composable
fun sizedUrl(url: String?, width: Dp): String? {
    if (url.isNullOrBlank()) return url
    val px = with(LocalDensity.current) { width.roundToPx() }
    return remember(url, px) { imageAtWidth(url, px) }
}

/**
 * Idem, pour une image qui occupe toute la largeur de l'écran — la photo d'une
 * rumeur, une couverture de profil.
 */
@Composable
fun fullWidthUrl(url: String?): String? =
    sizedUrl(url, LocalConfiguration.current.screenWidthDp.dp)

/**
 * La visionneuse plein écran, qui autorise le zoom.
 *
 * On demande le double de la largeur d'écran : assez pour que l'agrandissement
 * reste net, tout en profitant de `f_auto` et `q_auto`, qui allègent déjà
 * beaucoup sans qu'on touche aux dimensions.
 */
@Composable
fun zoomableUrl(url: String?): String? =
    sizedUrl(url, (LocalConfiguration.current.screenWidthDp * 2).dp)
