package com.rnandresy.lol.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.BubbleBadge
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.SheetHeader
import com.rnandresy.lol.ui.components.SlidingSegmented
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

/** Les deux listes : à deux, ou à plusieurs. */
private enum class ChatTab(val label: String) {
    DIRECT("Privés"),
    GROUPS("Groupes")
}

/**
 * La liste des conversations.
 *
 * Les deux onglets Material laissaient une barre soulignée en travers de
 * l'écran ; ils deviennent le segmenté glissant du reste de l'app. Le choix
 * d'un correspondant passe d'une boîte de dialogue à une feuille, plus haute
 * et plus facile à parcourir au pouce.
 */
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
    val groups by vm.groups.collectAsState()
    val allProfiles by vm.allProfiles.collectAsState()
    val profilesMap by vm.profilesMap.collectAsState()
    val uid = vm.currentUserId

    var tab by remember { mutableStateOf(ChatTab.DIRECT) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                actions = {
                    // Le bouton suit l'onglet : nouveau groupe côté groupes,
                    // nouvelle conversation côté messages.
                    BubbleIconButton(
                        icon = if (tab == ChatTab.GROUPS) Icons.Rounded.GroupAdd
                        else Icons.Rounded.Edit,
                        contentDescription = if (tab == ChatTab.GROUPS) "Créer un groupe"
                        else "Nouvelle conversation",
                        onClick = {
                            if (tab == ChatTab.GROUPS) onCreateGroup() else showPicker = true
                        },
                        tone = BubbleTone.PRIMARY,
                        modifier = Modifier.padding(end = Space.lg)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SlidingSegmented(
                items = ChatTab.entries.toList(),
                selected = tab,
                labelOf = {
                    // Le compte ne s'affiche que s'il y a quelque chose à
                    // compter : « Groupes (0) » n'apprend rien.
                    when (it) {
                        ChatTab.DIRECT -> it.label
                        ChatTab.GROUPS ->
                            if (groups.isEmpty()) it.label else "${it.label} (${groups.size})"
                    }
                },
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
            )

            when (tab) {
                ChatTab.DIRECT -> {
                    if (conversations.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            EmptyState(
                                "💌", "Aucun message",
                                "Appuie sur le crayon pour commencer"
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Space.lg,
                                vertical = Space.xs
                            ),
                            verticalArrangement = Arrangement.spacedBy(Space.sm)
                        ) {
                            items(conversations, key = { it.id }) { conv ->
                                val otherId = conv.participants.firstOrNull { it != uid } ?: ""
                                ConversationRow(
                                    conv = conv,
                                    otherId = otherId,
                                    otherName = conv.participantNames[otherId] ?: "Utilisateur",
                                    otherPhoto = profilesMap[otherId]?.photoUrl ?: "",
                                    unread = vm.getUnread(conv),
                                    onOpen = { vm.markRead(conv.id); onOpenChat(conv.id) },
                                    onOpenProfile = { onOpenProfile(otherId) }
                                )
                            }
                        }
                    }
                }

                ChatTab.GROUPS -> {
                    if (groups.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            EmptyState(
                                "👥", "Aucun groupe",
                                "Appuie sur + pour en créer un"
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Space.lg,
                                vertical = Space.xs
                            ),
                            verticalArrangement = Arrangement.spacedBy(Space.sm)
                        ) {
                            items(groups, key = { it.id }) { group ->
                                GroupRow(group = group, onClick = { onOpenGroup(group.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        NewChatSheet(
            profiles = allProfiles,
            onPick = { profile ->
                vm.startConversation(profile.userId, profile.username) { convId ->
                    showPicker = false
                    onOpenChat(convId)
                }
            },
            onDismiss = { showPicker = false }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Les lignes
// ═════════════════════════════════════════════════════════════════════════════

/** Une conversation à deux. */
@Composable
private fun ConversationRow(
    conv: Conversation,
    otherId: String,
    otherName: String,
    otherPhoto: String,
    unread: Int,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val hasUnread = unread > 0
    val tap = rememberTapFeedback()

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (hasUnread) 5.dp else 2.dp,
        gloss = if (hasUnread) 0.5f else 0.28f,
        onClick = { tap(); onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AskipAvatar(
                username = otherName,
                photoUrl = otherPhoto,
                size = 48.dp,
                isAdminUser = isAdmin(otherId),
                onClick = onOpenProfile
            )
            Spacer(Modifier.width(Space.md))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        otherName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conv.lastTimestamp > 0) {
                        Text(
                            formatTs(conv.lastTimestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Space.sm)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conv.lastMessage.ifBlank { "Démarrer la conversation 👋" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasUnread) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasUnread) {
                        Spacer(Modifier.width(Space.sm))
                        BubbleBadge(count = unread)
                    }
                }
            }
        }
    }
}

/** Un groupe. */
@Composable
private fun GroupRow(group: Group, onClick: () -> Unit) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        gloss = 0.28f,
        onClick = { tap(); onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // L'emoji du groupe tient lieu d'avatar : même taille, même
            // place que dans la liste des conversations.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(group.emoji, fontSize = 23.sp)
            }
            Spacer(Modifier.width(Space.md))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (group.lastTimestamp > 0) {
                        Text(
                            formatTs(group.lastTimestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Space.sm)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (group.lastMessage.isNotBlank())
                        "${group.lastSenderUsername.ifBlank { "..." }} : ${group.lastMessage}"
                    else "${group.members.size} membre(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (group.lastMessage.isNotBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else palette.scoop,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** La feuille de choix du correspondant. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatSheet(
    profiles: List<UserProfile>,
    onPick: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tap = rememberTapFeedback()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(bottom = Space.xxl)) {
            SheetHeader("Nouvelle conversation", "Choisis à qui tu écris.")

            if (profiles.isEmpty()) {
                Text(
                    "Aucun membre disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Space.xl)
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(profiles, key = { it.userId }) { profile ->
                        TapArea(
                            onTap = { tap(); onPick(profile) },
                            scaleDown = 0.99f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Space.xl, vertical = Space.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AskipAvatar(
                                    username = profile.username,
                                    photoUrl = profile.photoUrl,
                                    size = 40.dp,
                                    isAdminUser = isAdmin(profile.userId)
                                )
                                Spacer(Modifier.width(Space.md))
                                Column {
                                    Text(
                                        profile.username,
                                        style = MaterialTheme.typography.bodyLarge,
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
            }
        }
    }
}
