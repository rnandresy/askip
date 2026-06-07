package com.rnandresy.lol.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AskipViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val notifyMsg   by vm.notifyMessages.collectAsState()
    val notifyPost  by vm.notifyPosts.collectAsState()
    val notifyMents by vm.notifyMentions.collectAsState()
    val totalBytes  by vm.totalBytesStored.collectAsState()
    val theme       by vm.appTheme.collectAsState()
    val loading     by vm.loading.collectAsState()

    val sessionMB = remember { vm.dataTracker.getSessionMB() }
    val totalMB   = totalBytes / (1024f * 1024f)

    var showEmail       by remember { mutableStateOf(false) }
    var showPwd         by remember { mutableStateOf(false) }
    var showDeleteAcct  by remember { mutableStateOf(false) }
    var msg             by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors         = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Feedback
            msg?.let { (isErr, text) ->
                Surface(
                    color  = if (isErr) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = if (isErr) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { msg = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            // ── Données ───────────────────────────────────────────────────────
            SectionLabel("Données consommées")
            SettingsCard {
                DataRow("Session en cours", "%.2f Mo".format(sessionMB))
                SettingsDivider()
                DataRow("Total accumulé", "%.2f Mo".format(totalMB))
            }

            // ── Thème ─────────────────────────────────────────────────────────
            SectionLabel("Apparence")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Thème", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTheme.values().forEach { t ->
                            FilterChip(
                                selected = theme == t,
                                onClick  = { vm.setTheme(t) },
                                label    = {
                                    Text(
                                        "${t.emoji} ${t.displayName}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── Notifications ─────────────────────────────────────────────────
            SectionLabel("Notifications")
            SettingsCard {
                SwitchRow(Icons.Default.Chat,           "Messages",       "Alertes nouveaux messages",           notifyMsg)   { vm.setNotifyMessages(it) }
                SettingsDivider()
                SwitchRow(Icons.Default.AlternateEmail, "Mentions",       "Alertes quand tu es mentionné(e)",    notifyMents) { vm.setNotifyMentions(it) }
                SettingsDivider()
                SwitchRow(Icons.Default.Feed,           "Nouveaux posts", "Alertes publications des membres",    notifyPost)  { vm.setNotifyPosts(it) }
                SettingsDivider()
                SwitchRow(Icons.Default.Campaign,       "Admin 👑",       "Toujours activé — obligatoire",       true, false) { }
            }

            // ── Compte ────────────────────────────────────────────────────────
            SectionLabel("Compte")
            SettingsCard {
                ActionRow(Icons.Default.Email, "Changer l'email",         vm.currentEmail) { showEmail = true }
                SettingsDivider()
                ActionRow(Icons.Default.Lock,  "Changer le mot de passe", "••••••••")      { showPwd = true }
            }

            // ── App ───────────────────────────────────────────────────────────
            SectionLabel("À propos")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Askip 🔥", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Version 1.0 — Réseau ENI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Déconnexion
            Button(
                onClick  = onLogout, enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Se déconnecter", fontWeight = FontWeight.SemiBold)
            }

            // Supprimer le compte
            OutlinedButton(
                onClick  = { showDeleteAcct = true }, enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.5f))
            ) {
                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Supprimer mon compte", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    // Dialog email
    if (showEmail) {
        var e by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEmail = false },
            title            = { Text("Changer l'email", fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = e, onValueChange = { e = it }, label = { Text("Nouvel email") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Mot de passe actuel") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updateEmail(e, p) { ok, err ->
                        showEmail = false
                        msg = ok to if (ok) "✅ Email mis à jour !" else "❌ $err"
                    }
                }, enabled = e.isNotBlank() && p.isNotBlank(), shape = RoundedCornerShape(10.dp)) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { showEmail = false }) { Text("Annuler") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog mot de passe
    if (showPwd) {
        var cur by remember { mutableStateOf("") }
        var nw  by remember { mutableStateOf("") }
        var cf  by remember { mutableStateOf("") }
        val ok  = nw == cf && nw.length >= 6
        AlertDialog(
            onDismissRequest = { showPwd = false },
            title            = { Text("Nouveau mot de passe", fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = cur, onValueChange = { cur = it }, label = { Text("Actuel") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = nw, onValueChange = { nw = it }, label = { Text("Nouveau (6 min)") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = cf, onValueChange = { cf = it }, label = { Text("Confirmer") },
                        isError = cf.isNotBlank() && cf != nw, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updatePassword(cur, nw) { isOk, err ->
                        showPwd = false
                        msg = isOk to if (isOk) "✅ Mot de passe mis à jour !" else "❌ $err"
                    }
                }, enabled = cur.isNotBlank() && ok, shape = RoundedCornerShape(10.dp)) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { showPwd = false }) { Text("Annuler") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog suppression de compte
    if (showDeleteAcct) {
        var pwd     by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val WORD     = "SUPPRIMER"
        AlertDialog(
            onDismissRequest = { if (!loading) showDeleteAcct = false },
            title            = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Text("Supprimer mon compte", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "⚠️ Action irréversible. Tous tes posts, commentaires, stories et données seront supprimés définitivement.",
                            modifier = Modifier.padding(12.dp),
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    OutlinedTextField(
                        value = confirm, onValueChange = { confirm = it.uppercase() },
                        label = { Text("Tape « $WORD » pour confirmer") },
                        isError = confirm.isNotBlank() && confirm != WORD,
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = pwd, onValueChange = { pwd = it },
                        label = { Text("Mot de passe actuel") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        vm.deleteAccount(pwd,
                            onSuccess = { showDeleteAcct = false; onLogout() },
                            onError   = { err -> showDeleteAcct = false; msg = true to "❌ $err" }
                        )
                    },
                    enabled  = confirm == WORD && pwd.isNotBlank() && !loading,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAcct = false }, enabled = !loading) { Text("Annuler") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Composants locaux ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier   = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        color     = MaterialTheme.colorScheme.surface,
        border    = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.4f)),
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color     = MaterialTheme.colorScheme.outline.copy(0.15f),
        modifier  = Modifier.padding(start = 52.dp),
        thickness = 0.5.dp
    )
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(if (enabled) 1f else 0.4f),
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}