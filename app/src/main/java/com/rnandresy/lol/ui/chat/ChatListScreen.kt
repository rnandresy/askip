package com.rnandresy.lol.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.feed.UserAvatar
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: AskipViewModel,
    onOpenConversation: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val currentUserId = viewModel.currentUserId
    var showNewChat by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadAllProfiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages 💬", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showNewChat = true }) { Icon(Icons.Default.Edit, "Nouveau message") }
                }
            )
        }
    ) { pad ->
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💌", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Aucune conversation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Appuie sur ✏️ pour écrire", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad)) {
                items(conversations, key = { it.id }) { conv ->
                    ConversationItem(
                        conv = conv,
                        currentUserId = currentUserId,
                        unreadCount = viewModel.getUnreadCount(conv),
                        onOpen = { onOpenConversation(conv.id) },
                        onAvatarClick = {
                            val otherId = conv.participants.firstOrNull { it != currentUserId } ?: return@ConversationItem
                            onOpenProfile(otherId)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(start = 76.dp))
                }
            }
        }

        if (showNewChat) {
            AlertDialog(
                onDismissRequest = { showNewChat = false },
                title = { Text("Nouvelle conversation 💬") },
                text = {
                    if (allProfiles.isEmpty()) {
                        Text("Aucun membre disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(allProfiles, key = { it.userId }) { profile ->
                                UserProfileItem(profile = profile, onClick = {
                                    viewModel.startConversation(profile.userId, profile.username, profile.photoUrl) { convId ->
                                        showNewChat = false
                                        onOpenConversation(convId)
                                    }
                                })
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showNewChat = false }) { Text("Fermer") } }
            )
        }
    }
}

@Composable
fun ConversationItem(
    conv: Conversation,
    currentUserId: String,
    unreadCount: Int,
    onOpen: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val otherId = conv.participants.firstOrNull { it != currentUserId } ?: ""
    val otherName = conv.participantNames[otherId] ?: "Utilisateur"
    val otherPhoto = conv.participantPhotos[otherId] ?: ""
    val hasUnread = unreadCount > 0
    val timeStr = if (conv.lastTimestamp > 0L)
        SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(conv.lastTimestamp))
    else ""

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(photoUrl = otherPhoto, username = otherName, size = 50, isAdmin = isAdmin(otherId), onClick = onAvatarClick)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    otherName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (hasUnread) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (hasUnread) {
                        Badge { Text("$unreadCount", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                conv.lastMessage.ifBlank { "Démarrer la conversation 👋" },
                style = MaterialTheme.typography.bodySmall,
                color = if (hasUnread) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserProfileItem(profile: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(photoUrl = profile.photoUrl, username = profile.username, size = 40, isAdmin = isAdmin(profile.userId))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(profile.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (profile.classeENI.isNotBlank()) {
                Text(profile.classeENI, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}