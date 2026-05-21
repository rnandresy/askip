package com.rnandresy.lol.ui.members

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.AdminBadgeLabel
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.ENIBadgeLabel
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    vm: AskipViewModel,
    onOpenProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    val allProfiles by vm.allProfiles.collectAsState()
    val myProfile   by vm.myProfile.collectAsState()

    val sorted = remember(allProfiles, myProfile) {
        buildList {
            myProfile?.let { add(it) }
            addAll(allProfiles)
        }.distinctBy { it.userId }
            .sortedWith(
                compareByDescending<UserProfile> { isAdmin(it.userId) || it.isAdmin }
                    .thenByDescending { it.hasBadgeENI }
                    .thenBy { it.username.lowercase() }
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Membres", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors         = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                EmptyState("👥", "Aucun membre")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad)) {
                item {
                    Text(
                        "${sorted.size} membre(s)",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(sorted, key = { it.userId }) { profile ->
                    val userIsAdmin = isAdmin(profile.userId) || profile.isAdmin
                    val isMe        = profile.userId == vm.currentUserId

                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProfile(profile.userId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AskipAvatar(
                            username    = profile.username,
                            photoUrl    = profile.photoUrl,
                            size        = 46.dp,
                            isAdminUser = userIsAdmin,
                            onClick     = { onOpenProfile(profile.userId) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    if (isMe) "${profile.username} (Moi)" else profile.username,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                if (userIsAdmin) AdminBadgeLabel()
                                if (profile.hasBadgeENI) ENIBadgeLabel()
                            }
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (profile.classeENI.isNotBlank()) {
                                    Text(
                                        profile.classeENI,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (profile.moodEmoji.isNotBlank()) {
                                    Text(
                                        "${profile.moodEmoji} ${profile.moodText}".trim(),
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (profile.streak > 1) {
                            Text(
                                "🔥 ${profile.streak}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        modifier  = Modifier.padding(start = 74.dp),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}