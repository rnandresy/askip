package com.rnandresy.lol.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.feed.AdminBadge
import com.rnandresy.lol.ui.feed.BadgeChip
import com.rnandresy.lol.utils.ADMIN_BADGE_NAME
import com.rnandresy.lol.utils.ALL_ACHIEVEMENTS
import com.rnandresy.lol.utils.BADGE_COLORS
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val isMe          = userId == vm.currentUserId
    val myProfile    by vm.myProfile.collectAsState()
    val viewedProf   by vm.viewedProfile.collectAsState()
    val allBadges    by vm.allBadges.collectAsState()
    val myBadges     by vm.myBadges.collectAsState()
    val loading      by vm.loading.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()

    val profile       = if (isMe) myProfile else viewedProf
    val achievements  = if (isMe) vm.myAchievements.collectAsState().value
    else vm.viewedAchievements.collectAsState().value

    var showBadgeMgr by remember { mutableStateOf(false) }
    var badgeError   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (!isMe) vm.loadProfile(userId)
    }

    val themeColor = runCatching {
        Color(android.graphics.Color.parseColor(profile?.themeColor ?: "#7C4DFF"))
    }.getOrElse { Color(0xFF7C4DFF) }

    val userIsAdmin  = isAdmin(userId) || profile?.isAdmin == true
    val myIsAdmin    = isAdmin(vm.currentUserId) || myProfile?.isAdmin == true
    val unlockedIds  = achievements.map { it.id }.toSet()

    val displayBadges: List<Badge> = if (isMe) {
        myBadges
    } else {
        allBadges.filter { b -> b.id in (profile?.badgeIds ?: emptyList()) }
    }

    // Pickers
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadAvatar(it) } }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadCover(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = {
                    Text(
                        if (isMe) "Mon profil" else (profile?.username ?: "Profil"),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (isMe) {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, null)
                        }
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Default.Edit, null)
                        }
                    } else if (profile != null) {
                        IconButton(onClick = {
                            val me = myProfile ?: return@IconButton
                            vm.startConversation(profile.userId, profile.username) {
                                onOpenChat(it)
                            }
                        }) {
                            Icon(Icons.Default.Chat, null)
                        }
                    }
                }
            )
        }
    ) { pad ->

        if (profile == null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Bannière / Couverture ─────────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    if (profile.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model              = profile.coverUrl,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(
                                    listOf(
                                        themeColor.copy(alpha = 0.55f),
                                        themeColor,
                                        themeColor.copy(alpha = 0.75f)
                                    )
                                )
                            )
                        )
                    }

                    if (profile.moodEmoji.isNotBlank()) {
                        Surface(
                            color    = Color.Black.copy(alpha = 0.3f),
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(profile.moodEmoji, fontSize = 16.sp)
                                if (profile.moodText.isNotBlank()) {
                                    Text(profile.moodText, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }

                    // Barre de progression upload
                    if (isMe && loading && uploadProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { uploadProgress / 100f },
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        )
                    }

                    if (isMe) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Bouton supprimer couverture (visible seulement si photo existe)
                            if (profile.coverUrl.isNotBlank()) {
                                IconButton(
                                    onClick  = { vm.deleteCoverPhoto() },
                                    enabled  = !loading
                                ) {
                                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                                        Icon(
                                            Icons.Default.Delete, null,
                                            tint     = Color.White,
                                            modifier = Modifier.padding(6.dp).size(16.dp)
                                        )
                                    }
                                }
                            }
                            // Bouton changer couverture
                            IconButton(
                                onClick  = {
                                    coverPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled  = !loading
                            ) {
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate, null,
                                        tint     = Color.White,
                                        modifier = Modifier.padding(8.dp).size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Avatar ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier         = Modifier.offset(y = (-42).dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Cercle avatar
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .then(
                                if (isMe && !loading)
                                    Modifier.clickable {
                                        avatarPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                else Modifier
                            )
                    ) {
                        if (profile.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model              = profile.photoUrl,
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (userIsAdmin) Color(0xFFFFD700).copy(alpha = 0.22f)
                                        else themeColor.copy(alpha = 0.28f)
                                    )
                                    .then(
                                        if (userIsAdmin)
                                            Modifier.border(2.dp, Color(0xFFFFD700), CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    profile.username.firstOrNull()?.uppercase() ?: "?",
                                    fontSize   = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = if (userIsAdmin) Color(0xFFFFD700) else themeColor
                                )
                            }
                        }
                    }

                    // Indicateurs bas-droite de l'avatar
                    if (isMe) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Bouton supprimer la photo (si photo existe)
                            if (profile.photoUrl.isNotBlank()) {
                                Surface(
                                    shape    = CircleShape,
                                    color    = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable(enabled = !loading) { vm.deleteProfilePhoto() }
                                ) {
                                    Icon(
                                        Icons.Default.Close, null,
                                        tint     = Color.White,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                            // Bouton appareil photo (changer)
                            Surface(
                                shape    = CircleShape,
                                color    = if (loading) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.padding(5.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt, null,
                                        tint     = Color.White,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Cadre avatar
                    if (profile.avatarFrame != "none") {
                        val frameEmoji = when (profile.avatarFrame) {
                            "fire"    -> "🔥"; "star" -> "⭐"
                            "rainbow" -> "🌈"; "gold" -> "👑"
                            else -> ""
                        }
                        if (frameEmoji.isNotBlank()) {
                            Text(
                                frameEmoji,
                                fontSize = 18.sp,
                                modifier = Modifier.align(Alignment.TopStart).offset(x = (-2).dp, y = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── Nom + badges ──────────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .offset(y = (-30).dp)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Nom + badges officiels
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            profile.username,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (userIsAdmin) AdminBadge()
                        if (profile.hasBadgeENI) ENIBadge()
                    }

                    // Classe ENI
                    if (profile.classeENI.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "🎓 ${profile.classeENI}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Badges personnalisés
                    if (displayBadges.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding        = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(displayBadges, key = { it.id }) { badge ->
                                if (isMe) {
                                    BadgeChipManageable(
                                        badge     = badge,
                                        canEdit   = badge.createdBy == vm.currentUserId || myIsAdmin,
                                        canDelete = badge.createdBy == vm.currentUserId || myIsAdmin,
                                        onUnwear  = { vm.unwearBadge(badge.id) },
                                        onEdit    = { showBadgeMgr = true },
                                        onDelete  = {
                                            vm.deleteBadge(
                                                badge.id,
                                                onSuccess = {},
                                                onError   = { badgeError = it }
                                            )
                                        }
                                    )
                                } else {
                                    BadgeChip(badge.displayName, badge.colorHex)
                                }
                            }
                        }
                    }

                    // Bio
                    if (profile.bio.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            profile.bio,
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Infos perso
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (profile.age > 0)
                            item { InfoChip("🎂 ${profile.age} ans") }
                        if (profile.relationshipStatus.isNotBlank())
                            item { InfoChip("💑 ${profile.relationshipStatus}") }
                        if (profile.streak > 1)
                            item { InfoChip("🔥 ${profile.streak} jours actifs") }
                    }

                    // ── Activité ──────────────────────────────────────────────
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        StatBlock("${profile.postsCount}",    "posts")
                        StatBlock("${profile.commentsCount}", "coms")
                        StatBlock("${profile.storiesCount}",  "stories")
                    }

                    // ── Trophées ──────────────────────────────────────────────
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "🏆 Trophées",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (unlockedIds.isNotEmpty()) {
                            TextButton(onClick = { onAchievements(userId) }) {
                                Text("Voir tout (${unlockedIds.size}/${ALL_ACHIEVEMENTS.size})")
                            }
                        }
                    }

                    if (unlockedIds.isEmpty()) {
                        Text(
                            "Aucun trophée encore… commence à poster ! 💪",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                ALL_ACHIEVEMENTS
                                    .filter { it.id in unlockedIds }
                                    .take(5)
                            ) { def ->
                                val ac = runCatching {
                                    Color(android.graphics.Color.parseColor(def.color))
                                }.getOrElse { Color.Gray }
                                Surface(
                                    color    = ac.copy(alpha = 0.12f),
                                    shape    = RoundedCornerShape(12.dp),
                                    modifier = Modifier.border(
                                        1.dp, ac.copy(alpha = 0.4f), RoundedCornerShape(12.dp)
                                    )
                                ) {
                                    Column(
                                        modifier            = Modifier.padding(
                                            horizontal = 10.dp, vertical = 7.dp
                                        ),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(def.icon, fontSize = 20.sp)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            def.title,
                                            style      = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color      = ac,
                                            fontSize   = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Bouton gérer badges (isMe) ─────────────────────────────
                    if (isMe) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showBadgeMgr = true; badgeError = null },
                            shape   = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Style, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gérer mes badges")
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    // ── Dialog gestion badges ─────────────────────────────────────────────────
    if (showBadgeMgr && isMe) {
        BadgeManagerDialog(
            vm        = vm,
            myProfile = myProfile,
            myBadges  = myBadges,
            allBadges = allBadges,
            error     = badgeError,
            onDismiss = { showBadgeMgr = false; badgeError = null },
            onError   = { badgeError = it }
        )
    }
}

// ── Composants locaux ─────────────────────────────────────────────────────────

@Composable
fun ENIBadge() {
    Surface(
        color    = Color(0xFF1565C0).copy(alpha = 0.15f),
        shape    = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, Color(0xFF1565C0), RoundedCornerShape(6.dp))
    ) {
        Text(
            "🎓 ENI",
            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF1565C0)
        )
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        shape  = RoundedCornerShape(50)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style    = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
    }.getOrElse { Color(0xFF7C4DFF) }

    Box {
        Surface(
            color    = color.copy(alpha = 0.15f),
            shape    = RoundedCornerShape(50),
            modifier = Modifier
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable { showMenu = true }
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    badge.displayName,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.MoreVert, null,
                    tint     = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text        = { Text("Ne plus porter") },
                leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                onClick     = { showMenu = false; onUnwear() }
            )
            if (canEdit) {
                DropdownMenuItem(
                    text        = { Text("Modifier") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick     = { showMenu = false; onEdit() }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text        = {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
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

                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = tab == i,
                            onClick  = {
                                tab       = i
                                editBadge = null
                                badgeName  = ""
                                badgeColor = BADGE_COLORS.first()
                            }
                        ) {
                            Text(title, modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (tab) {

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
                                        BadgeChip(badge.displayName, badge.colorHex)
                                        Spacer(Modifier.width(2.dp))
                                        IconButton(
                                            onClick  = { vm.unwearBadge(badge.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close, null,
                                                tint     = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
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
                                BadgeChip(badgeName, badgeColor)
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
                                        BadgeChip(badge.displayName, badge.colorHex)
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
                                    Icons.Default.ArrowBack, null,
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
                                    BadgeChip(badgeName, badgeColor)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick  = {
                                    editBadge?.let { b ->
                                        vm.updateBadge(b.id, badgeName, badgeColor,
                                            onSuccess = { editBadge = null },
                                            onError   = onError
                                        )
                                    }
                                },
                                enabled  = badgeName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Enregistrer") }

                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick  = {
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
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete, null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Supprimer ce badge")
                            }
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
                1 -> Button(
                    onClick  = {
                        vm.createOrWearBadge(
                            displayName = badgeName,
                            colorHex    = badgeColor,
                            onSuccess   = onDismiss,
                            onError     = onError
                        )
                    },
                    enabled = badgeName.isNotBlank()
                ) {
                    val existsAlready = allBadges.any {
                        it.name == badgeName.trim().lowercase()
                    }
                    Text(if (existsAlready) "Porter ce badge" else "Créer")
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