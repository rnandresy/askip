package com.rnandresy.lol.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.rnandresy.lol.R

/**
 * L'écriture de l'app — « Shooting Star », une main manuscrite.
 *
 * Elle couvre l'ASCII et tous les accents français, ce qui suffit pour les
 * titres. Il lui manque en revanche les guillemets français, le tiret cadratin
 * et les points de suspension — trois signes que l'app emploie beaucoup.
 * Android les remplace glyphe par glyphe depuis la police système, donc rien
 * ne casse : ils sortent simplement dans un autre dessin. C'est une raison de
 * plus de la réserver aux titres, où ces signes ne figurent pas.
 *
 * C'est la seule police livrée avec le projet. Les icônes ne passent plus par
 * une fonte : elles sont tracées en vectoriel dans `AskipGlyph`, ce qui les
 * rend identiques d'un téléphone à l'autre — ce qu'aucune police ne garantit.
 *
 * Attention, licence « usage personnel » : l'auteur exige une licence
 * commerciale pour toute diffusion qui en relève. Voir le READ ME livré avec
 * la police.
 */
val ScriptFont = FontFamily(Font(R.font.shooting_star))
