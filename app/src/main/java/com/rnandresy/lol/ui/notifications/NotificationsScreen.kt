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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.rnandresy.lol.ui.feed.formatTs
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
                        Text("Notifications 🔔", fontWeight = FontWeight.ExtraBold)
                        if (unread > 0) {
                            Text(
                                "$unread non lue(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (unread > 0) {
                        IconButton(onClick = { vm.markAllNotificationsRead() }) {
                            Icon(Icons.Default.DoneAll, "Tout marquer lu",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (notifications.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🔕", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Aucune notification",
                        style     = MaterialTheme.typography.titleMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tes mentions, messages et annonces apparaîtront eto",
                        style  = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationRow(
                        notif    = notif,
                        onClick  = {
                            if (!notif.isRead) vm.markNotificationRead(notif.id)
                            when {
                                notif.postId.isNotBlank()         -> onOpenPost(notif.postId)
                                notif.conversationId.isNotBlank() -> onOpenConversation(notif.conversationId)
                                notif.fromUserId.isNotBlank()     -> onOpenProfile(notif.fromUserId)
                            }
                        },
                        onDelete = { vm.deleteNotification(notif.id) }
                    )
                    HorizontalDivider(
                        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(start = 72.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    notif: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnread    = !notif.isRead
    val isMandatory = notif.isMandatory()
    val fromAdmin   = notif.isFromAdmin()

    // Couleur de fond selon état
    val bgColor = when {
        isMandatory && isUnread -> Color(0xFFFFD700).copy(alpha = 0.07f)
        isUnread                -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f)
        else                    -> Color.Transparent   // ←  transparent si lu
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icône ─────────────────────────────────────────────────────────────
        NotifIconBox(type = notif.type, fromAdmin = fromAdmin)

        Spacer(Modifier.width(12.dp))

        // ── Corps ─────────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    modifier          = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text       = notifTitle(notif),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color      = when {
                            fromAdmin && isMandatory -> Color(0xFFE6B800)
                            isUnread                -> MaterialTheme.colorScheme.onSurface
                            else                    -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isMandatory) {
                        Surface(
                            color  = Color(0xFFFFD700).copy(alpha = 0.15f),
                            shape  = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ADMIN",
                                modifier   = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize   = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFFE6B800)
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
                    text     = notif.content,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // ── Indicateurs droite ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMandatory) Color(0xFFFFD700)
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                Icon(
                    Icons.Default.Close, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun NotifIconBox(type: String, fromAdmin: Boolean) {
    val (emoji, bgColor) = when (type) {
        "new_post_admin"   -> "📣" to Color(0xFFFFD700).copy(alpha = 0.18f)
        "new_post"         -> "📢" to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        "mention_everyone" -> "📣" to Color(0xFFE91E63).copy(alpha = 0.18f)
        "mention"          ->
            if (fromAdmin) "👑" to Color(0xFFFFD700).copy(alpha = 0.18f)
            else "💬" to Color(0xFF7C4DFF).copy(alpha = 0.18f)
        "message"          ->
            if (fromAdmin) "👑" to Color(0xFFFFD700).copy(alpha = 0.18f)
            else "✉️" to MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else               -> "🔔" to MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier         = Modifier.size(46.dp).clip(CircleShape).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

private fun notifTitle(notif: AppNotification) = when (notif.type) {
    "new_post_admin"   -> "📣 Annonce de ${notif.fromUsername}"
    "new_post"         -> "🔥 ${notif.fromUsername} a posté"
    "mention_everyone" -> "📢 ${notif.fromUsername} interpelle tout le monde"
    "mention"          ->
        if (notif.isFromAdmin()) "👑 ${notif.fromUsername} te mentionne !"
        else "💬 ${notif.fromUsername} te mentionne"
    "message"          ->
        if (notif.isFromAdmin()) "👑 Message de ${notif.fromUsername}"
        else "✉️ ${notif.fromUsername} t'a écrit"
    else -> "Notification"
}