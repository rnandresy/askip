package com.rnandresy.lol.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.feed.UserAvatar
import com.rnandresy.lol.ui.feed.formatTs
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    vm: AskipViewModel,
    onOpenChat: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val conversations by vm.conversations.collectAsState()
    val allProfiles   by vm.allProfiles.collectAsState()
    val profilesMap   by vm.profilesMap.collectAsState()   // ← pour les photos
    val uid            = vm.currentUserId
    var showPicker    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text("Messages 💬", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.Edit, "Nouvelle conversation")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { pad ->
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💌", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Aucun message", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Appuie sur ✏️ pour commencer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad)) {
                items(conversations, key = { it.id }) { conv ->
                    val otherId    = conv.participants.firstOrNull { it != uid } ?: ""
                    val otherName  = conv.participantNames[otherId] ?: "Utilisateur"
                    val otherPhoto = profilesMap[otherId]?.photoUrl ?: ""   // ← photo résolue
                    val unread     = vm.getUnread(conv)
                    val hasUnread  = unread > 0

                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.markRead(conv.id)
                                onOpenChat(conv.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            username = otherName,
                            photoUrl = otherPhoto,
                            size     = 52,
                            isAdmin  = isAdmin(otherId),
                            onClick  = { onOpenProfile(otherId) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    otherName,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Normal
                                )
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (conv.lastTimestamp > 0) {
                                        Text(
                                            formatTs(conv.lastTimestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (hasUnread) Badge { Text("$unread") }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                conv.lastMessage.ifBlank { "Démarrer la conversation 👋" },
                                style      = MaterialTheme.typography.bodySmall,
                                color      = if (hasUnread) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider(
                        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 80.dp)
                    )
                }
            }
        }

        if (showPicker) {
            AlertDialog(
                onDismissRequest = { showPicker = false },
                title            = { Text("Nouvelle conversation 💬") },
                text             = {
                    if (allProfiles.isEmpty()) {
                        Text("Aucun membre disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(allProfiles, key = { it.userId }) { profile ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.startConversation(profile.userId, profile.username) { convId ->
                                                showPicker = false
                                                onOpenChat(convId)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        username = profile.username,
                                        photoUrl = profile.photoUrl,   // ← photo depuis profil
                                        size     = 40,
                                        isAdmin  = isAdmin(profile.userId)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            profile.username,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (profile.classeENI.isNotBlank()) {
                                            Text(
                                                profile.classeENI,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPicker = false }) { Text("Fermer") }
                }
            )
        }
    }
}