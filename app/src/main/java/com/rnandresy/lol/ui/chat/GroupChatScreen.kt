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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.GroupMessage
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.BubbleGloss
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.bubbleShell
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    vm: AskipViewModel,
    groupId: String,
    group: Group?,
    onOpenProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    val messages       by vm.groupMessages.collectAsState()
    val profilesMap    by vm.profilesMap.collectAsState()
    val isRecording    by vm.isRecording.collectAsState()
    val recordingSecs  by vm.recordingSeconds.collectAsState()
    val loading        by vm.loading.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()
    val uid             = vm.currentUserId
    val context         = LocalContext.current
    val isCreator       = group?.createdBy == uid
    val userIsAdmin     = isAdmin(uid)

    var text            by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showInfo        by remember { mutableStateOf(false) }
    val listState        = rememberLazyListState()
    val palette          = LocalAskipPalette.current

    var actionOn by remember { mutableStateOf<GroupMessage?>(null) }
    var replyTo by remember { mutableStateOf<GroupMessage?>(null) }

    LaunchedEffect(groupId) { vm.listenGroupMessages(groupId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.sendGroupMessageWithMedia(groupId, imageUri = it) }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.sendGroupMessageWithMedia(groupId, videoUri = it) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.sendGroupMessageWithMedia(groupId, fileUri = it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clickable { showInfo = true }
                    ) {
                        Surface(
                            color    = MaterialTheme.colorScheme.secondaryContainer,
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(group?.emoji ?: "👥", fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(group?.name ?: "Groupe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${group?.members?.size ?: 0} membres · voir infos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    Box(Modifier.padding(start = Space.md)) {
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onBack
                        )
                    }
                },
                actions = {
                    Box(Modifier.padding(end = Space.lg)) {
                        if (isCreator || userIsAdmin) {
                            BubbleIconButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = "Supprimer le groupe",
                                onClick = { vm.deleteGroup(groupId); onBack() },
                                tone = BubbleTone.DANGER
                            )
                        } else {
                            BubbleIconButton(
                                icon = Icons.AutoMirrored.Rounded.ExitToApp,
                                contentDescription = "Quitter le groupe",
                                onClick = { vm.leaveGroup(groupId); onBack() },
                                tone = BubbleTone.DANGER
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = loading && uploadProgress in 1..99) {
                    LinearProgressIndicator(progress = { uploadProgress / 100f }, modifier = Modifier.fillMaxWidth())
                }
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isRecording) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp).navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BubbleIconButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = "Annuler l'enregistrement",
                                onClick = { vm.cancelVoiceRecording() },
                                tone = BubbleTone.DANGER
                            )
                            Surface(color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    var dotV by remember { mutableStateOf(true) }
                                    LaunchedEffect(Unit) { while (true) { delay(500L); dotV = !dotV } }
                                    if (dotV) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                                    else Spacer(Modifier.size(8.dp))
                                    Text(formatDuration(recordingSecs), style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.weight(1f))
                                    Text("🎤 Enregistrement…", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                            BubbleIconButton(
                                icon = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Envoyer le vocal",
                                onClick = { vm.stopAndSendGroupVoice(groupId) },
                                tone = BubbleTone.PRIMARY,
                                diameter = 46.dp,
                                enabled = !loading
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .navigationBarsPadding()
                        ) {
                        replyTo?.let { target ->
                            ReplyBanner(
                                username = target.senderUsername,
                                content = target.quote(),
                                onCancel = { replyTo = null },
                                modifier = Modifier.padding(bottom = Space.sm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BubbleIconButton(
                                icon = Icons.Rounded.AttachFile,
                                contentDescription = "Joindre un fichier",
                                onClick = { showMediaPicker = true },
                                enabled = !loading
                            )

                            Spacer(Modifier.width(Space.sm))

                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = { Text("Message au groupe…") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(Radius.pill),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = LocalAskipPalette.current.bubble,
                                    focusedContainerColor = LocalAskipPalette.current.bubble,
                                    unfocusedBorderColor = LocalAskipPalette.current.bubbleBorder,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(Modifier.width(Space.sm))

                            // Même bascule que dans la conversation à deux :
                            // envoi si l'on a écrit, micro sinon.
                            if (text.isNotBlank()) {
                                BubbleIconButton(
                                    icon = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Envoyer",
                                    onClick = {
                                        vm.sendGroupMessage(groupId, text.trim(), replyTo)
                                        text = ""
                                        replyTo = null
                                    },
                                    tone = BubbleTone.PRIMARY,
                                    diameter = 46.dp,
                                    enabled = !loading
                                )
                            } else {
                                BubbleIconButton(
                                    icon = Icons.Rounded.Mic,
                                    contentDescription = "Enregistrer un vocal",
                                    onClick = { vm.startVoiceRecording(context) },
                                    diameter = 46.dp,
                                    enabled = !loading
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

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom) {
                    if (!isMe) {
                        AskipAvatar(username = msg.senderUsername, photoUrl = senderPhoto, size = 28.dp,
                            onClick = { onOpenProfile(msg.senderId) })
                        Spacer(Modifier.width(6.dp))
                    }
                    Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                        if (!isMe) {
                            Text(msg.senderUsername, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                        }
                        val shape = messageBubbleShape(isMe)
                        val ink = messageInk(isMe)

                        TapArea(
                            onTap = { },
                            onLongPress = { actionOn = msg },
                            scaleDown = 0.98f,
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .bubbleShell(
                                    shape,
                                    if (isMe) MaterialTheme.colorScheme.primary else palette.bubble,
                                    if (isMe) MaterialTheme.colorScheme.primary
                                    else palette.bubbleBorder,
                                    palette.shadow,
                                    4.dp
                                )
                        ) {
                            BubbleGloss(shape, if (isMe) 0.8f else 0.4f)
                            Column {
                                if (msg.isReply()) {
                                    ReplyQuote(
                                        username = msg.replyToUsername,
                                        content = msg.replyToContent,
                                        onSurface = ink,
                                        modifier = Modifier.padding(
                                            start = Space.sm,
                                            end = Space.sm,
                                            top = Space.sm
                                        )
                                    )
                                }
                                GroupMessageContent(msg = msg, isMe = isMe)
                            }
                        }

                        ReactionStrip(
                            counts = msg.reactionCounts(),
                            mine = msg.myReaction(uid),
                            onToggle = { vm.toggleGroupMessageReaction(groupId, msg, it) },
                            modifier = Modifier.padding(top = 3.dp)
                        )

                        Spacer(Modifier.height(2.dp))
                        Text(formatTs(msg.timestamp), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    if (isMe) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }

    // ── Actions sur un message ────────────────────────────────────────────────
    actionOn?.let { target ->
        MessageActionSheet(
            myReaction = target.myReaction(uid),
            canDelete = target.senderId == uid || userIsAdmin,
            copyable = target.content.isNotBlank(),
            onReact = { vm.toggleGroupMessageReaction(groupId, target, it) },
            onReply = { replyTo = target },
            onCopy = { copyToClipboard(context, target.content) },
            onDelete = { vm.deleteGroupMessage(groupId, target.id) },
            onDismiss = { actionOn = null }
        )
    }

    // ── Dialog info groupe ────────────────────────────────────────────────────
    if (showInfo && group != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title            = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(group.emoji, fontSize = 24.sp); Text(group.name, fontWeight = FontWeight.Bold)
            }},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (group.description.isNotBlank())
                        Text(group.description, style = MaterialTheme.typography.bodyMedium)
                    Text("${group.members.size} membres :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    group.members.forEach { memberId ->
                        val name  = group.memberNames[memberId] ?: memberId
                        val photo = group.memberPhotos[memberId] ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            AskipAvatar(username = name, photoUrl = photo, size = 28.dp, isAdminUser = isAdmin(memberId) || memberId == group.createdBy)
                            Spacer(Modifier.width(8.dp))
                            Text(name, style = MaterialTheme.typography.bodySmall)
                            if (memberId == group.createdBy) {
                                Spacer(Modifier.width(4.dp))
                                Text("👑", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Fermer") } }
        )
    }

    // ── Dialog médias ─────────────────────────────────────────────────────────
    if (showMediaPicker) {
        AlertDialog(
            onDismissRequest = { showMediaPicker = false },
            title            = { Text("Joindre un fichier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MediaPickOption("📷 Photo") { showMediaPicker = false; imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    MediaPickOption("🎥 Vidéo") { showMediaPicker = false; videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                    MediaPickOption("📎 Fichier") { showMediaPicker = false; filePicker.launch("*/*") }
                }
            },
            confirmButton = { TextButton(onClick = { showMediaPicker = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun GroupMessageContent(msg: GroupMessage, isMe: Boolean) {
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    when {
        msg.isImage() -> {
            AsyncImage(model = msg.mediaUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.width(240.dp).heightIn(max = 280.dp).clip(messageBubbleShape(isMe)))
        }
        msg.isVideo() -> {
            val context = LocalContext.current
            val player = remember(msg.mediaUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(msg.mediaUrl)); prepare(); playWhenReady = false
                }
            }
            DisposableEffect(player) { onDispose { player.release() } }
            AndroidView(factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player; useController = true
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            }, modifier = Modifier.width(240.dp).aspectRatio(16f / 9f).clip(messageBubbleShape(isMe)))
        }
        msg.isAudio() -> AudioMessagePlayer(url = msg.mediaUrl, duration = msg.mediaDuration, isMe = isMe)
        msg.isFile() -> {
            val context = LocalContext.current
            Row(modifier = Modifier.widthIn(max = 240.dp).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = textColor, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(msg.mediaName.take(28), style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium, color = textColor, maxLines = 2)
                    Text("Ouvrir ↗", style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(msg.mediaUrl))) } })
                }
            }
        }
        else -> Text(msg.content, color = textColor, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun MediaPickOption(label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}