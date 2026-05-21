package com.rnandresy.lol.ui.search

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.ui.components.AdminBadgeLabel
import com.rnandresy.lol.ui.components.AskipAvatar
import com.rnandresy.lol.ui.components.formatTs
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

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Barre de recherche ────────────────────────────────────────────────
        Surface(shadowElevation = 4.dp) {
            Column {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { vm.searchQuery.value = it },
                    placeholder   = { Text("posts, membres, groupes…") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    trailingIcon  = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { vm.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(16.dp),
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .focusRequester(focusRequester)
                )

                if (query.isNotBlank()) {
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                        tabs.forEachIndexed { i, tab ->
                            Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                                val count = when (i) {
                                    1 -> results.posts.size
                                    2 -> results.users.size
                                    3 -> results.groups.size
                                    else -> results.posts.size + results.users.size + results.groups.size
                                }
                                Text(
                                    "$tab${if (count > 0) " ($count)" else ""}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    style    = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Résultats ─────────────────────────────────────────────────────────
        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
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
                        Text("🤷", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Aucun résultat pour « $query »", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val showPosts  = selectedTab == 0 || selectedTab == 1
                    val showUsers  = selectedTab == 0 || selectedTab == 2
                    val showGroups = selectedTab == 0 || selectedTab == 3

                    // ── Posts ──────────────────────────────────────────────────
                    if (showPosts && results.posts.isNotEmpty()) {
                        item {
                            SearchSectionHeader("📢 Posts")
                        }
                        items(results.posts, key = { it.id }) { post ->
                            SearchPostRow(post = post, onClick = { onOpenPost(post.id) })
                        }
                    }

                    // ── Membres ────────────────────────────────────────────────
                    if (showUsers && results.users.isNotEmpty()) {
                        item { SearchSectionHeader("👤 Membres") }
                        items(results.users, key = { it.userId }) { user ->
                            SearchUserRow(user = user, onClick = { onOpenProfile(user.userId) })
                        }
                    }

                    // ── Groupes ────────────────────────────────────────────────
                    if (showGroups && results.groups.isNotEmpty()) {
                        item { SearchSectionHeader("👥 Groupes") }
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
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SearchPostRow(post: Post, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color  = MaterialTheme.colorScheme.surfaceVariant,
            shape  = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    when (post.postType) { "poll" -> "📊"; "confession" -> "🎭"; else -> "📢" },
                    fontSize = 18.sp
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (post.isAnonymous) "Quelqu'un 🎭" else post.username,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                post.content, style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(formatTs(post.timestamp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun SearchUserRow(user: UserProfile, onClick: () -> Unit) {
    val userIsAdmin = isAdmin(user.userId) || user.isAdmin
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AskipAvatar(username = user.username, photoUrl = user.photoUrl, size = 44.dp, isAdminUser = userIsAdmin)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (userIsAdmin) AdminBadgeLabel()
            }
            if (user.classeENI.isNotBlank()) {
                Text("🎓 ${user.classeENI}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(start = 72.dp))
}

@Composable
private fun SearchGroupRow(group: Group, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color    = MaterialTheme.colorScheme.secondaryContainer,
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(group.emoji, fontSize = 22.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "${group.members.size} membre(s)${if (group.description.isNotBlank()) " · ${group.description}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(start = 72.dp))
}