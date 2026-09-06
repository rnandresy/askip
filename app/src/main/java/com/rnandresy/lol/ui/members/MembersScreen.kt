package com.rnandresy.lol.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.ENIBadgeLabel
import com.rnandresy.lol.ui.components.EmptyState
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Space
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
                title = { Text("Membres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    BubbleIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Retour",
                        onClick = onBack,
                        modifier = Modifier.padding(start = 6.dp)
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
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                EmptyState("👥", "Aucun membre")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.xs),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item(key = "count") {
                    Text(
                        "${sorted.size} membre(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Space.xxs)
                    )
                }
                items(sorted, key = { it.userId }) { profile ->
                    MemberRow(
                        profile = profile,
                        isMe = profile.userId == vm.currentUserId,
                        onOpen = { onOpenProfile(profile.userId) }
                    )
                }
            }
        }
    }
}
/**
 * Une ligne de membre.
 *
 * Chaque membre est une carte-bulle plutôt qu'une ligne séparée d'un trait :
 * la liste se parcourt par blocs, et l'appui a le même ressort que partout.
 */
@Composable
private fun MemberRow(
    profile: UserProfile,
    isMe: Boolean,
    onOpen: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val userIsAdmin = isAdmin(profile.userId) || profile.isAdmin
    val tap = rememberTapFeedback()

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        gloss = 0.28f,
        onClick = { tap(); onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            AskipAvatar(
                username = profile.username,
                photoUrl = profile.photoUrl,
                size = 46.dp,
                isAdminUser = userIsAdmin,
                onClick = onOpen
            )

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Text(
                        if (isMe) "${profile.username} (Moi)" else profile.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (userIsAdmin) AdminBadgeLabel()
                    if (profile.hasBadgeENI) ENIBadgeLabel()
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // La série ne s'affiche qu'à partir de deux jours : « 🔥 1 » n'est
            // pas une série, c'est juste être venu aujourd'hui.
            if (profile.streak > 1) {
                BubbleChip(
                    label = "${profile.streak}",
                    emoji = "🔥",
                    accent = palette.streak
                )
            }
        }
    }
}
