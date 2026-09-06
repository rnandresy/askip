@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.CustomBadgeChip
import com.rnandresy.lol.ui.components.ProgressTrack
import com.rnandresy.lol.ui.components.SlidingSegmented
import com.rnandresy.lol.ui.components.StarDust
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.softGlow
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.ADMIN_BADGE_NAME
import com.rnandresy.lol.utils.ALL_ACHIEVEMENTS
import com.rnandresy.lol.utils.BADGE_COLORS
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

/**
 * Le profil.
 *
 * C'est, avec le fil, l'écran qu'on ouvre vraiment — il méritait mieux qu'une
 * longue colonne de sections flottantes. Tout est désormais regroupé en trois
 * cartes : qui tu es, ta réputation, ton activité. Les photos se changent
 * depuis l'image elle-même, et les badges vivent dans leur propre carte.
 */
@Composable
fun ProfileScreen(
    vm: AskipViewModel,
    userId: String,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenChat: (String) -> Unit,
    onAchievements: (String) -> Unit,
    onSettings: () -> Unit
) {
    val isMe = userId == vm.currentUserId
    val myProfile by vm.myProfile.collectAsState()
    val viewedProf by vm.viewedProfile.collectAsState()
    val allBadges by vm.allBadges.collectAsState()
    val myBadges by vm.myBadges.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()
    // Pendant un envoi, les commandes photo se verrouillent : sans ça, deux
    // appuis lancent deux uploads pour la même image.
    val busy by vm.loading.collectAsState()

    val profile = if (isMe) myProfile else viewedProf
    val achievements = if (isMe) vm.myAchievements.collectAsState().value
    else vm.viewedAchievements.collectAsState().value

    var showBadgeMgr by remember { mutableStateOf(false) }
    var badgeError by remember { mutableStateOf<String?>(null) }

    // « Introuvable » ne doit s'afficher qu'une fois la lecture terminée, sinon
    // chaque ouverture de profil clignote sur un faux message d'erreur. Le sien
    // arrive par un écouteur permanent : là, on attend, tout simplement.
    var lookupDone by remember(userId) { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (!isMe) {
            vm.loadProfile(userId).join()
            lookupDone = true
        }
    }

    val userIsAdmin = isAdmin(userId) || profile?.isAdmin == true
    val unlockedIds = achievements.map { it.id }.toSet()

    val displayBadges: List<Badge> = if (isMe) {
        myBadges
    } else {
        allBadges.filter { it.id in (profile?.badgeIds ?: emptyList()) }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadAvatar(it) } }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadCover(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isMe) "Mon profil" else (profile?.username ?: "Profil"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isMe) {
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onBack,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        modifier = Modifier.padding(end = Space.lg)
                    ) {
                        if (isMe) {
                            BubbleIconButton(
                                icon = Icons.Rounded.Edit,
                                contentDescription = "Modifier le profil",
                                onClick = onEditProfile
                            )
                            BubbleIconButton(
                                icon = Icons.Rounded.Settings,
                                contentDescription = "Réglages",
                                onClick = onSettings
                            )
                        } else if (profile != null) {
                            BubbleIconButton(
                                icon = Icons.Rounded.ChatBubble,
                                contentDescription = "Envoyer un message",
                                tone = BubbleTone.PRIMARY,
                                onClick = {
                                    vm.startConversation(profile.userId, profile.username) {
                                        onOpenChat(it)
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (profile == null) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                if (lookupDone) {
                    Text(
                        "Profil introuvable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = Space.huge),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item(key = "head") {
                ProfileHeader(
                    profile = profile,
                    isMe = isMe,
                    userIsAdmin = userIsAdmin,
                    uploadProgress = uploadProgress,
                    busy = busy,
                    onPickCover = {
                        coverPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onPickAvatar = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onRemoveCover = vm::deleteCoverPhoto,
                    onRemoveAvatar = vm::deleteProfilePhoto
                )
            }

            item(key = "identity") {
                IdentityCard(profile = profile, userIsAdmin = userIsAdmin)
            }

            item(key = "reputation") { ReputationCard(profile) }

            item(key = "activity") { ActivityCard(profile) }

            item(key = "trophies") {
                TrophyCard(
                    unlocked = unlockedIds.size,
                    total = ALL_ACHIEVEMENTS.size,
                    recent = ALL_ACHIEVEMENTS.filter { it.id in unlockedIds }.takeLast(6),
                    onOpen = { onAchievements(userId) }
                )
            }

            item(key = "badges") {
                BadgeCard(
                    badges = displayBadges,
                    isMe = isMe,
                    canManage = isMe,
                    myIsAdmin = isAdmin(vm.currentUserId) || myProfile?.isAdmin == true,
                    currentUid = vm.currentUserId,
                    onManage = { showBadgeMgr = true },
                    onUnwear = { vm.unwearBadge(it) },
                    onDelete = { id -> vm.deleteBadge(id, {}, { badgeError = it }) }
                )
            }
        }
    }

    if (showBadgeMgr && isMe) {
        BadgeManagerDialog(
            vm = vm,
            myProfile = myProfile,
            myBadges = myBadges,
            allBadges = allBadges,
            error = badgeError,
            onDismiss = { showBadgeMgr = false; badgeError = null },
            onError = { badgeError = it }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  En-tête : couverture + avatar
// ═════════════════════════════════════════════════════════════════════════════

/**
 * La couverture et l'avatar.
 *
 * Les boutons de photo étaient éparpillés en petits ronds noirs à demi
 * transparents sur l'image. Ils deviennent des bulles, posées au même endroit
 * pour la couverture et pour l'avatar — un seul geste à apprendre.
 */
@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isMe: Boolean,
    userIsAdmin: Boolean,
    uploadProgress: Int,
    busy: Boolean,
    onPickCover: () -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveCover: () -> Unit,
    onRemoveAvatar: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(profile.themeColor))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    Box(modifier = Modifier.fillMaxWidth()) {
        // ── Couverture ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.15f))
                    )
                )
        ) {
            if (profile.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = profile.coverUrl,
                    contentDescription = "Couverture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StarDust(count = 20, seed = 13, tint = Color.White)
            }

            // Un voile sombre en bas : sans lui, l'avatar blanc disparaît sur
            // une couverture claire.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, palette.mediaScrim)
                        )
                    )
            )

            if (isMe) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Space.md),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    if (profile.coverUrl.isNotBlank()) {
                        BubbleChip(
                            "Retirer",
                            emoji = "🗑️",
                            enabled = !busy,
                            onClick = onRemoveCover
                        )
                    }
                    BubbleChip(
                        "Couverture",
                        emoji = "🖼️",
                        enabled = !busy,
                        onClick = onPickCover
                    )
                }
            }

            if (uploadProgress in 1..99) {
                ProgressTrack(
                    progress = uploadProgress / 100f,
                    height = 3.dp,
                    color = accent,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // ── Avatar, à cheval sur la couverture ───────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = Space.lg, y = 44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(128.dp)
                        .background(softGlow(accent, 0.35f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            3.dp,
                            if (userIsAdmin) AdminGold else MaterialTheme.colorScheme.surface,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Photo de profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            profile.username.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                // Le cadre choisi dans « Modifier le profil ». Sans cet
                // affichage, le réglage existerait sans jamais se voir.
                val frameEmoji = when (profile.avatarFrame) {
                    "fire" -> "🔥"
                    "star" -> "⭐"
                    "rainbow" -> "🌈"
                    "gold" -> "👑"
                    else -> ""
                }
                if (frameEmoji.isNotBlank()) {
                    Text(
                        frameEmoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
            }

            if (isMe) {
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (profile.photoUrl.isNotBlank()) {
                        BubbleIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Retirer la photo",
                            onClick = onRemoveAvatar,
                            tone = BubbleTone.SOFT,
                            enabled = !busy,
                            diameter = 30.dp
                        )
                    }
                    BubbleIconButton(
                        icon = Icons.Rounded.PhotoCamera,
                        contentDescription = "Changer la photo",
                        onClick = onPickAvatar,
                        tone = BubbleTone.PRIMARY,
                        enabled = !busy,
                        diameter = 34.dp
                    )
                }
            }
        }

        // Réserve la hauteur que l'avatar déborde.
        Spacer(Modifier.height(214.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Les trois cartes
// ═════════════════════════════════════════════════════════════════════════════

/** Qui tu es : pseudo, statut, humeur, bio, classe. */
@Composable
private fun IdentityCard(profile: UserProfile, userIsAdmin: Boolean) {
    BubbleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg)
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Text(
                    profile.username.ifBlank { "Sans pseudo" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (userIsAdmin) AdminGold else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (userIsAdmin) BubbleChip("Admin", emoji = "👑", accent = AdminGold)
                if (profile.hasBadgeENI) {
                    BubbleChip("ENI", emoji = "🎓", accent = Color(0xFF1565C0))
                }
            }

            if (profile.moodEmoji.isNotBlank() || profile.moodText.isNotBlank()) {
                Text(
                    "${profile.moodEmoji} ${profile.moodText}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (profile.bio.isNotBlank()) {
                Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
            }

            // Les informations d'état civil tenaient chacune sa ligne. Groupées
            // sur une rangée qui défile, elles occupent le quart de la place.
            val facts = buildList {
                if (profile.classeENI.isNotBlank()) add("🏫" to profile.classeENI)
                if (profile.age > 0) add("🎂" to "${profile.age} ans")
                if (profile.relationshipStatus.isNotBlank()) {
                    add("💑" to profile.relationshipStatus)
                }
            }
            if (facts.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    items(facts) { (emoji, label) ->
                        BubbleChip(label, emoji = emoji)
                    }
                }
            }
        }
    }
}

/**
 * La réputation : niveau, clout, fiabilité, flair.
 *
 * Ces quatre chiffres n'étaient nulle part sur le profil — ils vivaient dans
 * l'écran des missions. C'est pourtant ce qui dit le plus de quelqu'un dans
 * une app de rumeurs.
 */
@Composable
private fun ReputationCard(profile: UserProfile) {
    val palette = LocalAskipPalette.current
    val judged = profile.confirmedRumors + profile.debunkedRumors
    val bets = profile.betsWon + profile.betsLost

    BubbleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg)
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Niveau ${profile.level()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        profile.title(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BubbleChip(
                    "${profile.clout} clout",
                    emoji = "🎖",
                    accent = palette.contested
                )
            }

            ProgressTrack(progress = profile.levelProgress())
            Text(
                "${profile.xpToNextLevel()} XP avant le niveau ${profile.level() + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // On n'affiche un pourcentage que s'il repose sur quelque chose :
            // « 100 % de fiabilité » sur une seule rumeur ne veut rien dire.
            if (judged > 0 || bets > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (judged > 0) {
                        ProfileStat(
                            "${(profile.reliability() * 100).toInt()} %",
                            "fiabilité",
                            palette.confirmed
                        )
                        ProfileStat("${profile.confirmedRumors}", "confirmées")
                        ProfileStat("${profile.debunkedRumors}", "démenties")
                    }
                    if (bets > 0) {
                        ProfileStat(
                            "${(profile.betAccuracy() * 100).toInt()} %",
                            "flair",
                            palette.scoop
                        )
                    }
                }
            }
        }
    }
}

