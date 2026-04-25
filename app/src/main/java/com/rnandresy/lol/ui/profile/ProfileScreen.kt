package com.rnandresy.lol.ui.profile

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
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
import com.rnandresy.lol.ui.theme.PurplePrimary
import com.rnandresy.lol.utils.ADMIN_BADGE_NAME
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AskipViewModel,
    userId: String,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val isMe = userId == viewModel.currentUserId
    val myProfile by viewModel.myProfile.collectAsState()
    val viewedProfile by viewModel.viewedProfile.collectAsState()
    val profile = if (isMe) myProfile else viewedProfile
    val allBadges by viewModel.allBadges.collectAsState()

    // Dialogs état
    var showBadgeManager by remember { mutableStateOf(false) }
    var badgeDialogError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (!isMe) viewModel.loadProfile(userId)
    }

    val userBadges = profile?.badgeIds?.mapNotNull { bid -> allBadges.find { it.id == bid } } ?: emptyList()
    val userIsAdmin = isAdmin(userId) || profile?.isAdmin == true
    val myIsAdmin = isAdmin(viewModel.currentUserId) || myProfile?.isAdmin == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMe) "Mon profil" else (profile?.username ?: "Profil")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (isMe) {
                        IconButton(onClick = onEditProfile) { Icon(Icons.Default.Edit, null) }
                    } else if (profile != null) {
                        IconButton(onClick = {
                            val me = myProfile ?: return@IconButton
                            viewModel.startConversation(profile.userId, profile.username, profile.photoUrl) {
                                onOpenChat(it)
                            }
                        }) { Icon(Icons.Default.Chat, null) }
                    }
                }
            )
        }
    ) { pad ->

        if (profile == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Banner ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3700B3), PurplePrimary, Color(0xFF00BCD4))
                            )
                        )
                )
            }

            // ── Avatar ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .offset(y = (-44).dp)
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    if (profile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                if (userIsAdmin) Color(0xFFFFD700).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                profile.username.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (userIsAdmin) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Infos ─────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-32).dp)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            profile.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (userIsAdmin) AdminBadge()
                        if (profile.hasBadgeENI) ENIBadge()
                    }

                    // Badges portés
                    if (userBadges.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(userBadges) { badge ->
                                if (isMe) {
                                    // Badge cliquable pour gérer (unwear/edit/delete)
                                    BadgeChipManageable(
                                        badge = badge,
                                        canEdit = badge.createdBy == viewModel.currentUserId || myIsAdmin,
                                        canDelete = badge.createdBy == viewModel.currentUserId || myIsAdmin,
                                        onUnwear = { viewModel.unwearBadge(badge.id) },
                                        onEdit = { showBadgeManager = true },
                                        onDelete = {
                                            viewModel.deleteBadge(badge.id,
                                                onSuccess = {},
                                                onError = { badgeDialogError = it }
                                            )
                                        }
                                    )
                                } else {
                                    BadgeChip(badge)
                                }
                            }
                        }
                    }

                    if (profile.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            profile.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Chips d'info
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (profile.age > 0) item { InfoChip("🎂 ${profile.age} ans") }
                        if (profile.relationshipStatus.isNotBlank()) item { InfoChip("💑 ${profile.relationshipStatus}") }
                        if (profile.classeENI.isNotBlank()) item { InfoChip("🎓 ${profile.classeENI}") }
                    }

                    // Bouton gestion badge (seulement pour moi)
                    if (isMe) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showBadgeManager = true; badgeDialogError = null },
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Style, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gérer mes badges")
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // ── Dialog gestion des badges ─────────────────────────────────────────────
    if (showBadgeManager && isMe) {
        BadgeManagerDialog(
            viewModel = viewModel,
            myProfile = myProfile,
            allBadges = allBadges,
            error = badgeDialogError,
            onDismiss = { showBadgeManager = false; badgeDialogError = null },
            onError = { badgeDialogError = it }
        )
    }
}

