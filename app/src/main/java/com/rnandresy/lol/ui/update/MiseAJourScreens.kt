package com.rnandresy.lol.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.theme.ScriptFont
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.EtatMiseAJour

/**
 * L'écran qui remplace toute l'app quand sa version est trop vieille.
 *
 * Il prend la place de la navigation entière, écran de connexion compris :
 * une version bloquée ne doit rien pouvoir écrire. Pas de bouton retour ni de
 * « plus tard » — c'est la différence avec [MiseAJourDisponibleDialog].
 *
 * La mise en page reprend celle de la connexion (logotype manuscrit, une carte
 * en bulle) pour que ce moment désagréable ait au moins l'air d'Askip, et pas
 * d'une erreur système.
 */
@Composable
fun MiseAJourRequiseScreen(etat: EtatMiseAJour.Requise) {
    val uriHandler = LocalUriHandler.current
    var lienEnEchec by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(Space.huge))
        Text(
            "Askip",
            fontFamily = ScriptFont,
            fontSize = 44.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(Space.xxl))

        BubbleCard(modifier = Modifier.fillMaxWidth(), elevation = 8.dp) {
            Column(
                modifier = Modifier.padding(Space.xl),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                AskipGlyph(GlyphKind.ALERT, size = 28.dp)
                Text(
                    "Cette version n'est plus prise en charge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    etat.message.ifBlank {
                        "Installe la nouvelle version pour continuer. Tes rumeurs, " +
                            "tes messages et ton clout t'attendent : ils sont gardés " +
                            "sur le serveur, pas dans l'app."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (etat.lien.isNotBlank()) {
                    BubbleButton(
                        text = "Télécharger la nouvelle version",
                        onClick = {
                            lienEnEchec = runCatching { uriHandler.openUri(etat.lien) }.isFailure
                        },
                        size = BubbleSize.LARGE,
                        fillWidth = true
                    )
                    if (lienEnEchec) {
                        // Aucun navigateur n'a pris le lien : on le montre en
                        // entier pour qu'il puisse être recopié à la main.
                        Text(
                            "Le lien ne s'ouvre pas sur ce téléphone. Recopie-le dans ton navigateur : ${etat.lien}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        "Demande le lien de la nouvelle version à l'administrateur du campus.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Pour l'administrateur, quand quelqu'un vient demander de l'aide.
                Text(
                    "Version installée : ${etat.versionInstallee} · minimum : ${etat.versionMinimale}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Space.huge))
    }
}

/**
 * L'annonce d'une nouvelle version, qui n'impose rien.
 *
 * Fermée, elle ne revient pas pour ce numéro de version pendant le lancement
 * en cours ; elle reviendra au prochain lancement, ou tout de suite si une
 * version encore plus récente est publiée entre-temps.
 */
@Composable
fun MiseAJourDisponibleDialog(
    etat: EtatMiseAJour.Disponible,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val aUnLien = etat.lien.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AskipGlyph(GlyphKind.SPARKLE, size = 24.dp) },
        title = { Text("Une nouvelle version d'Askip est sortie") },
        text = {
            Text(
                etat.message.ifBlank {
                    if (aUnLien) {
                        "Elle apporte des corrections et des nouveautés. Ta version " +
                            "marche encore, mais pense à l'installer."
                    } else {
                        "Elle apporte des corrections et des nouveautés. Demande le " +
                            "lien à l'administrateur du campus."
                    }
                }
            )
        },
        confirmButton = {
            if (aUnLien) {
                TextButton(onClick = {
                    runCatching { uriHandler.openUri(etat.lien) }
                    onDismiss()
                }) { Text("Télécharger") }
            } else {
                TextButton(onClick = onDismiss) { Text("Compris") }
            }
        },
        dismissButton = if (aUnLien) {
            { TextButton(onClick = onDismiss) { Text("Plus tard") } }
        } else {
            null
        }
    )
}
