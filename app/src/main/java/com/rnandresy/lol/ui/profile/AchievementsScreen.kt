package com.rnandresy.lol.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.ProgressTrack
import com.rnandresy.lol.ui.components.SlidingSegmented
import com.rnandresy.lol.ui.components.glyphForAchievement
import com.rnandresy.lol.ui.components.readableOn
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Motion
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.ALL_ACHIEVEMENTS
import com.rnandresy.lol.utils.AchievementDef
import com.rnandresy.lol.viewmodel.AskipViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ce qu'on regarde : tout, ce qui est gagné, ou ce qui reste à faire. */
private enum class TrophyFilter(val label: String) {
    ALL("Tous"),
    DONE("Obtenus"),
    TODO("À faire")
}

/** La couleur d'une rareté. Sert au liseré comme à la pastille. */
private fun rarityColor(rarity: String): Color = when (rarity) {
    "légendaire" -> Color(0xFFD4A017)
    "épique" -> Color(0xFF9C27B0)
    "rare" -> Color(0xFF2196F3)
    else -> Color(0xFF78909C)
}

/**
 * Les trophées.
 *
 * La liste faisait défiler quarante cartes identiques. Elle garde tout, mais
 * la progression tient maintenant dans une carte en tête, et un sélecteur
 * permet de ne voir que ce qui reste à décrocher — c'est ce qu'on vient
 * chercher.
 */
/**
 * La date de déblocage, construite une fois par thread plutôt qu'une fois par
 * trophée affiché.
 */
private val DEBLOCAGE: ThreadLocal<SimpleDateFormat> =
    ThreadLocal.withInitial { SimpleDateFormat("d MMMM yyyy", Locale.FRENCH) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    vm: AskipViewModel,
    userId: String,
    onBack: () -> Unit
) {
    val isMe = userId == vm.currentUserId
    val achievements = if (isMe) vm.myAchievements.collectAsState().value
    else vm.viewedAchievements.collectAsState().value

    LaunchedEffect(userId) { if (!isMe) vm.loadProfile(userId) }

    var filter by remember { mutableStateOf(TrophyFilter.ALL) }

    val unlockedIds = achievements.map { it.id }.toSet()
    val total = ALL_ACHIEVEMENTS.size.coerceAtLeast(1)
    val progress = unlockedIds.size.toFloat() / total

    val shown = when (filter) {
        TrophyFilter.ALL -> ALL_ACHIEVEMENTS
        TrophyFilter.DONE -> ALL_ACHIEVEMENTS.filter { it.id in unlockedIds }
        TrophyFilter.TODO -> ALL_ACHIEVEMENTS.filterNot { it.id in unlockedIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = {
                    Text(
                        if (isMe) "Mes trophées" else "Trophées",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        LazyColumn(
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(
                start = Space.lg,
                end = Space.lg,
                bottom = Space.huge
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item(key = "progress") {
                ProgressCard(
                    unlocked = unlockedIds.size,
                    total = ALL_ACHIEVEMENTS.size,
                    progress = progress,
                    byRarity = ALL_ACHIEVEMENTS
                        .filter { it.id in unlockedIds }
                        .groupingBy { it.rarity }
                        .eachCount()
                )
            }

            item(key = "filter") {
                Box(Modifier.padding(vertical = Space.xs)) {
                    SlidingSegmented(
                        items = TrophyFilter.entries.toList(),
                        selected = filter,
                        labelOf = { it.label },
                        onSelect = { filter = it }
                    )
                }
            }

            if (shown.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (filter == TrophyFilter.DONE) "Aucun trophée pour l'instant."
                        else "Tout est débloqué. Chapeau.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Space.xxl)
                    )
                }
            }

            items(shown, key = { it.id }) { def ->
                TrophyRow(
                    def = def,
                    unlocked = def.id in unlockedIds,
                    unlockedAt = achievements.find { it.id == def.id }?.unlockedAt
                )
            }
        }
    }
}

/** La carte de tête : combien sur combien, et de quelle rareté. */
@Composable
private fun ProgressCard(
    unlocked: Int,
    total: Int,
    progress: Float,
    byRarity: Map<String, Int>
) {
    BubbleCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$unlocked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    " / $total trophées",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            ProgressTrack(progress = progress, height = 6.dp)

            // Les raretés obtenues, quand il y en a : trois légendaires disent
            // plus que « 12 trophées ».
            val earned = listOf("légendaire", "épique", "rare", "commun")
                .mapNotNull { r -> byRarity[r]?.takeIf { it > 0 }?.let { r to it } }

            if (earned.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    items(earned) { (rarity, count) ->
                        BubbleChip("$count $rarity", accent = rarityColor(rarity))
                    }
                }
            }
        }
    }
}

/** Une ligne de trophée : obtenu en couleur, à faire en sourdine. */
@Composable
private fun TrophyRow(
    def: AchievementDef,
    unlocked: Boolean,
    unlockedAt: Long?
) {
    val palette = LocalAskipPalette.current
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(def.color))
    }.getOrElse { Color.Gray }

    val fill by animateColorAsState(
        targetValue = if (unlocked) accent.copy(alpha = 0.12f) else palette.bubble,
        animationSpec = Motion.spring(),
        label = "trophyFill"
    )

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        fill = fill,
        elevation = if (unlocked) 4.dp else 1.dp,
        gloss = if (unlocked) 0.5f else 0.2f
    ) {
        Row(
            modifier = Modifier.padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            // La pastille garde toujours un fond : sur le thème beige, un
            // cadenas gris sur beige finissait par disparaître.
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) accent.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                AskipGlyph(
                    kind = if (unlocked) glyphForAchievement(def.id) else GlyphKind.LOCK,
                    size = 22.dp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    Text(
                        def.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RarityTag(def.rarity, dimmed = !unlocked)
                }

                // Le titre reste lisible même verrouillé : cacher l'objectif
                // n'aide personne à l'atteindre.
                Text(
                    def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(if (unlocked) 1f else 0.75f)
                )

                if (unlocked && unlockedAt != null) {
                    Text(
                        "Obtenu le ${
                            DEBLOCAGE.get()!!.format(Date(unlockedAt))
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }
    }
}

/** La pastille de rareté. Toujours du texte lisible sur son propre fond. */
@Composable
private fun RarityTag(rarity: String, dimmed: Boolean) {
    val c = rarityColor(rarity)
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.pill))
            .background(if (dimmed) c.copy(alpha = 0.18f) else c)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            rarity,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (dimmed) c else readableOn(c)
        )
    }
}
