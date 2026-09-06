package com.rnandresy.lol.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.barEdge
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.ProgressTrack
import com.rnandresy.lol.ui.components.SheetAction
import com.rnandresy.lol.ui.components.SheetHeader
import com.rnandresy.lol.ui.components.TapArea
import com.rnandresy.lol.ui.components.glyphForSymbol
import com.rnandresy.lol.ui.components.readableOn
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.AVATAR_FRAMES
import com.rnandresy.lol.utils.ENI_CLASSES
import com.rnandresy.lol.utils.STORY_COLORS
import com.rnandresy.lol.utils.STORY_MARKS
import com.rnandresy.lol.viewmodel.AskipViewModel

/** Les statuts proposés. Rien d'obligatoire : « — Aucun — » reste possible. */
private val REL_STATUSES = listOf(
    "Célibataire", "En couple", "Fiancé(e)", "Voay", "Mitady..",
    "Marié(e)", "C'est compliqué", "Préfère ne pas dire"
)

/** Quelle liste déroulante est ouverte, le cas échéant. */
private enum class EditSheet { STATUS, CLASS }

/**
 * L'édition du profil.
 *
 * Le formulaire était une longue liste de champs tous au même niveau. Il est
 * maintenant rangé en quatre cartes — identité, humeur, apparence, détails —
 * et les deux listes déroulantes sont devenues des feuilles, plus faciles à
 * viser au pouce que des menus qui se déplient sous le doigt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    vm: AskipViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val profile by vm.myProfile.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val uploadProgress by vm.uploadProgress.collectAsState()

    // Pendant un envoi, les commandes photo se verrouillent : sans ça, deux
    // appuis lancent deux envois pour la même image.
    val busy by vm.loading.collectAsState()

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadAvatar(it) } }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadCover(it) } }

    var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var age by remember(profile) {
        mutableStateOf(profile?.age?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var relStatus by remember(profile) { mutableStateOf(profile?.relationshipStatus ?: "") }
    var classeENI by remember(profile) { mutableStateOf(profile?.classeENI ?: "") }
    var themeColor by remember(profile) { mutableStateOf(profile?.themeColor ?: "#7C4DFF") }
    var frame by remember(profile) { mutableStateOf(profile?.avatarFrame ?: "none") }
    var moodEmoji by remember(profile) { mutableStateOf(profile?.moodEmoji ?: "") }
    var moodText by remember(profile) { mutableStateOf(profile?.moodText ?: "") }

    var sheet by remember { mutableStateOf<EditSheet?>(null) }

    val oldUsername = profile?.username ?: ""
    val usernameChanged = username.trim() != oldUsername && username.isNotBlank()

    val save = {
        val data = mutableMapOf<String, Any?>(
            "username" to username.trim(),
            "bio" to bio.trim(),
            "relationshipStatus" to relStatus,
            "classeENI" to classeENI,
            "hasBadgeENI" to classeENI.isNotBlank(),
            "themeColor" to themeColor,
            "avatarFrame" to frame,
            "moodEmoji" to moodEmoji,
            "moodText" to moodText.trim()
        )
        age.toIntOrNull()?.let { data["age"] = it }
        vm.updateProfile(data, onSaved)
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = { Text("Mon profil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(Modifier.padding(start = Space.md)) {
                        BubbleIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onBack,
                            enabled = !isSyncing
                        )
                    }
                },
                actions = {
                    Box(Modifier.padding(end = Space.lg)) {
                        // Pendant la synchro du pseudo on ne peut ni repartir ni
                        // renvoyer : deux enregistrements se marcheraient dessus.
                        if (isSyncing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Synchro…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            BubbleButton(
                                text = "Enregistrer",
                                onClick = save,
                                tone = BubbleTone.PRIMARY,
                                size = BubbleSize.SMALL,
                                enabled = username.isNotBlank()
                            )
                        }
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
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            PhotoSection(
                profile = profile,
                uploadProgress = uploadProgress,
                busy = busy,
                onPickCover = {
                    coverPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onPickAvatar = {
                    avatarPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onRemoveCover = vm::deleteCoverPhoto,
                onRemoveAvatar = vm::deleteProfilePhoto
            )

            IdentitySection(
                username = username,
                onUsername = { username = it },
                usernameChanged = usernameChanged,
                age = age,
                onAge = { age = it },
                bio = bio,
                onBio = { bio = it }
            )

            MoodSection(
                moodEmoji = moodEmoji,
                onMoodEmoji = { moodEmoji = it },
                moodText = moodText,
                onMoodText = { moodText = it }
            )

            LookSection(
                themeColor = themeColor,
                onThemeColor = { themeColor = it },
                frame = frame,
                onFrame = { frame = it }
            )

            DetailsSection(
                relStatus = relStatus,
                classeENI = classeENI,
                onOpenStatus = { sheet = EditSheet.STATUS },
                onOpenClass = { sheet = EditSheet.CLASS }
            )

            Spacer(Modifier.height(Space.huge))
        }
    }

    when (sheet) {
        EditSheet.STATUS -> ChoiceSheet(
            title = "Statut amoureux",
            subtitle = "Visible sur ton profil.",
            glyph = GlyphKind.HEART,
            options = REL_STATUSES,
            selected = relStatus,
            onPick = { relStatus = it; sheet = null },
            onDismiss = { sheet = null }
        )

        EditSheet.CLASS -> ChoiceSheet(
            title = "Classe ENI",
            subtitle = "Choisir une classe débloque le badge ENI.",
            glyph = GlyphKind.BOOK,
            options = ENI_CLASSES,
            selected = classeENI,
            onPick = { classeENI = it; sheet = null },
            onDismiss = { sheet = null }
        )

        null -> Unit
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Sections
// ═════════════════════════════════════════════════════════════════════════════

/** Pseudo, âge, bio — ce que les autres lisent en premier. */
@Composable
private fun IdentitySection(
    username: String,
    onUsername: (String) -> Unit,
    usernameChanged: Boolean,
    age: String,
    onAge: (String) -> Unit,
    bio: String,
    onBio: (String) -> Unit
) {
    EditCard("Identité", GlyphKind.PEOPLE) {
        EditField(
            value = username,
            onValueChange = onUsername,
            label = "Pseudo",
            imeAction = ImeAction.Next,
            error = if (username.isBlank()) "Requis" else null
        )

        // Le changement de pseudo repasse sur tout l'historique : mieux vaut
        // le dire avant l'enregistrement que de laisser croire à un bug.
        if (usernameChanged) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                    .padding(Space.md),
                verticalAlignment = Alignment.Top
            ) {
                AskipGlyph(kind = GlyphKind.FLAG, size = 13.dp)
                Spacer(Modifier.width(Space.sm))
                Text(
                    "Ton nouveau pseudo remplacera l'ancien sur tous tes posts, " +
                        "commentaires et messages.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        EditField(
            value = age,
            onValueChange = { onAge(it.take(3).filter(Char::isDigit)) },
            label = "Âge",
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Number
        )

        EditField(
            value = bio,
            onValueChange = { if (it.length <= 150) onBio(it) },
            label = "Bio (${bio.length}/150)",
            singleLine = false,
            minHeight = 92.dp
        )
    }
}

