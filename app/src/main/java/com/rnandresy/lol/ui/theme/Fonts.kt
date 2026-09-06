package com.rnandresy.lol.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.rnandresy.lol.R

/**
 * L'écriture de l'app — « Titan One », de Rodrigo Fuenzalida.
 *
 * Une grasse d'affiche, ronde et franche : assez de caractère pour porter le
 * logotype et les titres, assez lisible pour tenir à 15 sp, là où une brosse
 * fine se serait effondrée.
 *
 * Elle remplace « Shooting Star », qui avait deux défauts. Le premier était
 * juridique : licence « usage personnel », incompatible avec une app
 * distribuée à de vrais utilisateurs. Le second, visible à l'écran — il lui
 * manquait les guillemets français, le tiret cadratin et les points de
 * suspension, trois signes que l'app emploie partout. Android les remplaçait
 * un par un depuis la police système, et ces mots-là sortaient dans un autre
 * dessin que le reste du titre. Titan One couvre tout le français, accents
 * compris.
 *
 * Licence SIL Open Font 1.1 : libre d'usage, y compris commercial, à la seule
 * condition de ne pas la revendre seule et de garder ce nom de police
 * réservé. Le texte complet est dans `licences/titan-one-OFL.txt`.
 *
 * C'est la seule police livrée avec le projet. Les figures ne passent plus par
 * une fonte : elles sont tracées en vectoriel dans `AskipGlyph`, ce qui les
 * rend identiques d'un téléphone à l'autre — ce qu'aucune police ne garantit.
 */
val ScriptFont = FontFamily(Font(R.font.titan_one))
