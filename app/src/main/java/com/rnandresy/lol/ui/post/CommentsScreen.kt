package com.rnandresy.lol.ui.post

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.ui.feed.MentionText
import com.rnandresy.lol.ui.feed.MentionTextField
import com.rnandresy.lol.ui.feed.UserAvatar
import com.rnandresy.lol.ui.feed.formatTs
import com.rnandresy.lol.utils.isAdmin
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

    var textTfv  by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    LaunchedEffect(postId) { vm.listenComments(postId) }
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) listState.animateScrollToItem(comments.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Commentaires 💬") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        MentionTextField(
                            value         = textTfv,
                            onValueChange = { textTfv = it },
                            allProfiles   = allProfiles,
                            currentUserId = uid,
                            isAdminUser   = isAdminUser,
                            placeholder   = "Ton commentaire… (@ pour mentionner)",
                            maxLines      = 4,
                            shape         = RoundedCornerShape(24.dp),
                            modifier      = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val t = textTfv.text.trim()
                                if (t.isNotBlank()) {
                                    vm.addComment(postId, t)
                                    textTfv = TextFieldValue("")
                                }
                            },
                            enabled = textTfv.text.isNotBlank()
                        ) { Icon(Icons.Default.Send, null) }
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
                // Résolution photo depuis le cache
                val commentPhotoUrl = profilesMap[comment.userId]?.photoUrl ?: ""
                CommentRow(
                    comment       = comment,
                    photoUrl      = commentPhotoUrl,   // ← photo résolue
                    currentUid    = uid,
                    onAvatarClick = { onOpenProfile(comment.userId) },
                    onDelete      = { vm.deleteComment(postId, comment.id) }
                )
            }
        }
    }
}

@Composable
fun CommentRow(
    comment: Comment,
    photoUrl: String = "",           // ← paramètre ajouté
    currentUid: String,
    onAvatarClick: () -> Unit,
    onDelete: () -> Unit
) {
    val canDelete = comment.userId == currentUid || isAdmin(currentUid)

    Row(verticalAlignment = Alignment.Top) {
        UserAvatar(
            username = comment.username,
            photoUrl = photoUrl,     // ← photo résolue
            size     = 36,
            isAdmin  = isAdmin(comment.userId),
            onClick  = onAvatarClick
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Card(
                shape  = RoundedCornerShape(
                    topStart    = 2.dp, topEnd = 14.dp,
                    bottomStart = 14.dp, bottomEnd = 14.dp
                ),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            comment.username.ifBlank { "Anonyme" },
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                            modifier   = Modifier.clickable(onClick = onAvatarClick)
                        )
                        if (canDelete) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Delete, null,
                                    tint     = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    MentionText(text = comment.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                formatTs(comment.timestamp),
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}