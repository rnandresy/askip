package com.rnandresy.lol.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.theme.AskipPurple
import com.rnandresy.lol.ui.theme.DarkBg
import com.rnandresy.lol.ui.theme.DarkSurf
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

    var email   by remember { mutableStateOf("") }
    var pwd     by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onSuccess() }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg, Color(0xFF1A0040)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔥", fontSize = 64.sp)
            Spacer(Modifier.height(4.dp))
            Text("askip", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Les rumeurs du campus 👀", style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(32.dp))

            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = DarkSurf),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Connexion", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(Modifier.height(16.dp))

                    AskipField(email, { email = it }, "Email", Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    Spacer(Modifier.height(10.dp))
                    AskipField(pwd, { pwd = it }, "Mot de passe", Icons.Default.Lock,
                        isPassword = true, showPwd = showPwd,
                        onToggle   = { showPwd = !showPwd },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))

                    error?.let { e ->
                        Spacer(Modifier.height(8.dp))
                        Text(e, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        LaunchedEffect(e) { vm.clearError() }
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick  = { vm.login(email, pwd) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        enabled  = !loading && email.isNotBlank() && pwd.isNotBlank()
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Entrer dans le campus 🚀", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
                        Text("Pas de compte ? Rejoindre ->", color = AskipPurple)
                    }
                }
            }
        }
    }
}

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
    var pwd      by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onSuccess() }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg, Color(0xFF1A0040)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎓", fontSize = 56.sp)
            Spacer(Modifier.height(4.dp))
            Text("Rejoindre Askip", style = MaterialTheme.typography.titleLarge,
                color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Le réseau des ragoteurs de l'ENI 😈", style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(24.dp))

            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = DarkSurf),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Inscription", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(Modifier.height(16.dp))

                    AskipField(username, { username = it }, "Pseudo", Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Spacer(Modifier.height(10.dp))
                    AskipField(email, { email = it }, "Email", Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    Spacer(Modifier.height(10.dp))
                    AskipField(pwd, { pwd = it }, "Mot de passe (6+)", Icons.Default.Lock,
                        isPassword = true, showPwd = showPwd,
                        onToggle   = { showPwd = !showPwd },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))

                    error?.let { e ->
                        Spacer(Modifier.height(8.dp))
                        Text(e, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick  = { vm.register(email, pwd, username) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        enabled  = !loading && username.isNotBlank() && email.isNotBlank() && pwd.length >= 6
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Rejoindre le campus ! 🎉", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("Déjà inscrit ? Se connecter", color = AskipPurple)
                    }
                }
            }
        }
    }
}

@Composable
fun AskipField(
    value: String, onValueChange: (String) -> Unit,
    label: String, icon: ImageVector,
    isPassword: Boolean = false, showPwd: Boolean = false,
    onToggle: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label) },
        leadingIcon         = { Icon(icon, null, tint = Color.White.copy(alpha = 0.6f)) },
        trailingIcon        = if (isPassword && onToggle != null) {
            { IconButton(onClick = onToggle) {
                Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = Color.White.copy(alpha = 0.6f))
            }}
        } else null,
        visualTransformation = if (isPassword && !showPwd) PasswordVisualTransformation()
        else VisualTransformation.None,
        keyboardOptions     = keyboardOptions,
        singleLine          = true,
        modifier            = Modifier.fillMaxWidth(),
        shape               = RoundedCornerShape(14.dp),
        colors              = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            cursorColor             = AskipPurple,
            focusedBorderColor      = AskipPurple,
            unfocusedBorderColor    = Color.White.copy(alpha = 0.25f),
            focusedLabelColor       = AskipPurple,
            unfocusedLabelColor     = Color.White.copy(alpha = 0.5f),
            focusedContainerColor   = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
        )
    )
}