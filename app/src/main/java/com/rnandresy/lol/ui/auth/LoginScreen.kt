@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rnandresy.lol.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.SakuraFall
import com.rnandresy.lol.ui.components.StarDust
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.softGlow
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Motion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.viewmodel.AskipViewModel

// ═════════════════════════════════════════════════════════════════════════════
//  Décor commun aux deux écrans
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Le fond des écrans d'entrée : dégradé crème, poussière d'étoiles, pétales.
 *
 * C'est le premier écran que voit quelqu'un : il donne le ton de toute l'app.
 * Le décor est purement visuel et n'intercepte aucun geste — le formulaire
 * reste par-dessus.
 */
@Composable
private fun AuthBackdrop(content: @Composable () -> Unit) {
    val palette = LocalAskipPalette.current
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.background,
                        scheme.surfaceVariant.copy(alpha = 0.55f),
                        scheme.background
                    )
                )
            )
    ) {
        StarDust(count = 26, seed = 11)
        SakuraFall(count = 14, seed = 5, alpha = 0.45f)
        content()
    }
}

/** Le logo : un emoji posé sur un halo, dans une bulle ronde. */
@Composable
private fun AuthLogo(subtitle: String) {
    val palette = LocalAskipPalette.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Le halo déborde volontairement de la bulle : c'est lui qui donne
            // l'impression de lumière plutôt qu'un simple cercle coloré.
            Box(
                Modifier
                    .size(150.dp)
                    .background(softGlow(palette.petal, 0.5f), CircleShape)
            )
            Box(
                Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AskipGlyph(kind = GlyphKind.FLOWER, size = 42.dp)
            }
        }

        Spacer(Modifier.height(Space.lg))
        Text(
            "ASKIP",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 8.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Champ de saisie arrondi, assorti aux bulles. */
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp)) },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                TapArea(onTap = onTogglePassword, scaleDown = 0.85f) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.VisibilityOff
                        else Icons.Rounded.Visibility,
                        if (passwordVisible) "Masquer le mot de passe"
                        else "Afficher le mot de passe",
                        Modifier.padding(Space.md).size(20.dp)
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(Radius.md),
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surface,
            unfocusedContainerColor = scheme.surface,
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = scheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Message d'erreur, en bulle rouge discrète. */
@Composable
private fun AuthError(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(Motion.spring()) + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        BubbleCard(
            fill = MaterialTheme.colorScheme.errorContainer,
            elevation = 2.dp,
            gloss = 0.3f,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(Space.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                AskipGlyph(
                    kind = GlyphKind.ALERT,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Connexion
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    vm: AskipViewModel,
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var resetNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onSuccess() }

    val canSubmit = email.isNotBlank() && password.isNotBlank()

    AuthBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(Space.huge))
            AuthLogo("Le réseau du campus ENI")
            Spacer(Modifier.height(Space.xxl))

            BubbleCard(modifier = Modifier.fillMaxWidth(), elevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    Text(
                        "Content de te revoir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    AuthField(
                        value = email,
                        onValueChange = { email = it; vm.clearError() },
                        label = "Email",
                        icon = Icons.Rounded.AlternateEmail,
                        keyboardType = KeyboardType.Email
                    )
                    AuthField(
                        value = password,
                        onValueChange = { password = it; vm.clearError() },
                        label = "Mot de passe",
                        icon = Icons.Rounded.Lock,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = showPwd,
                        onTogglePassword = { showPwd = !showPwd }
                    )

                    AuthError(error) { vm.clearError() }
                    AuthError(resetNote) { resetNote = null }

                    Spacer(Modifier.height(Space.xs))

                    BubbleButton(
                        text = "Se connecter",
                        glyph = GlyphKind.FLOWER,
                        onClick = { vm.login(email, password) },
                        enabled = canSubmit,
                        loading = loading,
                        size = BubbleSize.LARGE,
                        fillWidth = true
                    )

                    // Discret : on ne le cherche que si on en a besoin.
                    TapArea(
                        onTap = {
                            if (email.isBlank()) {
                                resetNote = "Entre d'abord ton email."
                            } else {
                                vm.resetPassword(email) { _, msg -> resetNote = msg }
                            }
                        },
                        scaleDown = 0.98f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Mot de passe oublié ?",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = Space.sm),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.xl))

            BubbleButton(
                text = "Créer un compte",
                onClick = onGoRegister,
                tone = BubbleTone.GHOST,
                fillWidth = true
            )

            Spacer(Modifier.height(Space.huge))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Inscription
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun RegisterScreen(
    vm: AskipViewModel,
    onSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onSuccess() }

    val canSubmit = username.length >= 3 && email.isNotBlank() && password.length >= 6

    AuthBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(Space.huge))
            AuthLogo("Choisis ton pseudo. On ne le change pas tous les jours.")
            Spacer(Modifier.height(Space.xxl))

            BubbleCard(modifier = Modifier.fillMaxWidth(), elevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    Text(
                        "Rejoindre le campus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    AuthField(
                        value = username,
                        onValueChange = { username = it.trim(); vm.clearError() },
                        label = "Pseudo",
                        icon = Icons.Rounded.Person
                    )
                    AuthField(
                        value = email,
                        onValueChange = { email = it; vm.clearError() },
                        label = "Email",
                        icon = Icons.Rounded.AlternateEmail,
                        keyboardType = KeyboardType.Email
                    )
                    AuthField(
                        value = password,
                        onValueChange = { password = it; vm.clearError() },
                        label = "Mot de passe",
                        icon = Icons.Rounded.Lock,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = showPwd,
                        onTogglePassword = { showPwd = !showPwd }
                    )

                    // La règle est annoncée avant l'erreur, pas après : on
                    // n'apprend pas une contrainte en se la prenant en pleine
                    // figure au moment de valider.
                    Text(
                        "3 caractères minimum pour le pseudo, 6 pour le mot de passe.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AuthError(error) { vm.clearError() }

                    Spacer(Modifier.height(Space.xs))

                    BubbleButton(
                        text = "Créer mon compte",
                        glyph = GlyphKind.SPARKLE,
                        onClick = { vm.register(email, password, username) },
                        enabled = canSubmit,
                        loading = loading,
                        size = BubbleSize.LARGE,
                        fillWidth = true
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))

            BubbleButton(
                text = "J'ai déjà un compte",
                onClick = onGoLogin,
                tone = BubbleTone.GHOST,
                fillWidth = true
            )

            Spacer(Modifier.height(Space.huge))
        }
    }
}
