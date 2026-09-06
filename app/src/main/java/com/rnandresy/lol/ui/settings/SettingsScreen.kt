package com.rnandresy.lol.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.ui.theme.swatchFor
import com.rnandresy.lol.viewmodel.AskipViewModel

/**
 * Les réglages.
 *
 * Même contenu qu'avant — données, thème, confort, notifications, compte —
 * mais rangé en cartes-bulles, avec un sélecteur de thème qui montre
 * réellement les couleurs plutôt qu'un nom. Les deux actions dangereuses
 * restent en bas, séparées du reste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AskipViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val notifyMsg by vm.notifyMessages.collectAsState()
    val notifyPost by vm.notifyPosts.collectAsState()
    val notifyMents by vm.notifyMentions.collectAsState()
    val totalBytes by vm.totalBytesStored.collectAsState()
    val theme by vm.appTheme.collectAsState()
    val loading by vm.loading.collectAsState()
    val haptics by vm.haptics.collectAsState()
    val reduceMotion by vm.reduceMotion.collectAsState()

    val sessionMB = remember { vm.dataTracker.getSessionMB() }
    val totalMB = totalBytes / (1024f * 1024f)

    var showEmail by remember { mutableStateOf(false) }
    var showPwd by remember { mutableStateOf(false) }
    var showDeleteAcct by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text("Réglages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(Modifier.padding(start = Space.md)) {
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onBack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            msg?.let { (isErr, text) -> Banner(text, isErr) { msg = null } }

            SectionLabel("Apparence")
            SettingsCard {
                ThemePicker(selected = theme, onSelect = vm::setTheme)
            }

            SectionLabel("Confort")
            SettingsCard {
                SwitchRow(
                    Icons.Rounded.Vibration, "Vibrations",
                    "Retour tactile sur les réactions et les votes", haptics
                ) { vm.setHaptics(it) }
                SettingsDivider()
                SwitchRow(
                    Icons.Rounded.Animation, "Réduire les animations",
                    "Pour un appareil lent ou une sensibilité au mouvement", reduceMotion
                ) { vm.setReduceMotion(it) }
            }

            SectionLabel("Notifications")
            SettingsCard {
                SwitchRow(
                    Icons.Rounded.Forum, "Messages",
                    "Alertes nouveaux messages", notifyMsg
                ) { vm.setNotifyMessages(it) }
                SettingsDivider()
                SwitchRow(
                    Icons.Rounded.AlternateEmail, "Mentions",
                    "Alertes quand tu es mentionné(e)", notifyMents
                ) { vm.setNotifyMentions(it) }
                SettingsDivider()
                SwitchRow(
                    Icons.Rounded.Newspaper, "Nouveaux posts",
                    "Alertes publications des membres", notifyPost
                ) { vm.setNotifyPosts(it) }
                SettingsDivider()
                // Verrouillé : les annonces admin passent toujours.
                SwitchRow(
                    Icons.Rounded.Campaign, "Annonces admin 👑",
                    "Toujours activé — obligatoire", true, enabled = false
                ) { }
            }

            SectionLabel("Compte")
            SettingsCard {
                ActionRow(Icons.Rounded.Email, "Changer l'email", vm.currentEmail) {
                    showEmail = true
                }
                SettingsDivider()
                ActionRow(Icons.Rounded.Lock, "Changer le mot de passe", "••••••••") {
                    showPwd = true
                }
            }

            SectionLabel("Données consommées")
            SettingsCard {
                DataRow("Session en cours", "%.2f Mo".format(sessionMB))
                SettingsDivider()
                DataRow("Total accumulé", "%.2f Mo".format(totalMB))
            }

            SectionLabel("À propos")
            SettingsCard {
                Column(
                    modifier = Modifier.padding(Space.lg),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "Askip 🌸",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version 1.0 — Réseau ENI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Space.md))

            BubbleButton(
                text = "Se déconnecter",
                emoji = "👋",
                onClick = onLogout,
                enabled = !loading,
                tone = BubbleTone.SOFT,
                fillWidth = true
            )

            BubbleButton(
                text = "Supprimer mon compte",
                emoji = "🗑️",
                onClick = { showDeleteAcct = true },
                enabled = !loading,
                tone = BubbleTone.DANGER,
                fillWidth = true
            )

            Spacer(Modifier.height(Space.xxl))
        }
    }

    if (showEmail) {
        EmailDialog(
            onDismiss = { showEmail = false },
            onConfirm = { email, pwd ->
                vm.updateEmail(email, pwd) { ok, err ->
                    showEmail = false
                    msg = ok to if (ok) "✅ Email mis à jour." else "❌ $err"
                }
            }
        )
    }

    if (showPwd) {
        PasswordDialog(
            onDismiss = { showPwd = false },
            onConfirm = { current, next ->
                vm.updatePassword(current, next) { ok, err ->
                    showPwd = false
                    msg = ok to if (ok) "✅ Mot de passe mis à jour." else "❌ $err"
                }
            }
        )
    }

    if (showDeleteAcct) {
        DeleteAccountDialog(
            loading = loading,
            onDismiss = { showDeleteAcct = false },
            onConfirm = { pwd ->
                vm.deleteAccount(
                    pwd,
                    onSuccess = { showDeleteAcct = false; onLogout() },
                    onError = { err -> showDeleteAcct = false; msg = true to "❌ $err" }
                )
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Sélecteur de thème
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Les thèmes, montrés plutôt que nommés.
 *
 * Chaque pastille porte les vraies couleurs du thème : fond, carte, accent.
 * On voit d'un coup d'œil lequel est clair, lequel est sombre — impossible à
 * deviner depuis « Nostalgique 🕯 ».
 */