/** L'humeur du jour : un emoji, et une phrase courte si l'envie prend. */
@Composable
private fun MoodSection(
    moodEmoji: String,
    onMoodEmoji: (String) -> Unit,
    moodText: String,
    onMoodText: (String) -> Unit
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    EditCard("Humeur du jour", GlyphKind.MOON) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            contentPadding = PaddingValues(vertical = Space.xxs)
        ) {
            items(STORY_MARKS.take(12)) { emoji ->
                val picked = moodEmoji == emoji
                TapArea(
                    onTap = { tap(); onMoodEmoji(if (picked) "" else emoji) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (picked) palette.scoop.copy(alpha = 0.22f) else palette.bubble)
                        .border(
                            width = if (picked) 2.dp else 1.dp,
                            color = if (picked) palette.scoop else palette.bubbleBorder,
                            shape = CircleShape
                        )
                ) {
                    AskipGlyph(
                        kind = glyphForSymbol(emoji),
                        size = 20.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Le texte n'a de sens qu'accompagné : sans emoji, rien ne s'affiche.
        if (moodEmoji.isNotBlank()) {
            EditField(
                value = moodText,
                onValueChange = { if (it.length <= 50) onMoodText(it) },
                label = "Une phrase (optionnel)"
            )
        }
    }
}

/** Couleur du profil et cadre d'avatar : tout ce qui est décoratif. */
@Composable
private fun LookSection(
    themeColor: String,
    onThemeColor: (String) -> Unit,
    frame: String,
    onFrame: (String) -> Unit
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    EditCard("Apparence", GlyphKind.GEM) {
        SubLabel("Couleur du profil")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            contentPadding = PaddingValues(vertical = Space.xxs)
        ) {
            items(STORY_COLORS) { hex ->
                val c = runCatching {
                    Color(android.graphics.Color.parseColor(hex))
                }.getOrElse { Color.Gray }
                val picked = themeColor == hex

                TapArea(
                    onTap = { tap(); onThemeColor(hex) },
                    modifier = Modifier
                        .size(if (picked) 40.dp else 34.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (picked) 3.dp else 1.dp,
                            // Une pastille claire sur fond clair a besoin d'un
                            // trait, sinon elle disparaît dans le thème beige.
                            color = if (picked) palette.bubble else palette.bubbleBorder,
                            shape = CircleShape
                        )
                ) {
                    if (picked) {
                        AskipGlyph(
                            kind = GlyphKind.CHECK,
                            size = 15.dp,
                            tint = readableOn(c),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        SubLabel("Cadre de l'avatar")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            contentPadding = PaddingValues(vertical = Space.xxs)
        ) {
            items(AVATAR_FRAMES.entries.toList()) { (key, label) ->
                BubbleChip(
                    label = label,
                    filled = frame == key,
                    accent = if (frame == key) palette.scoop
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onFrame(key) }
                )
            }
        }
    }
}

/** Statut amoureux et classe ENI : deux choix, deux feuilles. */
@Composable
private fun DetailsSection(
    relStatus: String,
    classeENI: String,
    onOpenStatus: () -> Unit,
    onOpenClass: () -> Unit
) {
    EditCard("Détails", GlyphKind.DOC) {
        PickerRow(
            glyph = GlyphKind.HEART,
            label = "Statut amoureux",
            value = relStatus.ifBlank { "Non précisé" },
            onClick = onOpenStatus
        )
        PickerRow(
            glyph = GlyphKind.BOOK,
            label = "Classe ENI",
            value = classeENI.ifBlank { "Non précisée" },
            hint = if (classeENI.isNotBlank()) "Badge ENI attribué"
            else "Choisis ta classe pour débloquer le badge",
            onClick = onOpenClass
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Briques
// ═════════════════════════════════════════════════════════════════════════════

/** Une carte de formulaire, titrée. */
@Composable
private fun EditCard(
    title: String,
    glyph: GlyphKind,
    content: @Composable () -> Unit
) {
    BubbleCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                AskipGlyph(kind = glyph, size = 14.dp)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

/** Un intertitre dans une carte, pour séparer deux réglages voisins. */
@Composable
private fun SubLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Le champ texte de l'app : mêmes coins, mêmes couleurs partout. */
@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minHeight: Dp = 0.dp,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null
) {
    val palette = LocalAskipPalette.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.height(minHeight) else Modifier),
        shape = RoundedCornerShape(Radius.sm),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        supportingText = error?.let {
            { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = palette.bubble,
            focusedContainerColor = palette.bubble,
            unfocusedBorderColor = palette.bubbleBorder,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** Une ligne « libellé → valeur » qui ouvre une feuille de choix. */
@Composable
private fun PickerRow(
    glyph: GlyphKind,
    label: String,
    value: String,
    hint: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val tap = rememberTapFeedback()

    TapArea(
        onTap = { tap(); onClick() },
        scaleDown = 0.99f,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(palette.bubble)
            .border(1.dp, palette.bubbleBorder, RoundedCornerShape(Radius.sm))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AskipGlyph(kind = glyph, size = 15.dp)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "›",
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** La feuille de choix commune aux deux listes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(
    title: String,
    subtitle: String,
    glyph: GlyphKind,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(bottom = Space.xxl)
        ) {
            SheetHeader(title, subtitle)

            SheetAction(
                glyph = GlyphKind.CROSS,
                label = "Ne rien indiquer",
                onClick = { onPick("") }
            )
            options.forEach { option ->
                SheetAction(
                    glyph = if (option == selected) GlyphKind.CHECK else glyph,
                    label = option,
                    onClick = { onPick(option) }
                )
            }
        }
    }
}

/**
 * Un intertitre de section, repris par d'autres écrans du profil.
 *
 * Conservé tel quel : il sert encore ailleurs.
 */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Les deux photos : couverture et portrait.
 *
 * Elles vivent ici plutôt que sur le profil lui-même. Sur la couverture, les
 * commandes encombraient l'écran que tout le monde regarde, pour un geste
 * qu'on fait deux fois par an — et elles n'apparaissaient que sur son propre
 * profil, donc à moitié du temps.
 *
 * L'aperçu montre exactement ce que verront les autres, aux mêmes proportions.
 */
@Composable
private fun PhotoSection(
    profile: com.rnandresy.lol.model.UserProfile?,
    uploadProgress: Int,
    busy: Boolean,
    onPickCover: () -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveCover: () -> Unit,
    onRemoveAvatar: () -> Unit
) {
    val palette = LocalAskipPalette.current
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(profile?.themeColor ?: "#7C4DFF"))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    EditCard("Photos", GlyphKind.PHOTO) {
        // ── Couverture ───────────────────────────────────────────────────────
        SubLabel("Couverture")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(accent.copy(alpha = 0.20f))
                .border(1.dp, palette.bubbleBorder, RoundedCornerShape(Radius.sm))
        ) {
            if (!profile?.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile?.coverUrl,
                    contentDescription = "Aperçu de la couverture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    "Aucune couverture",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            BubbleButton(
                text = if (profile?.coverUrl.isNullOrBlank()) "Choisir" else "Changer",
                onClick = onPickCover,
                enabled = !busy,
                tone = BubbleTone.SOFT,
                size = BubbleSize.SMALL
            )
            if (!profile?.coverUrl.isNullOrBlank()) {
                BubbleButton(
                    text = "Retirer",
                    onClick = onRemoveCover,
                    enabled = !busy,
                    tone = BubbleTone.GHOST,
                    size = BubbleSize.SMALL
                )
            }
        }

        // ── Portrait ─────────────────────────────────────────────────────────
        SubLabel("Photo de profil")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, palette.bubbleBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!profile?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.photoUrl,
                        contentDescription = "Aperçu du portrait",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        profile?.username?.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                BubbleButton(
                    text = if (profile?.photoUrl.isNullOrBlank()) "Choisir" else "Changer",
                    onClick = onPickAvatar,
                    enabled = !busy,
                    tone = BubbleTone.SOFT,
                    size = BubbleSize.SMALL
                )
                if (!profile?.photoUrl.isNullOrBlank()) {
                    BubbleButton(
                        text = "Retirer",
                        onClick = onRemoveAvatar,
                        enabled = !busy,
                        tone = BubbleTone.GHOST,
                        size = BubbleSize.SMALL
                    )
                }
            }
        }

        if (uploadProgress in 1..99) {
            ProgressTrack(progress = uploadProgress / 100f, height = 4.dp, color = accent)
            Text(
                "Envoi : $uploadProgress %",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
