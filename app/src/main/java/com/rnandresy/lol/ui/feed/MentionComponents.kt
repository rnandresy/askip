package com.rnandresy.lol.ui.feed

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.AdminBadgeLabel
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.utils.isAdmin

@Composable
fun MentionTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    allProfiles: List<UserProfile>,
    currentUserId: String,
    isAdminUser: Boolean,
    placeholder: String    = "Écris quelque chose…",
    modifier: Modifier     = Modifier,
    maxLines: Int          = 8,
    shape: Shape           = RoundedCornerShape(12.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val text       = value.text
    val cursor     = value.selection.start.coerceIn(0, text.length)
    val atIndex    = text.substring(0, cursor).lastIndexOf('@')
    val typing     = atIndex >= 0 && text.substring(atIndex).none { it == ' ' }
    val query      = if (typing) text.substring(atIndex + 1).lowercase() else ""

    val suggestions = remember(query, allProfiles, isAdminUser) {
        buildList {
            if (isAdminUser && (query.isBlank() || "everyone".startsWith(query) ||
                        "tous".startsWith(query))) add(null)
            addAll(
                allProfiles.filter {
                    it.userId != currentUserId &&
                            (query.isBlank() || it.username.lowercase().startsWith(query))
                }.take(6)
            )
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            placeholder     = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier        = Modifier.fillMaxWidth(),
            shape           = shape,
            maxLines        = maxLines,
            keyboardOptions = keyboardOptions,
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (typing && suggestions.isNotEmpty()) {
            Surface(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                color     = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border    = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(suggestions) { profile ->
                        if (profile == null) {
                            // @everyone
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(insertMention(value, atIndex, "everyone"))
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color  = AdminGold.copy(alpha = 0.1f),
                                    shape  = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "@everyone",
                                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = AdminGold
                                    )
                                }
                                Text(
                                    "Mentionner tout le monde",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(insertMention(value, atIndex, profile.username))
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AskipAvatar(
                                    username    = profile.username,
                                    photoUrl    = profile.photoUrl,
                                    size        = 32.dp,
                                    isAdminUser = isAdmin(profile.userId)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "@${profile.username}",
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isAdmin(profile.userId)) AdminBadgeLabel()
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

private fun insertMention(current: TextFieldValue, atIndex: Int, username: String): TextFieldValue {
    val text     = current.text
    val cursor   = current.selection.start.coerceIn(0, text.length)
    val before   = text.substring(0, atIndex)
    val after    = text.substring(cursor)
    val inserted = "@$username "
    val newText  = "$before$inserted$after"
    return TextFieldValue(text = newText, selection = TextRange(before.length + inserted.length))
}