// ── Badge Manageable ──────────────────────────────────────────────────────────

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
    val color = try { Color(android.graphics.Color.parseColor(badge.colorHex)) } catch (_: Exception) { PurplePrimary }

    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = color.copy(alpha = 0.15f),
            modifier = Modifier
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
                .clickable { showMenu = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(badge.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.MoreVert, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Ne plus porter") },
                leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                onClick = { showMenu = false; onUnwear() }
            )
            if (canEdit) {
                DropdownMenuItem(
                    text = { Text("Modifier") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { showMenu = false; onEdit() }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

// ── Badge Manager Dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeManagerDialog(
    viewModel: AskipViewModel,
    myProfile: UserProfile?,
    allBadges: List<Badge>,
    error: String?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val myIsAdmin = isAdmin(viewModel.currentUserId) || myProfile?.isAdmin == true
    val myBadgeIds = myProfile?.badgeIds ?: emptyList()

    // Badges que je porte
    val wornBadges = allBadges.filter { it.id in myBadgeIds }
    // Badges disponibles (je ne porte pas, pas admin badge)
    val availableBadges = allBadges.filter { it.id !in myBadgeIds && it.name != ADMIN_BADGE_NAME }

    var tab by remember { mutableStateOf(0) } // 0=porter, 1=créer, 2=modifier
    var selectedBadgeToEdit by remember { mutableStateOf<Badge?>(null) }

    // Champs création/modification
    var badgeName by remember { mutableStateOf("") }
    var badgeColor by remember { mutableStateOf("#7C4DFF") }

    val predefinedColors = listOf(
        "#E91E63", "#9C27B0", "#3F51B5", "#2196F3",
        "#009688", "#4CAF50", "#FF9800", "#FF5722",
        "#F44336", "#FFEB3B", "#7C4DFF", "#607D8B"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏷️ Mes badges") },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp)) {

                // Tabs
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0; selectedBadgeToEdit = null }) { Text("Porter", modifier = Modifier.padding(vertical = 10.dp)) }
                    Tab(selected = tab == 1, onClick = { tab = 1; badgeName = ""; selectedBadgeToEdit = null }) { Text("Créer", modifier = Modifier.padding(vertical = 10.dp)) }
                    if (wornBadges.any { it.createdBy == viewModel.currentUserId } || myIsAdmin) {
                        Tab(selected = tab == 2, onClick = { tab = 2 }) { Text("Modifier", modifier = Modifier.padding(vertical = 10.dp)) }
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (tab) {

                    // ── Onglet Porter ─────────────────────────────────────────
                    0 -> {
                        if (availableBadges.isEmpty()) {
                            Text(
                                "Aucun badge disponible à porter",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                "Choisis un badge existant à porter :",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.heightIn(max = 260.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableBadges.forEach { badge ->
                                    val color = try { Color(android.graphics.Color.parseColor(badge.colorHex)) } catch (_: Exception) { PurplePrimary }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.wearExistingBadge(badge.id,
                                                    onSuccess = { onDismiss() },
                                                    onError = onError
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(12.dp).clip(CircleShape).background(color)
                                        )
                                        Text(badge.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
                                    }
                                }
                            }
                        }

                        // Badges portés actuellement
                        if (wornBadges.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Badges actuellement portés :", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            wornBadges.forEach { badge ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgeChip(badge)
                                    TextButton(onClick = { viewModel.unwearBadge(badge.id) }) {
                                        Text("Retirer", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // ── Onglet Créer ──────────────────────────────────────────
                    1 -> {
                        if (!myIsAdmin && myBadgeIds.isNotEmpty()) {
                            Text(
                                "ℹ️ Tu peux créer un badge mais tu dois d'abord retirer le tien pour le porter. Seul l'admin peut en avoir plusieurs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = badgeName,
                            onValueChange = { if (it.length <= 20) badgeName = it },
                            label = { Text("Nom du badge") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Couleur :", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        ColorPicker(predefinedColors, badgeColor) { badgeColor = it }

                        // Aperçu
                        if (badgeName.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Aperçu : ", style = MaterialTheme.typography.labelSmall)
                                BadgeChip(Badge(displayName = badgeName, colorHex = badgeColor))
                            }
                        }
                    }

                    // ── Onglet Modifier ───────────────────────────────────────
                    2 -> {
                        val editableBadges = if (myIsAdmin) wornBadges else wornBadges.filter { it.createdBy == viewModel.currentUserId }

                        if (editableBadges.isEmpty()) {
                            Text("Aucun badge à modifier", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        } else {
                            if (selectedBadgeToEdit == null) {
                                Text("Choisis un badge à modifier :", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                editableBadges.forEach { badge ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedBadgeToEdit = badge
                                            badgeName = badge.displayName
                                            badgeColor = badge.colorHex
                                        }.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BadgeChip(badge)
                                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                TextButton(onClick = { selectedBadgeToEdit = null }) {
                                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Retour")
                                }
                                OutlinedTextField(
                                    value = badgeName,
                                    onValueChange = { if (it.length <= 20) badgeName = it },
                                    label = { Text("Nom du badge") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("Couleur :", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                ColorPicker(predefinedColors, badgeColor) { badgeColor = it }

                                if (badgeName.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Aperçu : ", style = MaterialTheme.typography.labelSmall)
                                        BadgeChip(Badge(displayName = badgeName, colorHex = badgeColor))
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        selectedBadgeToEdit?.let { b ->
                                            viewModel.updateBadge(b.id, badgeName, badgeColor,
                                                onSuccess = { selectedBadgeToEdit = null },
                                                onError = onError
                                            )
                                        }
                                    },
                                    enabled = badgeName.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Enregistrer les modifications") }

                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        selectedBadgeToEdit?.let { b ->
                                            viewModel.deleteBadge(b.id,
                                                onSuccess = { selectedBadgeToEdit = null },
                                                onError = onError
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Supprimer ce badge")
                                }
                            }
                        }
                    }
                }

                // Erreur globale
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            when (tab) {
                1 -> Button(
                    onClick = {
                        viewModel.createOrWearBadge(
                            displayName = badgeName,
                            colorHex = badgeColor,
                            onSuccess = onDismiss,
                            onError = onError
                        )
                    },
                    enabled = badgeName.isNotBlank()
                ) { Text(if (badgeName.isNotBlank() && allBadges.any { it.name == badgeName.trim().lowercase() }) "Porter ce badge" else "Créer") }
                else -> TextButton(onClick = onDismiss) { Text("Fermer") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun ColorPicker(colors: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(colors.take(6), colors.drop(6)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hex ->
                    val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (selected == hex) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier)
                            .clickable { onSelect(hex) }
                    ) {
                        if (selected == hex) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ENIBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF1565C0).copy(alpha = 0.15f),
        modifier = Modifier.border(1.dp, Color(0xFF1565C0), RoundedCornerShape(6.dp))
    ) {
        Text("🎓 ENI", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
    }
}