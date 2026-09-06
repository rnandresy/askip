package com.rnandresy.lol.ui.confession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rnandresy.lol.ui.components.barEdge
import com.rnandresy.lol.ui.components.BubbleButton
import com.rnandresy.lol.ui.components.BubbleIconButton
import com.rnandresy.lol.ui.components.BubbleSize
import com.rnandresy.lol.ui.components.BubbleTone
import com.rnandresy.lol.ui.components.EmptyState
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
    val canScoop     by vm.canScoopToday.collectAsState()
    val uid           = vm.currentUserId

    var showCreate by remember { mutableStateOf(false) }
    var confText   by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.barEdge(),
                title = {
                    Column {
                        Text("Confessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Toujours anonyme ◌",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            // Une grosse bulle plutôt qu'un bouton flottant Material : c'est le
            // même relief que partout ailleurs dans l'app.
            BubbleIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Écrire une confession",
                onClick = { showCreate = true; confText = "" },
                tone = BubbleTone.PRIMARY,
                diameter = 58.dp
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        // Material3 1.3 remplace PullToRefreshContainer par PullToRefreshBox :
        // c'est lui qui gère le geste, l'indicateur et l'état de rafraîchissement.
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { vm.refreshFeed() },
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            if (confessions.isEmpty() && !isRefreshing) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    EmptyState("◌", "Aucune confession", "Appuie sur + pour te confesser anonymement")
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(confessions, key = { it.id }) { post ->
                        PostCard(
                            post          = post,
                            currentUid    = uid,
                            onAvatarClick = { },
                            onReaction    = { vm.toggleReaction(post, it) },
                            onVotePoll    = { },
                            onComment     = { onOpenComments(post.id) },
                            onPin         = { vm.togglePin(post) },
                            onDelete      = { vm.deletePost(post.id) },
                            onVoteVerdict = { vm.voteVerdict(post, it) },
                            onScoop       = { vm.giveScoop(post) },
                            canScoop      = canScoop
                        )
                        HorizontalDivider(
                            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title            = {
                Text("Confession anonyme ◌", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Personne ne saura que c'est toi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value         = confText,
                        onValueChange = { if (it.length <= 300) confText = it },
                        placeholder   = { Text("Ta confession…") },
                        modifier      = Modifier.fillMaxWidth().height(120.dp),
                        maxLines      = 6,
                        shape         = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    Text(
                        "${confText.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                BubbleButton(
                    text = "Publier",
                    emoji = "◌",
                    onClick = {
                        if (confText.isNotBlank()) {
                            vm.createPost(confText.trim(), "confession")
                            showCreate = false
                        }
                    },
                    enabled = confText.isNotBlank(),
                    tone = BubbleTone.PRIMARY,
                    size = BubbleSize.SMALL
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Annuler") }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
}