package com.rnandresy.lol.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.OathDialog
import com.rnandresy.lol.ui.components.ProgressTrack
import com.rnandresy.lol.ui.components.glyphForPostFormat
import com.rnandresy.lol.ui.components.glyphForTag
import com.rnandresy.lol.ui.feed.MentionTextField
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.MAX_TAGS_PER_POST
import com.rnandresy.lol.utils.RUMOR_TAGS
import com.rnandresy.lol.utils.SEAL_DURATIONS_HOURS
import com.rnandresy.lol.utils.SEAL_KEYS_TO_OPEN
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import kotlinx.coroutines.delay
import java.io.File

/**
 * L'écran de publication.
 *
 * Six formats, des pièces jointes, des salons, une durée de vie : tout tenait
 * dans une colonne où chaque format posait son propre encart d'explication.
 * Les règles sont désormais rassemblées dans une table de formats, les
 * réglages secondaires dans une seule carte, et chaque commande est une bulle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    vm: AskipViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    /** Type présélectionné — la Page de Vérité ouvre directement sur « vérité ». */
    initialType: String = "normal"
) {
    val loading by vm.loading.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()
    val isRecording by vm.isRecording.collectAsState()
    val recordingSecs by vm.recordingSeconds.collectAsState()
    val allProfiles by vm.allProfiles.collectAsState()
    val myProfile by vm.myProfile.collectAsState()
    val context = LocalContext.current

    var postType by remember { mutableStateOf(initialType) }
    var showOath by remember { mutableStateOf(false) }
    var contentTfv by remember { mutableStateOf(TextFieldValue("")) }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var audioDurSec by remember { mutableStateOf(0) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var ephemeral by remember { mutableStateOf(false) }
    var sealedText by remember { mutableStateOf("") }
    var sealHours by remember { mutableStateOf(SEAL_DURATIONS_HOURS[2]) }

    val content = contentTfv.text
    val isAdminUser = isAdmin(vm.currentUserId) || myProfile?.isAdmin == true

    val canPost = when (postType) {
        "poll" -> content.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank()
        // Une capsule sans contenu scellé n'aurait rien à révéler.
        "sealed" -> sealedText.isNotBlank()
        "chain" -> content.isNotBlank()
        "truth" -> content.isNotBlank()
        else -> content.isNotBlank() || imageUri != null || videoUri != null ||
            audioFile != null || fileUri != null
    }

    fun clearMedia() {
        imageUri = null; videoUri = null; audioFile = null; fileUri = null; fileName = ""
    }

    fun publish() {
        vm.createPostWithMedia(
            content = content.trim(),
            type = postType,
            pollOpt1 = opt1.trim(),
            pollOpt2 = opt2.trim(),
            imageUri = imageUri,
            videoUri = videoUri,
            audioFile = audioFile,
            audioSeconds = audioDurSec,
            fileUri = fileUri,
            tags = tags,
            ephemeral = ephemeral,
            sealedContent = if (postType == "sealed") sealedText else "",
            sealHours = if (postType == "sealed") sealHours else 0,
            isChain = postType == "chain"
        )
        onDone()
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { clearMedia(); imageUri = it } }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { clearMedia(); videoUri = it } }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            clearMedia()
            fileUri = it
            fileName = it.lastPathSegment?.substringAfterLast('/') ?: "fichier"
        }
    }

    val format = FORMATS.firstOrNull { it.type == postType } ?: FORMATS.first()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text(format.screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(Modifier.padding(start = Space.md)) {
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onBack,
                            enabled = !loading
                        )
                    }
                },
                actions = {
                    Box(Modifier.padding(end = Space.lg)) {
                        BubbleButton(
                            // Sur la Page de Vérité, on ne publie pas : on jure.
                            text = if (postType == "truth") "Jurer" else "Publier",
                            glyph = if (postType == "truth") GlyphKind.SCALE else null,
                            onClick = {
                                if (postType == "truth") showOath = true else publish()
                            },
                            enabled = canPost && !loading,
                            loading = loading,
                            tone = BubbleTone.PRIMARY,
                            size = BubbleSize.SMALL
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            // ── Le format ────────────────────────────────────────────────────
            // Six formats ne tiennent pas sur une ligne : la rangée défile.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                contentPadding = PaddingValues(vertical = Space.xs)
            ) {
                items(FORMATS) { f ->
                    BubbleChip(
                        label = f.label,
                        glyph = glyphForPostFormat(f.type),
                        filled = postType == f.type,
                        accent = if (postType == f.type) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { postType = f.type; clearMedia() }
                    )
                }
            }

            // La règle du format choisi, quand il en a une. Un seul encart :
            // avant, chaque format posait son propre bloc d'explication.
            format.note?.let { note -> FormatNote(glyph = glyphForPostFormat(format.type), text = note) }

            // ── Le texte ─────────────────────────────────────────────────────
            MentionTextField(
                value = contentTfv,
                onValueChange = { if (it.text.length <= 500) contentTfv = it },
                allProfiles = allProfiles,
                currentUserId = vm.currentUserId,
                isAdminUser = isAdminUser,
                placeholder = when (postType) {
                    "poll" -> "De quoi parle ce sondage ?"
                    "confession" -> "Ta confession (anonyme)…"
                    "sealed" -> "Aperçu visible avant l'ouverture (facultatif)…"
                    "chain" -> "La première phrase de la chaîne…"
                    "truth" -> "Énonce les faits. Rien que les faits."
                    else -> "Askip… qu'est-ce qui se passe ?"
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (content.isNotEmpty()) {
                Text(
                    "${content.length}/500",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (content.length > 450) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // ── La Capsule scellée ───────────────────────────────────────────
            if (postType == "sealed") {
                FormCard("Contenu scellé", GlyphKind.LOCK) {
                    FormField(
                        value = sealedText,
                        onValueChange = { sealedText = it },
                        placeholder = "Ce que personne ne doit lire tout de suite…",
                        minLines = 3,
                        maxLines = 8
                    )
                    Text(
                        "Ouverture dans",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        items(SEAL_DURATIONS_HOURS) { h ->
                            BubbleChip(
                                label = when {
                                    h >= 168 -> "1 semaine"
                                    h >= 24 -> "${h / 24} j"
                                    else -> "$h h"
                                },
                                filled = sealHours == h,
                                accent = if (sealHours == h) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { sealHours = h }
                            )
                        }
                    }
                    Text(
                        "$SEAL_KEYS_TO_OPEN clés du campus l'ouvriront plus tôt.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Les deux camps du sondage ────────────────────────────────────
            if (postType == "poll") {
                FormCard("Les deux camps", GlyphKind.CHART) {
                    FormField(
                        value = opt1,
                        onValueChange = { if (it.length <= 60) opt1 = it },
                        label = "Option A",
                        singleLine = true
                    )
                    FormField(
                        value = opt2,
                        onValueChange = { if (it.length <= 60) opt2 = it },
                        label = "Option B",
                        singleLine = true
                    )
                }
            }

            // ── Les pièces jointes ───────────────────────────────────────────
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .border(
                            1.dp,
                            LocalAskipPalette.current.bubbleBorder,
                            RoundedCornerShape(Radius.md)
                        )
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.align(Alignment.TopEnd).padding(Space.sm)) {
                        BubbleIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Retirer l'image",
                            onClick = { imageUri = null },
                            tone = BubbleTone.DANGER,
                            diameter = 30.dp
                        )
                    }
                }
            }

            if (videoUri != null) {
                AttachmentRow(glyph = GlyphKind.PLAY, label = "Vidéo sélectionnée") { videoUri = null }
            }

            if (audioFile != null) {
                AttachmentRow(
                    glyph = GlyphKind.PEOPLE,
                    label = "Note vocale (${formatDuration(audioDurSec)})"
                ) { audioFile = null; audioDurSec = 0 }
            }

            if (fileUri != null) {
                AttachmentRow(
                    glyph = GlyphKind.DOC,
                    label = fileName.take(28).ifBlank { "Fichier" }
                ) { fileUri = null; fileName = "" }
            }

            // ── Joindre quelque chose ────────────────────────────────────────
            if (postType == "normal") {
                if (!isRecording) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        item {
                            BubbleButton(
                                text = "Photo",
                                glyph = GlyphKind.PHOTO,
                                tone = BubbleTone.SOFT,
                                size = BubbleSize.SMALL,
                                onClick = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                        }
                        item {
                            BubbleButton(
                                text = "Vidéo",
                                glyph = GlyphKind.PLAY,
                                tone = BubbleTone.SOFT,
                                size = BubbleSize.SMALL,
                                onClick = {
                                    videoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.VideoOnly
                                        )
                                    )
                                }
                            )
                        }
                        item {
                            BubbleButton(
                                text = "Fichier",
                                glyph = GlyphKind.DOC,
                                tone = BubbleTone.SOFT,
                                size = BubbleSize.SMALL,
                                onClick = { filePicker.launch("*/*") }
                            )
                        }
                        if (audioFile == null) {
                            item {
                                BubbleButton(
                                    text = "Voix",
                                    glyph = GlyphKind.PEOPLE,
                                    tone = BubbleTone.SOFT,
                                    size = BubbleSize.SMALL,
                                    onClick = { vm.startVoiceRecording(context) }
                                )
                            }
                        }
                    }
                }

                // L'enregistrement en cours prend toute la largeur : c'est la
                // seule chose qu'on fait à cet instant.
                if (isRecording && audioFile == null) {
                    var dotVisible by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(500L)
                            dotVisible = !dotVisible
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = Space.lg, vertical = Space.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.sm)
                        ) {
                            if (dotVisible) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(Radius.pill))
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            } else {
                                Spacer(Modifier.size(8.dp))
                            }
                            Text(
                                "${formatDuration(recordingSecs)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        BubbleIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Annuler l'enregistrement",
                            onClick = { vm.cancelVoiceRecording() },
                            tone = BubbleTone.DANGER
                        )
                        BubbleButton(
                            text = "Joindre",
                            onClick = {
                                vm.stopRecordingForPost()?.let { (file, dur) ->
                                    audioFile = file
                                    audioDurSec = dur
                                }
                            },
                            tone = BubbleTone.PRIMARY,
                            size = BubbleSize.SMALL
                        )
                    }
                }
            }

            // ── Le rangement : salon et durée de vie ─────────────────────────
            // Deux réglages secondaires qui allaient ensemble sans jamais être
            // regroupés : ils partagent maintenant une seule carte.
            FormCard(
                title = if (tags.isEmpty()) "Ranger la rumeur"
                else "Salons (${tags.size}/$MAX_TAGS_PER_POST)",
                glyph = GlyphKind.DOC
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    items(RUMOR_TAGS) { def ->
                        val selected = def.slug in tags
                        BubbleChip(
                            label = def.label,
                            glyph = glyphForTag(def.slug),
                            filled = selected,
                            accent = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                tags = when {
                                    selected -> tags - def.slug
                                    tags.size < MAX_TAGS_PER_POST -> tags + def.slug
                                    else -> tags
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AskipGlyph(kind = GlyphKind.MOON, size = 16.dp)
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text("Rumeur éphémère", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Disparaît du fil après 24 h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = ephemeral, onCheckedChange = { ephemeral = it })
                }
            }

            // ── L'envoi en cours ─────────────────────────────────────────────
            if (loading && uploadProgress in 1..99) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    ProgressTrack(progress = uploadProgress / 100f, height = 6.dp)
                    Text(
                        "Envoi : $uploadProgress %",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.size(Space.huge))
        }
    }

    if (showOath) {
        OathDialog(
            onConfirm = { showOath = false; publish() },
            onDismiss = { showOath = false }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Les formats
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Un format de publication.
 *
 * Titre d'écran, libellé de puce et règle du jeu au même endroit : trois
 * `when` séparés finissaient toujours par diverger quand un format bougeait.
 */
private data class PostFormat(
    val type: String,
    val label: String,
    val screenTitle: String,
    val note: String? = null
)

private val FORMATS = listOf(
    PostFormat("normal", "Rumeur", "Nouvelle rumeur"),
    PostFormat("poll", "Sondage", "Sondage"),
    PostFormat(
        "confession", "Confession", "Confession",
        "Ton identité sera très bien cachée."
    ),
    PostFormat(
        "sealed", "Capsule", "Capsule scellée",
        "Personne ne pourra lire le contenu scellé avant l'heure — pas même " +
            "en fouillant la base. Le texte du haut, lui, reste visible : " +
            "c'est ton teaser."
    ),
    PostFormat(
        "chain", "Chaîne", "Téléphone arabe",
        "Tu écris la première phrase. Six autres personnes pourront ajouter " +
            "la leur, une seule chacune. La rumeur grandira sans toi."
    ),
    PostFormat(
        "truth", "Vérité", "Page de Vérité",
        "Ta publication sera faite sous serment. Le campus tranchera : un " +
            "serment démenti est enregistré comme parjure au registre."
    )
)

// ═════════════════════════════════════════════════════════════════════════════
//  Briques
// ═════════════════════════════════════════════════════════════════════════════

/** La règle du format choisi. */
@Composable
private fun FormatNote(glyph: GlyphKind, text: String) {
    BubbleCard(modifier = Modifier.fillMaxWidth(), gloss = 0.3f) {
        Row(
            modifier = Modifier.padding(Space.md),
            verticalAlignment = Alignment.Top
        ) {
            AskipGlyph(kind = glyph, size = 16.dp)
            Spacer(Modifier.width(Space.md))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Un groupe de champs, titré. */
@Composable
private fun FormCard(
    title: String,
    glyph: GlyphKind,
    content: @Composable ColumnScope.() -> Unit
) {
    BubbleCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                AskipGlyph(kind = glyph, size = 14.dp)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

/** Le champ de formulaire de l'écran : même forme, mêmes couleurs partout. */
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4
) {
    val palette = LocalAskipPalette.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.sm),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = palette.bubbleBorder,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** Une pièce jointe attachée, avec son bouton pour la retirer. */
@Composable
private fun AttachmentRow(glyph: GlyphKind, label: String, onRemove: () -> Unit) {
    BubbleCard(modifier = Modifier.fillMaxWidth(), gloss = 0.3f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            AskipGlyph(kind = glyph, size = 16.dp)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            BubbleIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Retirer",
                onClick = onRemove,
                tone = BubbleTone.DANGER,
                diameter = 30.dp
            )
        }
    }
}

/**
 * Formate des secondes en mm:ss.
 * Exemple : 65 → « 01:05 ».
 */
fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
