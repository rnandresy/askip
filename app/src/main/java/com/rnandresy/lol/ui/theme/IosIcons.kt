package com.rnandresy.lol.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.R

/**
 * La fonte d'icônes iOS livrée avec le projet.
 *
 * **Ce n'est pas une police de texte.** Elle ne contient ni lettre, ni chiffre,
 * ni ponctuation : 52 dessins seulement, rangés de U+E900 à U+E933 dans la zone
 * à usage privé. La poser comme police de l'app ferait disparaître tout le
 * texte — c'est vérifiable dans sa table `cmap`, qui ne déclare que cette
 * plage, l'espace et le caractère de remplacement.
 *
 * Elle sert donc là où elle est bonne : remplacer les icônes vectorielles
 * Material par des dessins de facture iPhone, plus fins et plus réguliers.
 */
val IosFont = FontFamily(Font(R.font.ios_icons))

/**
 * Les 52 dessins, par leur position dans la police.
 *
 * Les noms sont pour l'instant numérotés : la police ne porte aucune table de
 * noms de glyphes (`post` en version 3.0), il est donc impossible de savoir
 * depuis le fichier quel dessin correspond à quel code. Ouvrir
 * `tools/apercu-icones.html` dans un navigateur les affiche tous avec leur
 * code — il suffit alors de renommer les constantes ci-dessous.
 */
object IosIcons {
    /** Le premier code de la plage. */
    const val FIRST = 0xE900

    /** Le dernier code de la plage. */
    const val LAST = 0xE933

    /** Les 52 codes, dans l'ordre. */
    val ALL: List<String> = (FIRST..LAST).map { it.toChar().toString() }

    /** Le dessin numéro [index], à partir de zéro. */
    fun at(index: Int): String = ALL.getOrElse(index) { "" }
}

/**
 * Un dessin de la fonte iOS, posé comme du texte.
 *
 * Il prend la couleur qu'on lui donne — celle du texte par défaut — donc il
 * suit les neuf thèmes sans qu'on ait à s'en occuper.
 */
@Composable
fun IosIcon(
    glyph: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    tint: Color = Color.Unspecified
) {
    Text(
        text = glyph,
        modifier = modifier,
        fontFamily = IosFont,
        fontSize = size,
        color = if (tint == Color.Unspecified) LocalTextStyle.current.color else tint
    )
}
