package com.rnandresy.lol.ui.members

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.feed.AdminBadge
import com.rnandresy.lol.ui.feed.UserAvatar
import com.rnandresy.lol.ui.profile.ENIBadge
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
                // Admin en premier, puis par ordre alphabétique — aucun tri par popularité
                compareByDescending<UserProfile> { isAdmin(it.userId) || it.isAdmin }
                    .thenByDescending { it.hasBadgeENI }
                    .thenBy { it.username.lowercase() }
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Membres du campus 👥") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { pad ->
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Tsisy membre 😢", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad)) {
                items(sorted, key = { it.userId }) { profile ->
                    val userIsAdmin = isAdmin(profile.userId) || profile.isAdmin
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProfile(profile.userId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            username = profile.username,
                            photoUrl = profile.photoUrl,
                            size     = 50,
                            isAdmin  = userIsAdmin,
                            onClick  = { onOpenProfile(profile.userId) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    profile.username,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (userIsAdmin) AdminBadge()
                                if (profile.hasBadgeENI) ENIBadge()
                            }
                            if (profile.classeENI.isNotBlank()) {
                                Text(
                                    "🎓 ${profile.classeENI}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.moodEmoji.isNotBlank()) {
                                Text(
                                    "${profile.moodEmoji} ${profile.moodText}".trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (profile.streak > 1) {
                            Text(
                                "🔥${profile.streak}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider(
                        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 78.dp)
                    )
                }
            }
        }
    }
}