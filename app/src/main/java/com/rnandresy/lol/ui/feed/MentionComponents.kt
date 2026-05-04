package com.rnandresy.lol.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.utils.isAdmin

// ── Couleur des mentions ──────────────────────────────────────────────────────

val MentionColor      = Color(0xFF7C4DFF)
val MentionEveryoneColor = Color(0xFFE91E63)

// ── Texte avec mentions colorées (affichage) ──────────────────────────────────

@Composable
fun MentionText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        val words = text.split(" ")
        words.forEachIndexed { i, word ->
            when {
                word == "@everyone" || word == "@tout_le_monde" -> {
                    withStyle(
                        SpanStyle(
                            color      = MentionEveryoneColor,
                            fontWeight = FontWeight.Bold,
                            background = MentionEveryoneColor.copy(alpha = 0.1f)
                        )
                    ) { append(word) }
                }
                word.startsWith("@") && word.length > 1 -> {
                    withStyle(
                        SpanStyle(
                            color      = MentionColor,
                            fontWeight = FontWeight.Bold,
                            background = MentionColor.copy(alpha = 0.1f)
                        )
                    ) { append(word) }
                }
                else -> append(word)
            }
            if (i < words.lastIndex) append(" ")
        }
    }
    Text(text = annotated, style = style, modifier = modifier)
}

// ── Champ texte avec auto-complétion des mentions ─────────────────────────────

@Composable
fun MentionTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    allProfiles: List<UserProfile>,
    currentUserId: String,
    isAdminUser: Boolean,
    placeholder: String = "Écris quelque chose moa …",
    modifier: Modifier = Modifier,
    maxLines: Int = 8,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    // Détecte si on est en train d'écrire une mention
    val text        = value.text
    val cursor      = value.selection.start.coerceIn(0, text.length)
    val textBefore  = text.substring(0, cursor)
    val atIndex     = textBefore.lastIndexOf('@')
    val isTypingMention = atIndex >= 0 &&
            textBefore.substring(atIndex).none { it == ' ' }
    val mentionQuery    = if (isTypingMention) textBefore.substring(atIndex + 1).lowercase() else ""

    // Suggestions filtrées
    val suggestions = remember(mentionQuery, allProfiles, isAdminUser) {
        buildList {
            // @everyone uniquement pour l'admin
            if (isAdminUser && ("everyone".startsWith(mentionQuery) ||
                        "tout_le_monde".startsWith(mentionQuery) ||
                        "tous".startsWith(mentionQuery))
            ) {
                add(null) // null = @everyone
            }
            // Utilisateurs normaux
            addAll(
                allProfiles
                    .filter { it.userId != currentUserId }
                    .filter {
                        mentionQuery.isBlank() ||
                                it.username.lowercase().startsWith(mentionQuery)
                    }
                    .take(6)
            )
        }
    }

    val showSuggestions = isTypingMention && suggestions.isNotEmpty()

    Column(modifier = modifier) {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder) },
            modifier      = Modifier.fillMaxWidth(),
            shape         = shape,
            maxLines      = maxLines,
            keyboardOptions = keyboardOptions
        )

        // ── Dropdown suggestions ──────────────────────────────────────────────
        if (showSuggestions) {
            Surface(
                shape     = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                modifier  = Modifier.fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(suggestions) { profile ->
                        if (profile == null) {
                            // @everyone (admin only)
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(insertMention(value, atIndex, "everyone"))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color  = MentionEveryoneColor.copy(alpha = 0.15f),
                                    shape  = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        "@everyone",
                                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MentionEveryoneColor
                                    )
                                }
                                Text(
                                    "Mentionner tout le monde 📢",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Utilisateur normal
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(insertMention(value, atIndex, profile.username))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                UserAvatar(
                                    username = profile.username,
                                    size     = 32,
                                    isAdmin  = isAdmin(profile.userId)
                                )
                                Column {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "@${profile.username}",
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MentionColor
                                        )
                                        if (isAdmin(profile.userId) || profile.isAdmin) {
                                            AdminBadge()
                                        }
                                    }
                                    if (profile.classeENI.isNotBlank()) {
                                        Text(
                                            profile.classeENI,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

// ── Utilitaire : insère la mention dans le texte ──────────────────────────────

private fun insertMention(
    current: TextFieldValue,
    atIndex: Int,
    username: String
): TextFieldValue {
    val text      = current.text
    val cursor    = current.selection.start.coerceIn(0, text.length)
    val before    = text.substring(0, atIndex)            // texte avant le @
    val after     = text.substring(cursor)                // texte après le curseur
    val inserted  = "@$username "
    val newText   = "$before$inserted$after"
    val newCursor = before.length + inserted.length
    return TextFieldValue(
        text      = newText,
        selection = TextRange(newCursor)
    )
}