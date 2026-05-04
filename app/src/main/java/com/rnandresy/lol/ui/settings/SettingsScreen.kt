package com.rnandresy.lol.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AskipViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val notifyMsg      by vm.notifyMessages.collectAsState()
    val notifyPost     by vm.notifyPosts.collectAsState()
    val notifyMentions by vm.notifyMentions.collectAsState()
    val totalBytes     by vm.totalBytesStored.collectAsState()
    val sessionMB       = remember { vm.dataTracker.getSessionMB() }
    val totalMB         = totalBytes / (1024f * 1024f)

    var showChangeEmail by remember { mutableStateOf(false) }
    var showChangePwd   by remember { mutableStateOf(false) }
    var feedback        by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Paramètres ⚙️", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { pad ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Feedback ──────────────────────────────────────────────────────
            feedback?.let { msg ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { feedback = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Données consommées ────────────────────────────────────────────
            SettingsSectionTitle("📊 Données consommées")
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsDataRow("Session en cours", "%.2f Mo".format(sessionMB))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsDataRow("Total accumulé", "%.2f Mo".format(totalMB))
                }
            }

            // ── Notifications ─────────────────────────────────────────────────
            SettingsSectionTitle("🔔 Notifications")

            // Info admin
            Surface(
                color  = Color(0xFFFFD700).copy(alpha = 0.08f),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👑", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Les posts, messages et mentions de l'Admin sont toujours activés et ne peuvent pas être désactivés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8860B)
                    )
                }
            }

            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    // Messages non-admin
                    SettingsSwitchRow(
                        icon     = Icons.Default.Chat,
                        title    = "Messages privés",
                        subtitle = "Alertes pour les messages des autres membres",
                        checked  = notifyMsg,
                        enabled  = true
                    ) { vm.setNotifyMessages(it) }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                    // Mentions non-admin
                    SettingsSwitchRow(
                        icon     = Icons.Default.AlternateEmail,
                        title    = "Mentions",
                        subtitle = "Alertes quand quelqu'un te mentionne (@toi)",
                        checked  = notifyMentions,
                        enabled  = true
                    ) { vm.setNotifyMentions(it) }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                    // Nouveaux posts (autres users)
                    SettingsSwitchRow(
                        icon     = Icons.Default.Feed,
                        title    = "Nouveaux posts",
                        subtitle = "Alertes quand d'autres membres postent",
                        checked  = notifyPost,
                        enabled  = true
                    ) { vm.setNotifyPosts(it) }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                    // Posts admin (toujours activé — non modifiable)
                    SettingsSwitchRow(
                        icon     = Icons.Default.Campaign,
                        title    = "Annonces de l'Admin 👑",
                        subtitle = "Toujours activé",
                        checked  = true,
                        enabled  = false
                    ) { /* non modifiable */ }
                }
            }

            // ── Compte ────────────────────────────────────────────────────────
            SettingsSectionTitle("👤 Compte")
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    SettingsActionRow(
                        icon     = Icons.Default.Email,
                        title    = "Changer l'email",
                        subtitle = vm.currentEmail
                    ) { showChangeEmail = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))
                    SettingsActionRow(
                        icon     = Icons.Default.Lock,
                        title    = "Changer le mot de passe",
                        subtitle = "••••••••"
                    ) { showChangePwd = true }
                }
            }

            // ── À propos ──────────────────────────────────────────────────────
            SettingsSectionTitle("ℹ️ À propos")
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Askip 🔥", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.3.24 — Les rumeurs du campus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Développé avec ❤️ pour l'ENI, par 3077, 3073, 3189", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Déconnexion ───────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Se déconnecter", fontWeight = FontWeight.Bold)
            }
        }
    }

    // ── Dialog changer email ──────────────────────────────────────────────────
    if (showChangeEmail) {
        var newEmail by remember { mutableStateOf("") }
        var pwd      by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangeEmail = false },
            title            = { Text("Changer l'email") },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Un email de confirmation sera envoyé à la nouvelle adresse.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it },
                        label = { Text("Nouvel email") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = pwd, onValueChange = { pwd = it },
                        label = { Text("Mot de passe actuel") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        vm.updateEmail(newEmail, pwd) { ok, err ->
                            showChangeEmail = false
                            feedback = if (ok) "✅ Email mis à jour !" else "❌ $err"
                        }
                    },
                    enabled = newEmail.isNotBlank() && pwd.isNotBlank()
                ) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { showChangeEmail = false }) { Text("Annuler") } }
        )
    }

    // ── Dialog changer mot de passe ───────────────────────────────────────────
    if (showChangePwd) {
        var current    by remember { mutableStateOf("") }
        var newPwd     by remember { mutableStateOf("") }
        var confirmPwd by remember { mutableStateOf("") }
        val pwdMatch    = newPwd == confirmPwd && newPwd.length >= 6
        AlertDialog(
            onDismissRequest = { showChangePwd = false },
            title            = { Text("Nouveau mot de passe") },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = current, onValueChange = { current = it },
                        label = { Text("Mot de passe actuel") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = newPwd, onValueChange = { newPwd = it },
                        label = { Text("Nouveau mot de passe") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("6 caractères minimum") })
                    OutlinedTextField(value = confirmPwd, onValueChange = { confirmPwd = it },
                        label = { Text("Confirmer") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        isError = confirmPwd.isNotBlank() && confirmPwd != newPwd,
                        supportingText = {
                            if (confirmPwd.isNotBlank() && confirmPwd != newPwd)
                                Text("Ne correspondent pas", color = MaterialTheme.colorScheme.error)
                        })
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        vm.updatePassword(current, newPwd) { ok, err ->
                            showChangePwd = false
                            feedback = if (ok) "✅ Mot de passe mis à jour !" else "❌ $err"
                        }
                    },
                    enabled = current.isNotBlank() && pwdMatch
                ) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { showChangePwd = false }) { Text("Annuler") } }
        )
    }
}

// ── Composants ────────────────────────────────────────────────────────────────

@Composable
fun SettingsSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
}

@Composable
fun SettingsDataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean = true,
    onChanged: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null,
            tint     = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChanged, enabled = enabled)
    }
}

@Composable
fun SettingsActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}