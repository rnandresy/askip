package com.rnandresy.lol.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.rnandresy.lol.R

/**
 * L'écriture de l'app — « Sedgwick Ave ».
 *
 * Un feutre incliné, tracé à la main : c'est le registre de l'app, et c'est ce
 * qu'on cherchait. Les minuscules sont de vraies minuscules, donc le logotype
 * « Askip » garde sa forme — plusieurs manuscrites grasses n'existent qu'en
 * capitales et l'auraient transformé en « ASKIP ».
 *
 * Elle remplace « Shooting Star », qui avait deux défauts.
 *
 * Le premier, juridique : licence « usage personnel », incompatible avec une
 * app distribuée à de vrais utilisateurs.
 *
 * Le second se voyait à l'écran sans qu'on sache le nommer. Sa table `cmap`
 * ignorait les guillemets français, le tiret cadratin, les points de
 * suspension, l'apostrophe courbe — et le point médian, que l'app emploie
 * comme séparateur partout (« 06 mai · 09:27 »). Android les remplaçait un par
 * un depuis la police système : ces signes-là sortaient dans un autre dessin,
 * au milieu même des titres. Sedgwick Ave couvre tout le français.
 *
 * Vérifier avant de changer de police : lire sa `cmap` plutôt que se fier à
 * l'œil. Un glyphe manquant ne laisse pas de carré vide, il emprunte
 * discrètement une autre police, et ça ne se remarque qu'au bout de semaines.
 *
 * Licence SIL Open Font 1.1 — libre d'usage, y compris commercial. Texte
 * complet dans `licences/sedgwick-ave-OFL.txt`.
 *
 * C'est la seule police livrée avec le projet. Les figures ne passent plus par
 * une fonte : elles sont tracées en vectoriel dans `AskipGlyph`, ce qui les
 * rend identiques d'un téléphone à l'autre — ce qu'aucune police ne garantit.
 */
val ScriptFont = FontFamily(Font(R.font.sedgwick_ave))
