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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rnandresy.lol.ui.feed.MentionTextField
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    vm: AskipViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val loading        by vm.loading.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()
    val isRecording    by vm.isRecording.collectAsState()
    val recordingSecs  by vm.recordingSeconds.collectAsState()
    val allProfiles    by vm.allProfiles.collectAsState()
    val myProfile      by vm.myProfile.collectAsState()
    val context         = LocalContext.current

    var postType    by remember { mutableStateOf("normal") }
    var contentTfv  by remember { mutableStateOf(TextFieldValue("")) }
    var opt1        by remember { mutableStateOf("") }
    var opt2        by remember { mutableStateOf("") }
    var imageUri    by remember { mutableStateOf<Uri?>(null) }
    var videoUri    by remember { mutableStateOf<Uri?>(null) }
    var audioFile   by remember { mutableStateOf<File?>(null) }
    var audioDurSec by remember { mutableStateOf(0) }
    var fileUri     by remember { mutableStateOf<Uri?>(null) }
    var fileName    by remember { mutableStateOf("") }

    val content     = contentTfv.text
    val isAdminUser = isAdmin(vm.currentUserId) || myProfile?.isAdmin == true

    val canPost = when (postType) {
        "poll" -> content.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank()
        else   -> content.isNotBlank() || imageUri != null || videoUri != null
                || audioFile != null || fileUri != null
    }



    fun clearMedia() { imageUri = null; videoUri = null; audioFile = null; fileUri = null; fileName = "" }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { clearMedia(); imageUri = it }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { clearMedia(); videoUri = it }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            clearMedia()
            fileUri  = it
            fileName = it.lastPathSegment?.substringAfterLast('/') ?: "fichier"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (postType) {
                        "poll"       -> "Sondage 📊"
                        "confession" -> "Confession 🎭"
                        else         -> "Nouvelle rumeur 📢"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !loading) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick  = {
                            vm.createPostWithMedia(
                                content   = content.trim(),
                                type      = postType,
                                pollOpt1  = opt1.trim(),
                                pollOpt2  = opt2.trim(),
                                imageUri  = imageUri,
                                videoUri  = videoUri,
                                audioFile = audioFile,
                                fileUri   = fileUri
                            )
                            onDone()
                        },
                        enabled = canPost && !loading
                    ) {
                        if (loading)
                            CircularProgressIndicator(
                                Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp
                            )
                        else Icon(Icons.Default.Send, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier            = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Sélecteur de type ─────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "normal"     to "📢 Rumeur",
                    "poll"       to "📊 Sondage",
                    "confession" to "🎭 Confession"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = postType == type,
                        onClick  = { postType = type; clearMedia() },
                        label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // ── Info confession ───────────────────────────────────────────────
            if (postType == "confession") {
                Surface(
                    color  = MaterialTheme.colorScheme.surfaceVariant,
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕵️", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ton identité sera très bien cachée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Champ texte avec mentions ─────────────────────────────────────
            MentionTextField(
                value         = contentTfv,
                onValueChange = { if (it.text.length <= 500) contentTfv = it },
                allProfiles   = allProfiles,
                currentUserId = vm.currentUserId,
                isAdminUser   = isAdminUser,
                placeholder   = when (postType) {
                    "poll"       -> "De quoi parle ce sondage ?"
                    "confession" -> "Ta confession (anonyme)…"
                    else         -> "Askip… qu'est-ce qui se passe ? 👀"
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (content.isNotEmpty()) {
                Text(
                    "${content.length}/500",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (content.length > 450) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Aperçu image ──────────────────────────────────────────────────
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model              = imageUri,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick  = { imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.55f)) {
                            Icon(Icons.Default.Close, null, tint = Color.White,
                                modifier = Modifier.padding(4.dp).size(18.dp))
                        }
                    }
                }
            }

            // ── Aperçu vidéo ──────────────────────────────────────────────────
            if (videoUri != null) {
                Surface(
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VideoFile, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Vidéo sélectionnée", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { videoUri = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Aperçu audio enregistré ───────────────────────────────────────
            if (audioFile != null) {
                Surface(
                    color    = MaterialTheme.colorScheme.secondaryContainer,
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎤", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Note vocale (${formatDuration(audioDurSec)})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = { audioFile = null; audioDurSec = 0 }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Aperçu fichier ────────────────────────────────────────────────
            if (fileUri != null) {
                Surface(
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
                            Text(fileName.take(28).ifBlank { "Fichier" }, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { fileUri = null; fileName = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Boutons médias (sauf sondage/confession) ──────────────────────
            if (postType == "normal") {
                if (!isRecording) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            shape   = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Photo")
                        }
                        OutlinedButton(
                            onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            shape   = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vidéo")
                        }
                        OutlinedButton(
                            onClick = { filePicker.launch("*/*") },
                            shape   = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Fichier")
                        }
                    }
                }

                // Bouton micro / enregistrement
                if (audioFile == null) {
                    if (isRecording) {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Point clignotant
                            var dotVisible by remember { mutableStateOf(true) }
                            LaunchedEffect(Unit) { while (true) { delay(500L); dotVisible = !dotVisible } }

                            Surface(
                                color    = MaterialTheme.colorScheme.errorContainer,
                                shape    = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (dotVisible) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier.size(8.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    } else Spacer(Modifier.size(8.dp))
                                    Text(
                                        "🎤 ${formatDuration(recordingSecs)}",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            // Annuler
                            OutlinedButton(
                                onClick = { vm.cancelVoiceRecording() },
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Annuler") }
                            // Arrêter + joindre
                            Button(onClick = {
                                val result = vm.stopRecordingForPost()
                                result?.let { (file, dur) -> audioFile = file; audioDurSec = dur }
                            }) { Text("Joindre") }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { vm.startVoiceRecording(context) },
                            shape   = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Note vocale")
                        }
                    }
                }
            }

            // ── Options sondage ───────────────────────────────────────────────
            if (postType == "poll") {
                Text("Options", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = opt1, onValueChange = { if (it.length <= 60) opt1 = it },
                    label = { Text("Option A") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = opt2, onValueChange = { if (it.length <= 60) opt2 = it },
                    label = { Text("Option B") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Barre de progression ──────────────────────────────────────────
            if (loading && uploadProgress in 1..99) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { uploadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Upload : $uploadProgress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
/**
 * Formats seconds into a mm:ss string.
 * Example: 65 -> "01:05"
 */
fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}