/** L'activité : ce qui a été publié, et la régularité. */
@Composable
private fun ActivityCard(profile: UserProfile) {
    val palette = LocalAskipPalette.current

    BubbleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg)
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat("${profile.postsCount}", "rumeurs")
                ProfileStat("${profile.commentsCount}", "commentaires")
                ProfileStat("${profile.storiesCount}", "stories")
                ProfileStat("${profile.confessionsCount}", "confessions")
            }

            if (profile.streak > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    BubbleChip(
                        "${profile.streak} jours d'affilée",
                        emoji = "🔥",
                        accent = palette.streak
                    )
                    if (profile.bestStreak > profile.streak) {
                        Text(
                            "record : ${profile.bestStreak}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    accent: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Trophées et badges
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrophyCard(
    unlocked: Int,
    total: Int,
    recent: List<com.rnandresy.lol.utils.AchievementDef>,
    onOpen: () -> Unit
) {
    BubbleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Trophées",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$unlocked / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (recent.isEmpty()) {
                Text(
                    "Aucun trophée pour l'instant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    items(recent) { def ->
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(def.color))
                        }.getOrElse { MaterialTheme.colorScheme.primary }
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.16f))
                                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(def.icon, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(
    badges: List<Badge>,
    isMe: Boolean,
    canManage: Boolean,
    myIsAdmin: Boolean,
    currentUid: String,
    onManage: () -> Unit,
    onUnwear: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    BubbleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg)
    ) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Text(
                "Badges",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (badges.isEmpty()) {
                Text(
                    if (isMe) "Tu n'en portes aucun pour l'instant."
                    else "Aucun badge porté.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    items(badges, key = { it.id }) { badge ->
                        BadgeChipManageable(
                            badge = badge,
                            canEdit = isMe && (myIsAdmin || badge.createdBy == currentUid),
                            canDelete = myIsAdmin || badge.createdBy == currentUid,
                            onUnwear = { onUnwear(badge.id) },
                            onEdit = onManage,
                            onDelete = { onDelete(badge.id) }
                        )
                    }
                }
            }

            if (canManage) {
                BubbleButton(
                    text = "Gérer mes badges",
                    emoji = "🏷️",
                    onClick = onManage,
                    tone = BubbleTone.SOFT,
                    size = BubbleSize.SMALL,
                    fillWidth = true
                )
            }
        }
    }
}

