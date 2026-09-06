package com.rnandresy.lol.ui.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.bubbleShell
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.components.*
import com.rnandresy.lol.utils.STORY_MARKS
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel
import androidx.compose.ui.draw.clip
@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun CreateGroupScreen(
    vm: AskipViewModel,
    onDone: (String) -> Unit,
    onBack: () -> Unit
) {
    val allProfiles by vm.allProfiles.collectAsState()
    val loading     by vm.loading.collectAsState()

    var groupName   by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var emoji       by remember { mutableStateOf(STORY_MARKS.first()) }
    val selected    = remember { mutableStateListOf<String>() }

    val canCreate = groupName.isNotBlank() && selected.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text("Nouveau groupe", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    BubbleIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Retour",
                        onClick = onBack,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                },
                actions = {
                    BubbleButton(
                        text = "Créer",
                        onClick = {
                            vm.createGroup(
                                groupName.trim(), description.trim(), emoji, selected.toList()
                            ) { id -> onDone(id) }
                        },
                        enabled = canCreate && !loading,
                        loading = loading,
                        tone = BubbleTone.PRIMARY,
                        size = BubbleSize.SMALL,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { pad ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Emoji du groupe ───────────────────────────────────────────────
            item {
                Text("Marque du groupe", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(STORY_MARKS) { e ->
                        val picked = emoji == e
                        val palette = LocalAskipPalette.current
                        val shape = RoundedCornerShape(Radius.sm)
                        TapArea(
                            onTap = { emoji = e },
                            scaleDown = 0.9f,
                            modifier = Modifier.bubbleShell(
                                shape,
                                if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else palette.bubble,
                                if (picked) MaterialTheme.colorScheme.primary
                                else palette.bubbleBorder,
                                palette.shadow,
                                if (picked) 5.dp else 2.dp
                            )
                        ) {
                            AskipGlyph(kind = glyphForSymbol(e), size = 21.dp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }

            // ── Nom ───────────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = groupName,
                    onValueChange = { if (it.length <= 40) groupName = it },
                    label         = { Text("Nom du groupe *") },
                    leadingIcon   = { AskipGlyph(kind = glyphForSymbol(emoji), size = 16.dp) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value         = description,
                    onValueChange = { if (it.length <= 100) description = it },
                    label         = { Text("Description (optionnel)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )
            }

            // ── Membres sélectionnés ──────────────────────────────────────────
            if (selected.isNotEmpty()) {
                item {
                    Text("Membres ajoutés (${selected.size})",
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selected) { uid ->
                            val p = vm.allProfiles.value.find { it.userId == uid }
                            if (p != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box {
                                        AskipAvatar(username = p.username, photoUrl = p.photoUrl, size = 40.dp)
                                        BubbleIconButton(
                                            icon = Icons.Default.Close,
                                            contentDescription = "Retirer ${p.username}",
                                            onClick = { selected.remove(uid) },
                                            tone = BubbleTone.DANGER,
                                            diameter = 20.dp,
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        )
                                    }
                                    Text(p.username.take(8), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── Liste membres ─────────────────────────────────────────────────
            item {
                Text("Ajouter des membres",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }

            items(allProfiles, key = { it.userId }) { profile ->
                val isSelected = profile.userId in selected
                val userIsAdmin = isAdmin(profile.userId) || profile.isAdmin

                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            if (isSelected) 1.5.dp else 0.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (isSelected) selected.remove(profile.userId)
                            else selected.add(profile.userId)
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AskipAvatar(username = profile.username, photoUrl = profile.photoUrl, size = 42.dp, isAdminUser = userIsAdmin)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (profile.classeENI.isNotBlank())
                            Text(profile.classeENI, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
