package com.rnandresy.lol.ui.confession

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rnandresy.lol.ui.feed.PostCard
import com.rnandresy.lol.viewmodel.AskipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfessionsScreen(
    vm: AskipViewModel,
    onOpenComments: (String) -> Unit
) {
    val confessions  by vm.confessions.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val uid           = vm.currentUserId

    var showCreate by remember { mutableStateOf(false) }
    var text       by remember { mutableStateOf("") }

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) vm.refreshFeed()
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullState.endRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Confessions 🎭", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Tout le monde est anonyme ici 🤫 même l'admin ne peut pas tout voir..",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true; text = "" },
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text("Confesser") }
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            if (confessions.isEmpty() && !isRefreshing) {
                Column(
                    modifier            = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎭", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aucune confession pour l'instant…",
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sois courageux, confesse-toi ! 😈",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(bottom = 100.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(confessions, key = { it.id }) { post ->
                        PostCard(
                            post          = post,
                            currentUid    = uid,
                            onAvatarClick = { /* anonyme */ },
                            onReaction    = { emoji -> vm.toggleReaction(post, emoji) },
                            onVotePoll    = { },
                            onComment     = { onOpenComments(post.id) },
                            onPin         = { },
                            onDelete      = { vm.deletePost(post.id) }
                        )
                    }
                }
            }
            PullToRefreshContainer(
                state    = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎭")
                    Text("Confession anonyme")
                }
            },
            text = {
                Column {
                    Text(
                        "Personne ne saura que c'est toi 🤫 vazzyyy!!!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value         = text,
                        onValueChange = { if (it.length <= 300) text = it },
                        placeholder   = { Text("Ta confession secrète…") },
                        modifier      = Modifier.fillMaxWidth().height(120.dp),
                        maxLines      = 6
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${text.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            vm.createPost(text.trim(), "confession")
                            showCreate = false
                        }
                    },
                    enabled = text.isNotBlank()
                ) { Text("Confesser 🎭") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Annuler") }
            }
        )
    }
}