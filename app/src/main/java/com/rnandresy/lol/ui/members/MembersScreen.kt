package com.rnandresy.lol.ui.members

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.rnandresy.lol.ui.feed.BadgeChip
import com.rnandresy.lol.ui.feed.UserAvatar
import com.rnandresy.lol.ui.profile.ENIBadge
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: AskipViewModel,
    onOpenProfile: (String) -> Unit
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    val allBadges by viewModel.allBadges.collectAsState()

    // Tri: admin → badgé → normal, puis par username
    val sortedProfiles = remember(allProfiles, allBadges) {
        val all = buildList {
            myProfile?.let { add(it) }
            addAll(allProfiles)
        }.distinctBy { it.userId }

        all.sortedWith(
            compareByDescending<UserProfile> { isAdmin(it.userId) || it.isAdmin }
                .thenByDescending { it.badgeIds.size }
                .thenBy { it.username.lowercase() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Membres 👥", fontWeight = FontWeight.ExtraBold) })
        }
    ) { pad ->
        if (sortedProfiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Aucun membre 😢", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad)) {
                items(sortedProfiles, key = { it.userId }) { profile ->
                    val userIsAdmin = isAdmin(profile.userId) || profile.isAdmin
                    val badges = profile.badgeIds.mapNotNull { bid -> allBadges.find { it.id == bid } }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProfile(profile.userId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            photoUrl = profile.photoUrl,
                            username = profile.username,
                            size = 48,
                            isAdmin = userIsAdmin,
                            onClick = { onOpenProfile(profile.userId) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(profile.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (userIsAdmin) AdminBadge()
                                if (profile.hasBadgeENI) ENIBadge()
                            }
                            if (badges.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    badges.take(3).forEach { BadgeChip(it) }
                                }
                            }
                            if (profile.bio.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(profile.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(start = 76.dp))
                }
            }
        }
    }
}