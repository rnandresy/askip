package com.rnandresy.lol.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.utils.ENI_CLASSES
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: AskipViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val profile by viewModel.myProfile.collectAsState()

    var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var age by remember(profile) { mutableStateOf(profile?.age?.let { if (it > 0) it.toString() else "" } ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var relationshipStatus by remember(profile) { mutableStateOf(profile?.relationshipStatus ?: "") }
    var classeENI by remember(profile) { mutableStateOf(profile?.classeENI ?: "") }

    // Dropdown ENI
    var eniExpanded by remember { mutableStateOf(false) }

    // Dropdown statut amoureux
    val lovStatuts = listOf("Célibataire", "En couple", "Fiancé(e)", "Marié(e)", "C'est compliqué", "Préfère ne pas dire")
    var statutExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil ✏️") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    FilledIconButton(
                        onClick = {
                            val data = mutableMapOf<String, Any>(
                                "username" to username.trim(),
                                "bio" to bio.trim(),
                                "relationshipStatus" to relationshipStatus,
                                "classeENI" to classeENI,
                                // Badge ENI automatique si classe ENI sélectionnée
                                "hasBadgeENI" to classeENI.isNotBlank()
                            )
                            age.toIntOrNull()?.let { data["age"] = it }
                            viewModel.updateProfile(data, onSaved)
                        },
                        enabled = username.isNotBlank()
                    ) { Icon(Icons.Default.Save, null) }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Info photo
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("📸")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Photo de profil — bientôt disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pseudo
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Pseudo *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                supportingText = { if (username.isBlank()) Text("Champ requis") }
            )

            // Âge
            OutlinedTextField(
                value = age,
                onValueChange = { if (it.length <= 3) age = it.filter { c -> c.isDigit() } },
                label = { Text("Âge") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            // Bio
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 150) bio = it },
                label = { Text("Bio (${bio.length}/150)") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(14.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            // Statut amoureux — Dropdown
            ExposedDropdownMenuBox(
                expanded = statutExpanded,
                onExpandedChange = { statutExpanded = !statutExpanded }
            ) {
                OutlinedTextField(
                    value = relationshipStatus,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Statut amoureux") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = statutExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                ExposedDropdownMenu(
                    expanded = statutExpanded,
                    onDismissRequest = { statutExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("— Aucun —") },
                        onClick = { relationshipStatus = ""; statutExpanded = false }
                    )
                    lovStatuts.forEach { statut ->
                        DropdownMenuItem(
                            text = { Text(statut) },
                            onClick = { relationshipStatus = statut; statutExpanded = false }
                        )
                    }
                }
            }

            // Classe ENI — Dropdown avec badge auto
            ExposedDropdownMenuBox(
                expanded = eniExpanded,
                onExpandedChange = { eniExpanded = !eniExpanded }
            ) {
                OutlinedTextField(
                    value = classeENI,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Classe ENI 🎓") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = eniExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    supportingText = {
                        if (classeENI.isNotBlank()) {
                            Text("✅ Badge ENI attribué automatiquement", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("Sélectionne ta classe pour obtenir le badge ENI", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = eniExpanded,
                    onDismissRequest = { eniExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("— Aucune classe —") },
                        onClick = { classeENI = ""; eniExpanded = false }
                    )
                    ENI_CLASSES.forEach { classe ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(classe)
                                    if (classe == classeENI) {
                                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = { classeENI = classe; eniExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}