@Composable
private fun ThemePicker(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    val palette = LocalAskipPalette.current
    val systemDark = isSystemInDarkTheme()
    val tap = rememberTapFeedback()

    Column(
        modifier = Modifier.padding(vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Text(
            "Thème",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = Space.lg)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            contentPadding = PaddingValues(horizontal = Space.lg)
        ) {
            items(AppTheme.entries.toList()) { t ->
                val swatch = swatchFor(t, systemDark)
                val picked = selected == t

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    TapArea(
                        onTap = { tap(); onSelect(t) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(swatch.background)
                            .border(
                                width = if (picked) 3.dp else 1.dp,
                                color = if (picked) MaterialTheme.colorScheme.primary
                                else palette.bubbleBorder,
                                shape = CircleShape
                            )
                    ) {
                        // Une carte et un accent, dans le fond du thème : la
                        // pastille est un mini-écran, pas une tache de couleur.
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                                .clip(RoundedCornerShape(Radius.xs))
                                .background(swatch.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(swatch.accent)
                            )
                        }
                        if (picked) {
                            Text(
                                "✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = swatch.accent,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                            )
                        }
                    }

                    Text(
                        "${t.emoji} ${t.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (picked) FontWeight.Bold else FontWeight.Normal,
                        color = if (picked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Dialogues
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmailDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }

    AskipDialog(
        title = "Changer l'email",
        onDismiss = onDismiss,
        confirmLabel = "Confirmer",
        confirmEnabled = email.isNotBlank() && pwd.isNotBlank(),
        onConfirm = { onConfirm(email, pwd) }
    ) {
        DialogField(email, { email = it }, "Nouvel email")
        DialogField(pwd, { pwd = it }, "Mot de passe actuel", secret = true)
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = next == confirm && next.length >= 6

    AskipDialog(
        title = "Nouveau mot de passe",
        onDismiss = onDismiss,
        confirmLabel = "Confirmer",
        confirmEnabled = current.isNotBlank() && valid,
        onConfirm = { onConfirm(current, next) }
    ) {
        DialogField(current, { current = it }, "Actuel", secret = true)
        DialogField(next, { next = it }, "Nouveau (6 caractères min.)", secret = true)
        DialogField(
            confirm, { confirm = it }, "Confirmer", secret = true,
            error = confirm.isNotBlank() && confirm != next
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    var typed by remember { mutableStateOf("") }
    val word = "SUPPRIMER"

    AskipDialog(
        title = "Supprimer mon compte",
        emoji = "🗑️",
        danger = true,
        onDismiss = { if (!loading) onDismiss() },
        confirmLabel = "Supprimer",
        confirmEnabled = typed == word && pwd.isNotBlank() && !loading,
        confirmLoading = loading,
        onConfirm = { onConfirm(pwd) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.sm))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(Space.md)
        ) {
            Text(
                "⚠️ Action irréversible. Tes posts, commentaires, stories et " +
                    "données seront supprimés définitivement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        DialogField(
            typed, { typed = it.uppercase() }, "Tape « $word » pour confirmer",
            error = typed.isNotBlank() && typed != word
        )
        DialogField(pwd, { pwd = it }, "Mot de passe actuel", secret = true)
    }
}

/** Le dialogue commun : même forme, mêmes bulles, partout. */
@Composable
private fun AskipDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    emoji: String? = null,
    danger: Boolean = false,
    confirmLoading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.lg),
        containerColor = LocalAskipPalette.current.bubble,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                if (emoji != null) Text(emoji, fontSize = 17.sp)
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = if (danger) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Space.md),
                content = content
            )
        },
        confirmButton = {
            BubbleButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = confirmLoading,
                tone = if (danger) BubbleTone.DANGER else BubbleTone.PRIMARY,
                size = BubbleSize.SMALL
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !confirmLoading) { Text("Annuler") }
        }
    )
}

/** Le champ des dialogues : même forme que ceux du profil. */
@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    secret: Boolean = false,
    error: Boolean = false
) {
    val palette = LocalAskipPalette.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error,
        visualTransformation = if (secret) PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.sm),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = palette.bubbleBorder,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  Briques
// ═════════════════════════════════════════════════════════════════════════════

/** Le bandeau de retour, en tête d'écran, refermable. */
@Composable
private fun Banner(text: String, isError: Boolean, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(
                if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        BubbleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Fermer",
            onClick = onClose,
            diameter = 26.dp
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Space.xs, top = Space.md, bottom = Space.xxs)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    BubbleCard(modifier = Modifier.fillMaxWidth(), gloss = 0.3f) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = LocalAskipPalette.current.bubbleBorder,
        modifier = Modifier.padding(start = 54.dp),
        thickness = 0.5.dp
    )
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** L'icône ronde qui ouvre chaque ligne de réglage. */
@Composable
private fun RowIcon(icon: ImageVector, enabled: Boolean) {
    val palette = LocalAskipPalette.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = palette.scoop,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon, enabled)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                // Un interrupteur verrouillé mais actif doit rester lisible :
                // par défaut il vire au gris et se confond avec « éteint ».
                disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledCheckedThumbColor = Color.White
            )
        )
    }
}

/**
 * Une ligne d'action.
 *
 * Toute la ligne réagit maintenant, pas seulement le chevron : viser une
 * cible de 32 dp au bout d'une ligne pleine largeur n'avait aucune raison
 * d'être.
 */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val tap = rememberTapFeedback()

    TapArea(
        onTap = { tap(); onClick() },
        scaleDown = 0.99f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowIcon(icon, enabled = true)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "›",
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
