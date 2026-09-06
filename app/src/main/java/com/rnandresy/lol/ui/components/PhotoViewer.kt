package com.rnandresy.lol.ui.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.rnandresy.lol.ui.theme.Space

/** Les motifs de signalement d'une image. */
private val PHOTO_REPORTS = listOf(
    "Contenu choquant",
    "Nudité ou contenu sexuel",
    "Harcèlement ou intimidation",
    "Photo de quelqu'un sans son accord",
    "Autre"
)

/**
 * La photo en grand.
 *
 * Pincer pour zoomer, glisser pour se déplacer, double-tap pour revenir. Le
 * déplacement est borné à ce que le zoom a réellement débordé : sans cette
 * borne, on pousse l'image hors de l'écran et on la croit disparue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewer(
    url: String,
    onDismiss: () -> Unit,
    onReport: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showReport by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            // À l'échelle 1, l'image tient à l'écran : rien à
                            // déplacer, donc on ramène le décalage à zéro.
                            val maxX = (size.width * (scale - 1f)) / 2f
                            val maxY = (size.height * (scale - 1f)) / 2f
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Photo en grand",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            // ── Les commandes, en haut ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(Space.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BubbleIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Fermer",
                    onClick = onDismiss,
                    diameter = 36.dp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    BubbleIconButton(
                        icon = Icons.Rounded.Download,
                        contentDescription = "Télécharger",
                        onClick = { downloadImage(context, url) },
                        diameter = 36.dp
                    )
                    if (onReport != null) {
                        BubbleIconButton(
                            icon = Icons.Rounded.Flag,
                            contentDescription = "Signaler",
                            onClick = { showReport = true },
                            tone = BubbleTone.DANGER,
                            diameter = 36.dp
                        )
                    }
                }
            }

            if (scale > 1f) {
                Text(
                    "Double-tap pour revenir",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Space.xxl)
                )
            }
        }
    }

    if (showReport && onReport != null) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showReport = false },
            sheetState = state
        ) {
            Column(Modifier.padding(bottom = Space.xxl)) {
                SheetHeader(
                    "Signaler cette photo",
                    "L'administrateur du campus la recevra avec ton motif."
                )
                PHOTO_REPORTS.forEach { motif ->
                    SheetAction(emoji = "⚑", label = motif) {
                        onReport(motif)
                        showReport = false
                        onDismiss()
                    }
                }
            }
        }
    }
}

/**
 * Enregistre l'image dans les téléchargements de l'appareil.
 *
 * Passe par [DownloadManager] plutôt que d'écrire le fichier soi-même : c'est
 * lui qui gère la notification de progression et le rangement dans le dossier
 * public, sans demander la moindre permission depuis Android 10.
 */
private fun downloadImage(context: Context, url: String) {
    runCatching {
        val nom = "askip_${System.currentTimeMillis()}.jpg"
        val requete = DownloadManager.Request(Uri.parse(url))
            .setTitle(nom)
            .setDescription("Téléchargement depuis Askip")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, nom)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(requete)
        Toast.makeText(context, "Téléchargement lancé", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Téléchargement impossible", Toast.LENGTH_SHORT).show()
    }
}

/** Un titre de section pour la visionneuse. */
@Composable
private fun ViewerTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(Modifier.height(Space.xs))
}
