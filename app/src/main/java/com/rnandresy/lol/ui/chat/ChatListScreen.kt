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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.components.*
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    vm: AskipViewModel,
    onOpenChat: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    val conversations by vm.conversations.collectAsState()
    val groups        by vm.groups.collectAsState()
    val allProfiles   by vm.allProfiles.collectAsState()
    val profilesMap   by vm.profilesMap.collectAsState()
    val uid            = vm.currentUserId
    var selectedTab   by remember { mutableStateOf(0) }
    var showPicker    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text("Messages 💬", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = onCreateGroup) { Icon(Icons.Default.GroupAdd, null) }
                    } else {
                        IconButton(onClick = { showPicker = true }) { Icon(Icons.Default.Edit, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                        Text("Privés", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp))
                        Text("Groupes (${groups.size})", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            when (selectedTab) {
                // ── DMs ───────────────────────────────────────────────────────
                0 -> {
                    if (conversations.isEmpty()) {
                        EmptyChatHint("💌", "Aucun message", "Appuie sur ✏️ pour commencer")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(conversations, key = { it.id }) { conv ->
                                val otherId    = conv.participants.firstOrNull { it != uid } ?: ""
                                val otherName  = conv.participantNames[otherId] ?: "Utilisateur"
                                val otherPhoto = profilesMap[otherId]?.photoUrl ?: ""
                                val unread     = vm.getUnread(conv)
                                val hasUnread  = unread > 0

                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.markRead(conv.id); onOpenChat(conv.id) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AskipAvatar(username = otherName, photoUrl = otherPhoto,
                                        size = 52.dp, isAdminUser = isAdmin(otherId),
                                        onClick = { onOpenProfile(otherId) })
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Text(otherName, style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Normal)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (conv.lastTimestamp > 0)
                                                    Text(formatTs(conv.lastTimestamp), style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (hasUnread) Badge { Text("$unread") }
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(conv.lastMessage.ifBlank { "Démarrer la conversation 👋" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasUnread) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(start = 80.dp))
                            }
                        }
                    }
                }

                // ── Groupes ───────────────────────────────────────────────────
                1 -> {
                    if (groups.isEmpty()) {
                        EmptyChatHint("👥", "Aucun groupe", "Appuie sur + pour créer un groupe")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(groups, key = { it.id }) { group ->
                                GroupRow(group = group, currentUid = uid, onClick = { onOpenGroup(group.id) })
                            }
                        }
                    }
                }
            }
        }

        // ── Dialog nouveau DM ─────────────────────────────────────────────────
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
                                                showPicker = false; onOpenChat(convId)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AskipAvatar(username = profile.username, photoUrl = profile.photoUrl,
                                        size = 40.dp, isAdminUser = isAdmin(profile.userId))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(profile.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        if (profile.classeENI.isNotBlank())
                                            Text(profile.classeENI, style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Fermer") } }
            )
        }
    }
}

@Composable
private fun GroupRow(group: Group, currentUid: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color    = MaterialTheme.colorScheme.secondaryContainer,
            shape    = RoundedCornerShape(14.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(group.emoji, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (group.lastTimestamp > 0)
                    Text(formatTs(group.lastTimestamp), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (group.lastMessage.isNotBlank())
                    "${group.lastSenderUsername.ifBlank { "..." }}: ${group.lastMessage}"
                else "${group.members.size} membre(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        modifier = Modifier.padding(start = 80.dp))
}

@Composable
private fun EmptyChatHint(emoji: String, title: String, sub: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}