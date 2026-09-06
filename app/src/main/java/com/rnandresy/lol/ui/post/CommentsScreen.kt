package com.rnandresy.lol.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.ui.components.AddChainLinkField
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.components.ChainThread
import com.rnandresy.lol.ui.components.MentionText
import com.rnandresy.lol.ui.components.RightOfReplyCard
import com.rnandresy.lol.ui.components.RightOfReplyPrompt
import com.rnandresy.lol.ui.components.VoiceComposer
import com.rnandresy.lol.ui.components.VoiceCommentBubble
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.feed.MentionTextField
import com.rnandresy.lol.utils.CHAIN_MAX_LINKS
import com.rnandresy.lol.utils.MAX_COMMENT_LENGTH
import com.rnandresy.lol.utils.MAX_VOICE_COMMENT_SECONDS
import com.rnandresy.lol.utils.RumorEngine
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    vm: AskipViewModel,
    postId: String,
    onOpenProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    val comments    by vm.comments.collectAsState()
    val allProfiles by vm.allProfiles.collectAsState()
    val profilesMap by vm.profilesMap.collectAsState()   // ← pour les photos
    val myProfile   by vm.myProfile.collectAsState()
    val uid          = vm.currentUserId
    val isAdminUser  = isAdmin(uid) || myProfile?.isAdmin == true

    val viewedPost by vm.viewedPost.collectAsState()
    val replies    by vm.mentionReplies.collectAsState()
    val chainLinks by vm.chainLinks.collectAsState()

    var textTfv  by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    // Sous une confession, commenter à visage découvert trahirait tout le
    // monde autour : le masque est imposé, pas proposé.
    val forcedAnonymous = viewedPost?.isConfession() == true
    var anonymous by remember { mutableStateOf(false) }
    val postAnonymously = forcedAnonymous || anonymous

    /** Sous une rumeur vocale, la réponse écrite n'existe pas. */
    val voiceOnly = viewedPost?.isVoice() == true
    val isRecording by vm.isRecording.collectAsState()
    val recordingSeconds by vm.recordingSeconds.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(postId) { vm.openPost(postId) }
    LaunchedEffect(comments.size) {
        // La liste contient aussi les réponses épinglées et la chaîne :
        // l'index d'un commentaire n'est plus celui de l'élément affiché.
        val last = listState.layoutInfo.totalItemsCount - 1
        if (comments.isNotEmpty() && last >= 0) listState.animateScrollToItem(last)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text("Commentaires", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(Modifier.padding(start = Space.md)) {
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onBack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Bascule masque — cachée sous une confession, où
                    // l'anonymat n'est pas négociable.
                    if (!forcedAnonymous) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            BubbleChip(
                                label = if (anonymous) "Masqué" else "Commenter masqué",
                                emoji = "🎭",
                                filled = anonymous,
                                accent = if (anonymous) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { anonymous = !anonymous }
                            )
                        }
                    } else {
                        Text(
                            "🎭 Sous une confession, tout le monde commente masqué",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Sous une rumeur vocale, le champ texte disparaît : on
                    // répond par la voix ou on ne répond pas. C'est ce qui
                    // donne son identité à cette actualité.
                    if (voiceOnly) {
                        VoiceComposer(
                            isRecording = isRecording,
                            seconds = recordingSeconds,
                            maxSeconds = MAX_VOICE_COMMENT_SECONDS,
                            onStart = { vm.startVoiceRecording(context) },
                            onStop = {
                                vm.stopRecordingForPost()?.let { (file, seconds) ->
                                    vm.addVoiceComment(
                                        postId, file, seconds,
                                        anonymous = postAnonymously
                                    )
                                }
                            },
                            onCancel = vm::cancelVoiceRecording,
                            idleLabel = "Appuie pour répondre à la voix"
                        )
                        return@Column
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        MentionTextField(
                            value         = textTfv,
                            // Le serveur refuse au-delà de 500 signes : mieux
                            // vaut empêcher la saisie que rejeter l'envoi.
                            onValueChange = {
                                if (it.text.length <= MAX_COMMENT_LENGTH) textTfv = it
                            },
                            allProfiles   = allProfiles,
                            currentUserId = uid,
                            isAdminUser   = isAdminUser,
                            placeholder   = if (postAnonymously) "Ton commentaire masqué…"
                                            else "Ton commentaire… (@ pour mentionner)",
                            maxLines      = 4,
                            shape         = RoundedCornerShape(24.dp),
                            modifier      = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Envoyer",
                            onClick = {
                                val t = textTfv.text.trim()
                                if (t.isNotBlank()) {
                                    vm.addComment(postId, t, anonymous = postAnonymously)
                                    textTfv = TextFieldValue("")
                                }
                            },
                            tone = BubbleTone.PRIMARY,
                            diameter = 46.dp,
                            enabled = textTfv.text.isNotBlank()
                        )
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize().padding(pad),
            contentPadding      = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Le droit de réponse, tout en haut ─────────────────────────────
            // La version de la personne citée passe avant la discussion :
            // en bas, elle arriverait toujours trop tard.
            items(replies, key = { "reply_${it.userId}" }) { reply ->
                RightOfReplyCard(reply)
            }

            viewedPost?.let { post ->
                // Lu depuis l'état collecté plutôt que via le ViewModel : le
                // profil arrive après la première composition, et un appel
                // direct ne déclencherait pas de recomposition à son arrivée.
                val citesMe = post.userId != uid &&
                    RumorEngine.mentions(post.content, myProfile?.username.orEmpty())
                if (citesMe) {
                    item(key = "replyPrompt") {
                        RightOfReplyPrompt(
                            alreadyReplied = replies.any { it.userId == uid },
                            onPublish = { vm.publishRightOfReply(post, it) },
                            onRemove = { vm.removeRightOfReply(post.id) }
                        )
                    }
                }

                // ── Le Téléphone arabe ────────────────────────────────────────
                if (post.isChain()) {
                    item(key = "chain") {
                        ChainThread(
                            origin = post.content,
                            originAuthor = post.username,
                            links = chainLinks
                        )
                    }
                    if (post.canAddLink(uid, CHAIN_MAX_LINKS)) {
                        item(key = "chainAdd") {
                            AddChainLinkField(onSend = { vm.addChainLink(post, it) })
                        }
                    } else {
                        item(key = "chainClosed") {
                            Text(
                                if (post.chainIsFull(CHAIN_MAX_LINKS))
                                    "🔗 Chaîne complète — $CHAIN_MAX_LINKS maillons."
                                else "Tu as déjà posé ton maillon.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🤫", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Personne n'a encore commenté…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(comments, key = { it.id }) { comment ->
                // Un commentaire masqué n'affiche ni photo ni lien vers le profil.
                val commentPhotoUrl =
                    if (comment.isAnonymous) "" else profilesMap[comment.userId]?.photoUrl ?: ""
                CommentRow(
                    comment       = comment,
                    photoUrl      = commentPhotoUrl,
                    currentUid    = uid,
                    onAvatarClick = {
                        if (!comment.isAnonymous) onOpenProfile(comment.userId)
                    },
                    onLike        = { vm.toggleCommentLike(postId, comment) },
                    onDelete      = { vm.deleteComment(postId, comment.id) }
                )
            }
        }
    }
}

@Composable
fun CommentRow(
    comment: Comment,
    photoUrl: String = "",
    currentUid: String,
    onAvatarClick: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit = {}
) {
    val canDelete = comment.userId == currentUid || isAdmin(currentUid)
    val liked = comment.isLikedBy(currentUid)
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    Row(verticalAlignment = Alignment.Top) {
        AskipAvatar(
            username = comment.username.ifBlank { "?" },
            photoUrl = photoUrl,
            size     = 36.dp,
            isAdminUser  = isAdmin(comment.userId) && !comment.isAnonymous,
            onClick  = if (comment.isAnonymous) null else onAvatarClick
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            // La pointe en haut à gauche rattache la bulle à son avatar,
            // comme dans une messagerie.
            BubbleCard(
                shape = RoundedCornerShape(
                    topStart = 3.dp, topEnd = Radius.md,
                    bottomStart = Radius.md, bottomEnd = Radius.md
                ),
                elevation = 2.dp,
                gloss = 0.35f
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            comment.username.ifBlank { "Anonyme 🎭" },
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (comment.isAnonymous)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            modifier   = if (comment.isAnonymous) Modifier
                            else Modifier.clickable(onClick = onAvatarClick)
                        )
                        if (canDelete) {
                            BubbleIconButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = "Supprimer le commentaire",
                                onClick = onDelete,
                                tone = BubbleTone.DANGER,
                                diameter = 26.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    if (comment.isVoice()) {
                        VoiceCommentBubble(
                            url = comment.audioUrl,
                            duration = comment.audioDuration
                        )
                    } else {
                        MentionText(
                            text = comment.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    formatTs(comment.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TapArea(
                    onTap = { tap(); onLike() },
                    scaleDown = 0.88f,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(palette.bubble)
                        .border(1.dp, palette.bubbleBorder, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(if (liked) "❤️" else "🤍", fontSize = 11.sp)
                        if (comment.likeCount() > 0) {
                            Text(
                                "${comment.likeCount()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}