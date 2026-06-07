package com.rnandresy.lol.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.utils.ALL_ACHIEVEMENTS
import com.rnandresy.lol.viewmodel.AskipViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    vm: AskipViewModel,
    userId: String,
    onBack: () -> Unit
) {
    val isMe         = userId == vm.currentUserId
    val achievements = if (isMe) vm.myAchievements.collectAsState().value
    else vm.viewedAchievements.collectAsState().value

    LaunchedEffect(userId) { if (!isMe) vm.loadProfile(userId) }

    val unlockedIds = achievements.map { it.id }.toSet()
    val progress    = unlockedIds.size.toFloat() / ALL_ACHIEVEMENTS.size.coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Trophées 🏆") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad)) {
            // ── Barre de progression ──────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp)
                )
                Text(
                    "${unlockedIds.size}/${ALL_ACHIEVEMENTS.size}",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ALL_ACHIEVEMENTS) { def ->
                    val unlocked    = def.id in unlockedIds
                    val color       = runCatching {
                        Color(android.graphics.Color.parseColor(def.color))
                    }.getOrElse { Color.Gray }
                    val achievement = achievements.find { it.id == def.id }

                    Card(
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = if (unlocked)
                                color.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().then(
                            if (unlocked) Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            else Modifier
                        )
                    ) {
                        Row(
                            modifier              = Modifier.padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                if (unlocked) def.icon else "🔒",
                                fontSize = 32.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        if (unlocked) def.title else "???",
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (unlocked) color
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    // Badge de rareté
                                    val rarityColor = when (def.rarity) {
                                        "légendaire" -> Color(0xFFFFD700)
                                        "épique"     -> Color(0xFF9C27B0)
                                        "rare"       -> Color(0xFF2196F3)
                                        else         -> Color.Gray
                                    }
                                    Surface(
                                        color  = rarityColor.copy(alpha = 0.15f),
                                        shape  = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            def.rarity,
                                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            fontSize   = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = rarityColor
                                        )
                                    }
                                }
                                Text(
                                    def.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (unlocked)
                                        MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                if (unlocked && achievement != null) {
                                    Text(
                                        "Débloqué le ${
                                            SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
                                                .format(Date(achievement.unlockedAt))
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (unlocked) {
                                Icon(
                                    Icons.Default.Star, null,
                                    tint     = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}