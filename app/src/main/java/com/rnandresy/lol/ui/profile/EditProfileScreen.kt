package com.rnandresy.lol.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.utils.AVATAR_FRAMES
import com.rnandresy.lol.utils.ENI_CLASSES
import com.rnandresy.lol.utils.STORY_COLORS
import com.rnandresy.lol.utils.STORY_EMOJIS
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    vm: AskipViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val profile   by vm.myProfile.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()

    var username   by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var age        by remember(profile) { mutableStateOf(profile?.age?.let { if (it > 0) it.toString() else "" } ?: "") }
    var bio        by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var relStatus  by remember(profile) { mutableStateOf(profile?.relationshipStatus ?: "") }
    var classeENI  by remember(profile) { mutableStateOf(profile?.classeENI ?: "") }
    var themeColor by remember(profile) { mutableStateOf(profile?.themeColor ?: "#7C4DFF") }
    var frame      by remember(profile) { mutableStateOf(profile?.avatarFrame ?: "none") }
    var moodEmoji  by remember(profile) { mutableStateOf(profile?.moodEmoji ?: "") }
    var moodText   by remember(profile) { mutableStateOf(profile?.moodText ?: "") }

    var eniExpanded    by remember { mutableStateOf(false) }
    var statutExpanded by remember { mutableStateOf(false) }

    val relStatuts = listOf(
        "Célibataire", "En couple", "Fiancé(e)", "Voay", "Mitady..",
        "Marié(e)", "C'est compliqué", "Préfère ne pas dire"
    )

    val oldUsername = profile?.username ?: ""
    val usernameChanged = username.trim() != oldUsername && username.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Modifier le profil ✏️") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSyncing) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (isSyncing) {
                        Row(
                            modifier          = Modifier.padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                "Sync…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        FilledIconButton(
                            onClick = {
                                val data = mutableMapOf<String, Any?>(
                                    "username"           to username.trim(),
                                    "bio"                to bio.trim(),
                                    "relationshipStatus" to relStatus,
                                    "classeENI"          to classeENI,
                                    "hasBadgeENI"        to classeENI.isNotBlank(),
                                    "themeColor"         to themeColor,
                                    "avatarFrame"        to frame,
                                    "moodEmoji"          to moodEmoji,
                                    "moodText"           to moodText.trim()
                                )
                                age.toIntOrNull()?.let { data["age"] = it }
                                vm.updateProfile(data, onSaved)
                            },
                            enabled = username.isNotBlank()
                        ) { Icon(Icons.Default.Save, null) }
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier            = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Info synchro pseudo ───────────────────────────────────────────
            if (usernameChanged) {
                Surface(
                    color  = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sync, null,
                            tint     = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Le nouveau pseudo sera synchronisé sur tous tes anciens posts, commentaires et messages.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // ── Humeur ───────────────────────────────────────────────────────
            SectionLabel("Humeur du jour 😊")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(STORY_EMOJIS.take(12)) { emoji ->
                    Surface(
                        onClick = { moodEmoji = if (moodEmoji == emoji) "" else emoji },
                        color   = if (moodEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape   = RoundedCornerShape(10.dp)
                    ) {
                        Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(6.dp))
                    }
                }
            }
            if (moodEmoji.isNotBlank()) {
                OutlinedTextField(
                    value           = moodText,
                    onValueChange   = { if (it.length <= 50) moodText = it },
                    label           = { Text("Texte humeur (optionnel)") },
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            // ── Couleur thème ─────────────────────────────────────────────────
            SectionLabel("Couleur du profil 🎨")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(STORY_COLORS) { hex ->
                    val c = runCatching {
                        Color(android.graphics.Color.parseColor(hex))
                    }.getOrElse { Color.Gray }
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(c)
                            .then(if (themeColor == hex) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable { themeColor = hex }
                    )
                }
            }

            // ── Cadre avatar ──────────────────────────────────────────────────
            SectionLabel("Cadre avatar 🖼️")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AVATAR_FRAMES.entries.toList()) { (key, label) ->
                    FilterChip(
                        selected = frame == key,
                        onClick  = { frame = key },
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SectionLabel("Informations 👤")

            // Pseudo
            OutlinedTextField(
                value           = username,
                onValueChange   = { username = it },
                label           = { Text("Pseudo *") },
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                supportingText  = {
                    if (username.isBlank())
                        Text("Requis", color = MaterialTheme.colorScheme.error)
                }
            )

            // Âge
            OutlinedTextField(
                value           = age,
                onValueChange   = { if (it.length <= 3) age = it.filter { c -> c.isDigit() } },
                label           = { Text("Âge") },
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            // Bio
            OutlinedTextField(
                value         = bio,
                onValueChange = { if (it.length <= 150) bio = it },
                label         = { Text("Bio (${bio.length}/150)") },
                modifier      = Modifier.fillMaxWidth().height(90.dp),
                shape         = RoundedCornerShape(12.dp),
                maxLines      = 4
            )

            // Statut amoureux
            ExposedDropdownMenuBox(expanded = statutExpanded, onExpandedChange = { statutExpanded = !statutExpanded }) {
                OutlinedTextField(
                    value         = relStatus, onValueChange = {}, readOnly = true,
                    label         = { Text("Statut amoureux 💑") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statutExpanded) },
                    modifier      = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = statutExpanded, onDismissRequest = { statutExpanded = false }) {
                    DropdownMenuItem(text = { Text("— Aucun —") }, onClick = { relStatus = ""; statutExpanded = false })
                    relStatuts.forEach { s ->
                        DropdownMenuItem(
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s)
                                if (s == relStatus) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }},
                            onClick = { relStatus = s; statutExpanded = false }
                        )
                    }
                }
            }

            // Classe ENI
            ExposedDropdownMenuBox(expanded = eniExpanded, onExpandedChange = { eniExpanded = !eniExpanded }) {
                OutlinedTextField(
                    value          = classeENI, onValueChange = {}, readOnly = true,
                    label          = { Text("Classe ENI 🎓") },
                    trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eniExpanded) },
                    modifier       = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        if (classeENI.isNotBlank())
                            Text("✅ Badge ENI attribué !", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        else
                            Text("Sélectionne ta classe pour le badge ENI 🎓", style = MaterialTheme.typography.labelSmall)
                    }
                )
                ExposedDropdownMenu(expanded = eniExpanded, onDismissRequest = { eniExpanded = false }) {
                    DropdownMenuItem(text = { Text("— Aucune —") }, onClick = { classeENI = ""; eniExpanded = false })
                    ENI_CLASSES.forEach { cl ->
                        DropdownMenuItem(
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(cl)
                                if (cl == classeENI) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }},
                            onClick = { classeENI = cl; eniExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary
    )
}