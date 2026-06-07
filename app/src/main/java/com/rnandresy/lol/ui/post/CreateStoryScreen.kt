package com.rnandresy.lol.ui.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.utils.STORY_COLORS
import com.rnandresy.lol.utils.STORY_EMOJIS
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(vm: AskipViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    var content  by remember { mutableStateOf("") }
    var selColor by remember { mutableStateOf(STORY_COLORS.first()) }
    var selEmoji by remember { mutableStateOf(STORY_EMOJIS.first()) }

    val bgColor = runCatching {
        Color(android.graphics.Color.parseColor(selColor))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Nouvelle story", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions        = {
                    FilledIconButton(
                        onClick  = { vm.createStory(content.trim(), selEmoji, selColor); onDone() },
                        enabled  = content.isNotBlank(),
                        shape    = RoundedCornerShape(10.dp)
                    ) { Icon(Icons.Default.Send, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aperçu
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(20.dp)
                ) {
                    Text(selEmoji, fontSize = 50.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        content.ifBlank { "Écris quelque chose…" },
                        color     = Color.White.copy(alpha = if (content.isBlank()) 0.5f else 1f),
                        style     = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Texte
            OutlinedTextField(
                value         = content,
                onValueChange = { if (it.length <= 200) content = it },
                placeholder   = { Text("Ce que tu veux dire…") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                maxLines      = 4,
                supportingText = { Text("${content.length}/200", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )

            // Couleurs
            Text("Couleur", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(STORY_COLORS) { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
                    Box(
                        modifier         = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (selColor == hex) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier)
                            .clickable { selColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selColor == hex) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Emojis
            Text("Emoji", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(STORY_EMOJIS) { emoji ->
                    Surface(
                        onClick = { selEmoji = emoji },
                        color   = if (selEmoji == emoji) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        shape   = RoundedCornerShape(10.dp),
                        border  = if (selEmoji == emoji) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                    ) {
                        Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}