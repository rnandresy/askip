package com.rnandresy.lol.ui.notifications

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.AppNotification
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.ui.theme.AdminGoldBg
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    vm: AskipViewModel,
    onOpenPost: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val notifications by vm.notifications.collectAsState()
    val unread         = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (unread > 0) {
                            Text(
                                "$unread non lue(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (unread > 0) {
                        TextButton(onClick = { vm.markAllNotificationsRead() }) {
                            Text(
                                "Tout lire",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                EmptyState("🔕", "Aucune notification", "Les mentions et annonces apparaîtront ici")
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotifRow(
                        notif    = notif,
                        onClick  = {
                            vm.markNotificationRead(notif.id)
                            when {
                                notif.postId.isNotBlank()         -> onOpenPost(notif.postId)
                                notif.conversationId.isNotBlank() -> onOpenConversation(notif.conversationId)
                                notif.fromUserId.isNotBlank()     -> onOpenProfile(notif.fromUserId)
                            }
                        },
                        onDelete = { vm.deleteNotification(notif.id) }
                    )
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifRow(notif: AppNotification, onClick: () -> Unit, onDelete: () -> Unit) {
    val isUnread    = !notif.isRead
    val fromAdmin   = notif.isFromAdmin()
    val isMandatory = notif.isMandatory()

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isMandatory && isUnread -> AdminGoldBg
                    isUnread                -> MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                    else                    -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icône
        Box(
            modifier         = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    when (notif.type) {
                        "new_post_admin"   -> AdminGoldBg
                        "mention_everyone" -> MaterialTheme.colorScheme.tertiaryContainer
                        "mention"          -> if (fromAdmin) AdminGoldBg
                        else MaterialTheme.colorScheme.primaryContainer
                        "message"          -> if (fromAdmin) AdminGoldBg
                        else MaterialTheme.colorScheme.surfaceVariant
                        else               -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (notif.type) {
                    "new_post_admin"   -> "📣"
                    "new_post"         -> "📢"
                    "mention_everyone" -> "📣"
                    "mention"          -> if (fromAdmin) "👑" else "💬"
                    "message"          -> if (fromAdmin) "👑" else "✉️"
                    else               -> "🔔"
                },
                fontSize = 18.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Row(
                    modifier          = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        notifTitle(notif),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color      = if (fromAdmin && isMandatory) AdminGold
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (isMandatory) {
                        Surface(
                            color  = AdminGoldBg,
                            shape  = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ADMIN",
                                modifier   = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = AdminGold,
                                fontSize   = 8.sp
                            )
                        }
                    }
                }
                Text(
                    formatTs(notif.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (notif.content.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    notif.content,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMandatory) AdminGold
                            else MaterialTheme.colorScheme.onSurface
                        )
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Close, null,
                    modifier = Modifier.size(12.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                )
            }
        }
    }
}

private fun notifTitle(notif: AppNotification): String = when (notif.type) {
    "new_post_admin"   -> "${notif.fromUsername} a posté une annonce"
    "new_post"         -> "${notif.fromUsername} a publié un post"
    "mention_everyone" -> "${notif.fromUsername} interpelle tout le monde"
    "mention"          -> "${notif.fromUsername} te mentionne"
    "message"          -> "Message de ${notif.fromUsername}"
    else               -> "Notification"
}