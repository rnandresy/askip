package com.rnandresy.lol.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.AdminBadgeLabel
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.AskipGlyph
import com.rnandresy.lol.ui.components.BubbleCard
import com.rnandresy.lol.ui.components.BubbleChip
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.GlyphKind
import com.rnandresy.lol.ui.components.formatTs
import com.rnandresy.lol.ui.components.glyphForSymbol
import com.rnandresy.lol.ui.components.rememberTapFeedback
import com.rnandresy.lol.ui.theme.LocalAskipPalette
import com.rnandresy.lol.ui.theme.Radius
import com.rnandresy.lol.ui.theme.Space
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: AskipViewModel,
    onOpenPost: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenGroup: (String) -> Unit
) {
    val query         by vm.searchQuery.collectAsState()
    val results       by vm.searchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs         = listOf("Tout", "Posts", "Membres", "Groupes")

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val palette = LocalAskipPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Barre de recherche ───────────────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = { vm.searchQuery.value = it },
            placeholder = { Text("posts, membres, groupes…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    BubbleIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Effacer la recherche",
                        onClick = { vm.searchQuery.value = "" },
                        diameter = 30.dp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(Radius.pill),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = palette.bubble,
                focusedContainerColor = palette.bubble,
                unfocusedBorderColor = palette.bubbleBorder,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg, vertical = Space.md)
                .focusRequester(focusRequester)
        )

        // Les filtres ne servent à rien tant qu'on n'a rien cherché : ils
        // n'apparaissent qu'une fois la requête tapée.
        if (query.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                items(tabs.size) { i ->
                    val count = when (i) {
                        1 -> results.posts.size
                        2 -> results.users.size
                        3 -> results.groups.size
                        else -> results.posts.size + results.users.size + results.groups.size
                    }
                    BubbleChip(
                        label = tabs[i] + if (count > 0) " ($count)" else "",
                        filled = selectedTab == i,
                        accent = if (selectedTab == i) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { selectedTab = i }
                    )
                }
            }
        }

        // ── Résultats ─────────────────────────────────────────────────────────
        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AskipGlyph(kind = GlyphKind.SEARCH, size = 46.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Recherche dans Askip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Posts, membres, groupes…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val totalEmpty = results.posts.isEmpty() && results.users.isEmpty() && results.groups.isEmpty()
            if (totalEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AskipGlyph(kind = GlyphKind.VEIL, size = 38.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("Aucun résultat pour « $query »", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = Space.lg,
                        vertical = Space.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    val showPosts  = selectedTab == 0 || selectedTab == 1
                    val showUsers  = selectedTab == 0 || selectedTab == 2
                    val showGroups = selectedTab == 0 || selectedTab == 3

                    // ── Posts ──────────────────────────────────────────────────
                    if (showPosts && results.posts.isNotEmpty()) {
                        item {
                            SearchSectionHeader("Posts")
                        }
                        items(results.posts, key = { it.id }) { post ->
                            SearchPostRow(post = post, onClick = { onOpenPost(post.id) })
                        }
                    }

                    // ── Membres ────────────────────────────────────────────────
                    if (showUsers && results.users.isNotEmpty()) {
                        item { SearchSectionHeader("Membres") }
                        items(results.users, key = { it.userId }) { user ->
                            SearchUserRow(user = user, onClick = { onOpenProfile(user.userId) })
                        }
                    }

                    // ── Groupes ────────────────────────────────────────────────
                    if (showGroups && results.groups.isNotEmpty()) {
                        item { SearchSectionHeader("Groupes") }
                        items(results.groups, key = { it.id }) { group ->
                            SearchGroupRow(group = group, onClick = { onOpenGroup(group.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Space.sm, bottom = Space.xxs)
    )
}

@Composable
private fun SearchPostRow(post: Post, onClick: () -> Unit) {
    val tap = rememberTapFeedback()

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        gloss = 0.28f,
        onClick = { tap(); onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            ResultTile(
                glyph = when (post.postType) {
                    "poll" -> GlyphKind.CHART
                    "confession" -> GlyphKind.VEIL
                    else -> GlyphKind.STAR
                },
                size = 40.dp
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (post.isAnonymous) "Quelqu'un" else post.username,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    post.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatTs(post.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** La pastille carrée qui ouvre un résultat : rumeur ou groupe. */
@Composable
private fun ResultTile(glyph: GlyphKind, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(Radius.xs))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AskipGlyph(kind = glyph, size = if (size > 42.dp) 21.dp else 18.dp)
    }
}

@Composable
private fun SearchUserRow(user: UserProfile, onClick: () -> Unit) {
    val userIsAdmin = isAdmin(user.userId) || user.isAdmin
    val tap = rememberTapFeedback()

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        gloss = 0.28f,
        onClick = { tap(); onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            AskipAvatar(
                username = user.username,
                photoUrl = user.photoUrl,
                size = 44.dp,
                isAdminUser = userIsAdmin
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Text(
                        user.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (userIsAdmin) AdminBadgeLabel()
                }
                if (user.classeENI.isNotBlank()) {
                    Text(
                        "${user.classeENI}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text("›", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchGroupRow(group: Group, onClick: () -> Unit) {
    val tap = rememberTapFeedback()

    BubbleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        gloss = 0.28f,
        onClick = { tap(); onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            ResultTile(glyph = glyphForSymbol(group.emoji), size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${group.members.size} membre(s)${if (group.description.isNotBlank()) " · ${group.description}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Text("›", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
