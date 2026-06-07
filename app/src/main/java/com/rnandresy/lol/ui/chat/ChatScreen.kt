package com.rnandresy.lol.ui.chat

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: AskipViewModel,
    convId: String,
    otherUserId: String,
    otherUsername: String,
    otherPhotoUrl: String = "",
    onOpenProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    val messages       by vm.messages.collectAsState()
    val profilesMap    by vm.profilesMap.collectAsState()
    val isRecording    by vm.isRecording.collectAsState()
    val recordingSecs  by vm.recordingSeconds.collectAsState()
    val loading        by vm.loading.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()
    val uid             = vm.currentUserId
    val context         = LocalContext.current

    var text            by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }
    val listState        = rememberLazyListState()

    LaunchedEffect(convId) {
        vm.listenMessages(convId)
        vm.markRead(convId)
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // ── Pickers ───────────────────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.sendMessageWithMedia(convId, imageUri = it) } }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.sendMessageWithMedia(convId, videoUri = it) } }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.sendMessageWithMedia(convId, fileUri = it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clickable { onOpenProfile(otherUserId) }
                    ) {
                        AskipAvatar(
                            username = otherUsername,
                            photoUrl = otherPhotoUrl,
                            size     = 34.dp,
                            isAdminUser  = isAdmin(otherUserId)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                otherUsername,
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Voir le profil",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            Column {
                // Barre de progression upload
                AnimatedVisibility(visible = loading && uploadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { uploadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Surface(shadowElevation = 6.dp) {
                    if (isRecording) {
                        // ── Mode enregistrement vocal ─────────────────────────
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Bouton annuler
                            IconButton(onClick = { vm.cancelVoiceRecording() }) {
                                Icon(
                                    Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            // Indicateur + timer
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
                                    // Point clignotant
                                    var dotVisible by remember { mutableStateOf(true) }
                                    LaunchedEffect(Unit) {
                                        while (true) { delay(500L); dotVisible = !dotVisible }
                                    }
                                    if (dotVisible) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    } else {
                                        Spacer(Modifier.size(8.dp))
                                    }
                                    Text(
                                        formatDuration(recordingSecs),
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "🎤 Enregistrement…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            // Bouton envoyer le vocal
                            FilledIconButton(
                                onClick = { vm.stopAndSendVoice(convId) },
                                enabled = !loading
                            ) {
                                Icon(Icons.Default.Send, null)
                            }
                        }
                    } else {
                        // ── Mode normal ───────────────────────────────────────
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bouton pièce jointe
                            IconButton(onClick = { showMediaPicker = true }) {
                                Icon(
                                    Icons.Default.AttachFile, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value         = text,
                                onValueChange = { text = it },
                                placeholder   = { Text("Message…") },
                                modifier      = Modifier.weight(1f),
                                shape         = RoundedCornerShape(24.dp),
                                maxLines      = 4
                            )

                            Spacer(Modifier.width(6.dp))

                            if (text.isNotBlank()) {
                                FilledIconButton(
                                    onClick  = {
                                        vm.sendMessage(convId, text.trim())
                                        text = ""
                                    },
                                    enabled  = !loading
                                ) { Icon(Icons.Default.Send, null) }
                            } else {
                                FilledIconButton(
                                    onClick  = { vm.startVoiceRecording(context) },
                                    enabled  = !loading,
                                    colors   = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Mic, null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize().padding(pad),
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe        = msg.senderId == uid
                val senderPhoto = profilesMap[msg.senderId]?.photoUrl ?: ""

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    if (!isMe) {
                        AskipAvatar(
                            username = msg.senderUsername,
                            photoUrl = senderPhoto,
                            size     = 28.dp,
                            onClick  = { onOpenProfile(msg.senderId) }
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    Column(
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart    = 16.dp,
                                        topEnd      = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd   = if (isMe) 4.dp  else 16.dp
                                    )
                                )
                                .background(
                                    if (isMe) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .widthIn(max = 280.dp)
                        ) {
                            MessageContent(msg = msg, isMe = isMe)
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            formatTs(msg.timestamp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    if (isMe) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }

    // ── Dialog sélection média ────────────────────────────────────────────────
    if (showMediaPicker) {
        AlertDialog(
            onDismissRequest = { showMediaPicker = false },
            title            = { Text("Joindre un fichier") },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MediaPickOption("📷 Photo") {
                        showMediaPicker = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    MediaPickOption("🎥 Vidéo") {
                        showMediaPicker = false
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                    MediaPickOption("📎 Fichier (PDF, doc…, mbola tsy mety fa andramo iany)") {
                        showMediaPicker = false
                        filePicker.launch("*/*")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaPicker = false }) { Text("Annuler") }
            }
        )
    }
}

// ── Contenu d'une bulle ───────────────────────────────────────────────────────

@Composable
private fun MessageContent(msg: Message, isMe: Boolean) {
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    when {
        msg.isImage() -> {
            Column {
                AsyncImage(
                    model              = msg.mediaUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .width(240.dp)
                        .heightIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart    = 16.dp,
                                topEnd      = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd   = if (isMe) 4.dp  else 16.dp
                            )
                        )
                )
                if (msg.content.isNotBlank()) {
                    Text(
                        msg.content,
                        color    = textColor,
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        msg.isVideo() -> {
            Column {
                VideoMessagePlayer(url = msg.mediaUrl, isMe = isMe)
                if (msg.content.isNotBlank()) {
                    Text(
                        msg.content,
                        color    = textColor,
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        msg.isAudio() -> {
            AudioMessagePlayer(
                url      = msg.mediaUrl,
                duration = msg.mediaDuration,
                isMe     = isMe
            )
        }

        msg.isFile() -> {
            FileMessageItem(
                name = msg.mediaName,
                url  = msg.mediaUrl,
                isMe = isMe
            )
        }

        else -> {
            // Texte pur
            Text(
                msg.content,
                color    = textColor,
                style    = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

// ── Lecteur vidéo inline ──────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoMessagePlayer(url: String, isMe: Boolean) {
    val context = LocalContext.current
    val player  = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    AndroidView(
        factory  = { ctx ->
            PlayerView(ctx).apply {
                this.player   = player
                useController = true
                layoutParams  = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = Modifier
            .width(240.dp)
            .aspectRatio(16f / 9f)
            .clip(
                RoundedCornerShape(
                    topStart    = 16.dp,
                    topEnd      = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd   = if (isMe) 4.dp  else 16.dp
                )
            )
    )
}

// ── Lecteur audio ─────────────────────────────────────────────────────────────

@Composable
fun AudioMessagePlayer(url: String, duration: Int, isMe: Boolean) {
    val context   = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress  by remember { mutableStateOf(0f) }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    // Mise à jour de la progression
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            val dur = player.duration.takeIf { it > 0 }
                ?: (duration.coerceAtLeast(1) * 1000L)
            val pos = player.currentPosition
            progress = pos.toFloat() / dur
            if (pos >= dur - 150L) {
                player.seekTo(0)
                player.pause()
                isPlaying = false
                progress  = 0f
            }
            delay(100L)
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    val tint       = if (isMe) Color.White else MaterialTheme.colorScheme.primary
    val trackColor = if (isMe) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val subColor   = if (isMe) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier              = Modifier
            .widthIn(min = 180.dp, max = 240.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🎤", fontSize = 20.sp)

        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(3.dp),
                color      = tint,
                trackColor = trackColor
            )
            Spacer(Modifier.height(3.dp))
            Text(
                formatDuration(duration.coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = subColor
            )
        }

        IconButton(
            onClick  = {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    if (progress >= 1f) { player.seekTo(0); progress = 0f }
                    player.play()
                    isPlaying = true
                }
            },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint     = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Fichier joint ─────────────────────────────────────────────────────────────

@Composable
private fun FileMessageItem(name: String, url: String, isMe: Boolean) {
    val context   = LocalContext.current
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val subColor  = if (isMe) Color.White.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.primary

    Row(
        modifier              = Modifier
            .widthIn(min = 160.dp, max = 240.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.InsertDriveFile, null,
            tint     = textColor,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name.take(32).ifBlank { "Fichier" },
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color      = textColor,
                maxLines   = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Ouvrir ↗",
                style    = MaterialTheme.typography.labelSmall,
                color    = subColor,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    }
                }
            )
        }
    }
}

// ── Option dialog médias ──────────────────────────────────────────────────────

@Composable
private fun MediaPickOption(label: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Utilitaire durée ──────────────────────────────────────────────────────────

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}