/**
 * Un badge porté. Un appui long ouvre ce qu'on peut en faire — retirer,
 * renommer, supprimer — plutôt qu'un menu visible en permanence.
 */
@Composable
fun BadgeChipManageable(
    badge: Badge,
    canEdit: Boolean,
    canDelete: Boolean,
    onUnwear: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = runCatching {
        Color(android.graphics.Color.parseColor(badge.colorHex))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    Box {
        BubbleChip(
            label = badge.displayName,
            emoji = "🏷️",
            accent = color,
            filled = true,
            onClick = { if (canEdit || canDelete) showMenu = true }
        )

        if (showMenu) {
            AlertDialog(
                onDismissRequest = { showMenu = false },
                title = { Text(badge.displayName, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Que veux-tu faire de ce badge ?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showMenu = false; onUnwear() }) {
                        Text("Retirer")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        if (canEdit) {
                            TextButton(onClick = { showMenu = false; onEdit() }) {
                                Text("Modifier")
                            }
                        }
                        if (canDelete) {
                            TextButton(onClick = { showMenu = false; onDelete() }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(Radius.lg)
            )
        }
    }
}

/** Le badge ENI officiel — conservé pour les écrans qui l'affichent encore. */
@Composable
fun ENIBadge() {
    BubbleChip("ENI", emoji = "🎓", accent = Color(0xFF1565C0))
}

@Composable
fun InfoChip(text: String) {
    BubbleChip(text)
}

@Composable
fun StatBlock(value: String, label: String) {
    ProfileStat(value, label)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeManagerDialog(
    vm: AskipViewModel,
    myProfile: UserProfile?,
    myBadges: List<Badge>,
    allBadges: List<Badge>,
    error: String?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val myIsAdmin       = isAdmin(vm.currentUserId) || myProfile?.isAdmin == true
    val availableBadges = allBadges.filter { b ->
        myBadges.none { it.id == b.id } && b.name != ADMIN_BADGE_NAME
    }

    var tab       by remember { mutableStateOf(0) }
    var editBadge by remember { mutableStateOf<Badge?>(null) }
    var badgeName  by remember { mutableStateOf("") }
    var badgeColor by remember { mutableStateOf(BADGE_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("🏷️ Badges") },
        text             = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {

                val tabs = buildList {
                    add("Porter")
                    add("Créer")
                    val canModify = myBadges.any {
                        it.createdBy == vm.currentUserId
                    } || myIsAdmin
                    if (canModify) add("Modifier")
                }

                // L'onglet « Modifier » disparaît quand on supprime son dernier
                // badge : sans ce recadrage, la pastille indiquerait un onglet
                // et l'écran en afficherait un autre.
                val current = tab.coerceIn(0, tabs.lastIndex)

                SlidingSegmented(
                    items = tabs,
                    selected = tabs[current],
                    labelOf = { it },
                    onSelect = { title ->
                        tab = tabs.indexOf(title)
                        editBadge = null
                        badgeName = ""
                        badgeColor = BADGE_COLORS.first()
                    }
                )

                Spacer(Modifier.height(12.dp))

                when (current) {

                    // ── Porter ────────────────────────────────────────────────
                    0 -> {
                        // Badges portés actuellement
                        if (myBadges.isNotEmpty()) {
                            Text(
                                "Portés :",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(myBadges, key = { it.id }) { badge ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CustomBadgeChip(badge.displayName, badge.colorHex)
                                        Spacer(Modifier.width(2.dp))
                                        BubbleIconButton(
                                            icon = Icons.Default.Close,
                                            contentDescription =
                                                "Ne plus porter ${badge.displayName}",
                                            onClick = { vm.unwearBadge(badge.id) },
                                            tone = BubbleTone.DANGER,
                                            diameter = 22.dp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // Badges disponibles à porter
                        if (availableBadges.isEmpty()) {
                            Text(
                                "Aucun autre badge disponible pour l'instant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Disponibles :",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Column(
                                modifier            = Modifier.heightIn(max = 220.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableBadges.forEach { badge ->
                                    val c = runCatching {
                                        Color(android.graphics.Color.parseColor(badge.colorHex))
                                    }.getOrElse { Color(0xFF7C4DFF) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                1.dp,
                                                c.copy(alpha = 0.35f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                vm.wearExistingBadge(
                                                    badgeId   = badge.id,
                                                    onSuccess = { onDismiss() },
                                                    onError   = onError
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                        )
                                        Text(
                                            badge.displayName,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color      = c
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Créer ─────────────────────────────────────────────────
                    1 -> {
                        if (!myIsAdmin && myBadges.isNotEmpty()) {
                            Surface(
                                color  = MaterialTheme.colorScheme.surfaceVariant,
                                shape  = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "ℹ️ Tu peux créer un badge, mais tu devras d'abord retirer le tien pour le porter.",
                                    modifier = Modifier.padding(10.dp),
                                    style    = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value         = badgeName,
                            onValueChange = { if (it.length <= 20) badgeName = it },
                            label         = { Text("Nom du badge (20 max)") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Couleur :",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        BadgeColorPicker(selected = badgeColor) { badgeColor = it }

                        if (badgeName.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Aperçu : ",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                CustomBadgeChip(badgeName, badgeColor)
                            }
                        }
                    }

                    // ── Modifier ──────────────────────────────────────────────
                    2 -> {
                        val editable = if (myIsAdmin) myBadges
                        else myBadges.filter { it.createdBy == vm.currentUserId }

                        if (editBadge == null) {
                            if (editable.isEmpty()) {
                                Text(
                                    "Aucun badge à modifier.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "Sélectionne un badge :",
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                editable.forEach { badge ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editBadge  = badge
                                                badgeName  = badge.displayName
                                                badgeColor = badge.colorHex
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        CustomBadgeChip(badge.displayName, badge.colorHex)
                                        Icon(
                                            Icons.Default.ChevronRight, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            // Formulaire modification
                            TextButton(onClick = { editBadge = null }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack, null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Retour")
                            }

                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value         = badgeName,
                                onValueChange = { if (it.length <= 20) badgeName = it },
                                label         = { Text("Nom") },
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            BadgeColorPicker(selected = badgeColor) { badgeColor = it }

                            if (badgeName.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Aperçu : ", style = MaterialTheme.typography.labelSmall)
                                    CustomBadgeChip(badgeName, badgeColor)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            BubbleButton(
                                text = "Enregistrer",
                                onClick = {
                                    editBadge?.let { b ->
                                        vm.updateBadge(b.id, badgeName, badgeColor,
                                            onSuccess = { editBadge = null },
                                            onError   = onError
                                        )
                                    }
                                },
                                enabled = badgeName.isNotBlank(),
                                tone = BubbleTone.PRIMARY,
                                fillWidth = true
                            )

                            Spacer(Modifier.height(6.dp))
                            BubbleButton(
                                text = "Supprimer ce badge",
                                emoji = "🗑️",
                                onClick = {
                                    editBadge?.let { b ->
                                        vm.deleteBadge(b.id,
                                            onSuccess = {
                                                editBadge = null
                                                onDismiss()
                                            },
                                            onError = onError
                                        )
                                    }
                                },
                                tone = BubbleTone.DANGER,
                                fillWidth = true
                            )
                        }
                    }
                }

                // Message d'erreur
                error?.let { e ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        e,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            when (tab) {
                1 -> {
                    // Le libellé dit ce qui va vraiment se passer : un badge
                    // qui existe déjà se porte, il ne se recrée pas.
                    val existsAlready = allBadges.any {
                        it.name == badgeName.trim().lowercase()
                    }
                    BubbleButton(
                        text = if (existsAlready) "Porter ce badge" else "Créer",
                        onClick = {
                            vm.createOrWearBadge(
                                displayName = badgeName,
                                colorHex    = badgeColor,
                                onSuccess   = onDismiss,
                                onError     = onError
                            )
                        },
                        enabled = badgeName.isNotBlank(),
                        tone = BubbleTone.PRIMARY,
                        size = BubbleSize.SMALL
                    )
                }
                else -> TextButton(onClick = onDismiss) { Text("Fermer") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun BadgeColorPicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(BADGE_COLORS.take(6), BADGE_COLORS.drop(6)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hex ->
                    val c = runCatching {
                        Color(android.graphics.Color.parseColor(hex))
                    }.getOrElse { Color.Gray }
                    Box(
                        modifier         = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (selected == hex)
                                    Modifier.border(2.5.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { onSelect(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected == hex) {
                            Icon(
                                Icons.Default.Check, null,
                                tint     = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}