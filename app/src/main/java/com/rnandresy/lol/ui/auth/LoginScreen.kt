package com.rnandresy.lol.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipButton
import com.rnandresy.lol.ui.components.AskipTextField
import com.rnandresy.lol.viewmodel.AskipViewModel

@Composable
fun LoginScreen(
    vm: AskipViewModel,
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val loading    by vm.loading.collectAsState()
    val error      by vm.error.collectAsState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onSuccess() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

            // ── Logo ──────────────────────────────────────────────────────────
            Text("🔥", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "ASKIP",
                style       = MaterialTheme.typography.displaySmall,
                fontWeight  = FontWeight.Black,
                color       = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 6.sp
            )
            Text(
                "Le réseau du campus ENI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // ── Formulaire ────────────────────────────────────────────────────
            Surface(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                color     = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border    = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Connexion",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    AskipTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = "Email",
                        leadingIcon   = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        )
                    )

                    AskipTextField(
                        value             = password,
                        onValueChange     = { password = it },
                        label             = "Mot de passe",
                        isPassword        = true,
                        showPassword      = showPwd,
                        onTogglePassword  = { showPwd = !showPwd },
                        leadingIcon       = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions   = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        )
                    )

                    // ── Erreur ────────────────────────────────────────────────
                    AnimatedVisibility(visible = error != null) {
                        Surface(
                            color  = MaterialTheme.colorScheme.errorContainer,
                            shape  = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠", fontSize = 14.sp)
                                Text(
                                    error ?: "",
                                    style  = MaterialTheme.typography.bodySmall,
                                    color  = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick         = { vm.clearError() },
                                    contentPadding  = PaddingValues(0.dp),
                                    modifier        = Modifier.size(28.dp)
                                ) {
                                    Text("✕", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    AskipButton(
                        text      = "Entrer",
                        onClick   = { vm.login(email, password) },
                        modifier  = Modifier.fillMaxWidth(),
                        enabled   = email.isNotBlank() && password.isNotBlank(),
                        isLoading = loading
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Inscription ───────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pas de compte ?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onGoRegister) {
                    Text(
                        "S'inscrire",
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Écran inscription ─────────────────────────────────────────────────────────
@Composable
fun RegisterScreen(
    vm: AskipViewModel,
    onSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val loading    by vm.loading.collectAsState()
    val error      by vm.error.collectAsState()

    var username by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onSuccess() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

            Text("🎓", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Rejoindre Askip",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Le réseau des étudiants de l'ENI",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            Surface(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                color     = MaterialTheme.colorScheme.surface,
                border    = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Inscription", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    AskipTextField(
                        value         = username,
                        onValueChange = { username = it },
                        label         = "Pseudo",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    AskipTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = "Email",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    AskipTextField(
                        value            = password,
                        onValueChange    = { password = it },
                        label            = "Mot de passe (6 min)",
                        isPassword       = true,
                        showPassword     = showPwd,
                        onTogglePassword = { showPwd = !showPwd },
                        keyboardOptions  = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )

                    AnimatedVisibility(visible = error != null) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                error ?: "",
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    AskipButton(
                        text      = "Créer mon compte",
                        onClick   = { vm.register(email, password, username) },
                        modifier  = Modifier.fillMaxWidth(),
                        enabled   = username.isNotBlank() && email.isNotBlank() && password.length >= 6,
                        isLoading = loading
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Déjà inscrit ?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onGoLogin) {
                    Text("Se connecter", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}