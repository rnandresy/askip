package com.rnandresy.lol.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.rnandresy.lol.ui.theme.AdminGold
import com.rnandresy.lol.ui.theme.AdminGoldBg
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Avatar universel ──────────────────────────────────────────────────────────
@Composable
fun AskipAvatar(
    username: String,
    photoUrl: String   = "",
    size: Dp           = 44.dp,
    isAdminUser: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (isAdminUser) AdminGold else MaterialTheme.colorScheme.outline
    val borderWidth = if (isAdminUser) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model              = photoUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
                loading            = {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            username.firstOrNull()?.uppercase() ?: "?",
                            fontSize   = (size.value / 2.5).sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (isAdminUser) AdminGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isAdminUser) AdminGoldBg
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    username.firstOrNull()?.uppercase() ?: "?",
                    fontSize   = (size.value / 2.5).sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isAdminUser) AdminGold
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Badge Admin ───────────────────────────────────────────────────────────────
@Composable
fun AdminBadgeLabel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color    = AdminGoldBg,
        shape    = RoundedCornerShape(4.dp),
        border   = BorderStroke(0.5.dp, AdminGold.copy(alpha = 0.6f))
    ) {
        Text(
            "👑",
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = AdminGold,
            fontSize   = 10.sp
        )
    }
}

// ── Badge ENI ─────────────────────────────────────────────────────────────────
@Composable
fun ENIBadgeLabel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color    = Color(0xFF0A1628),
        shape    = RoundedCornerShape(4.dp),
        border   = BorderStroke(0.5.dp, Color(0xFF1565C0).copy(alpha = 0.6f))
    ) {
        Text(
            "🎓 ENI",
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF4A9EFF),
            fontSize   = 10.sp
        )
    }
}

// ── Chip badge personnalisé ───────────────────────────────────────────────────
@Composable
fun CustomBadgeChip(displayName: String, colorHex: String, modifier: Modifier = Modifier) {
    val color = runCatching {
        Color(android.graphics.Color.parseColor(colorHex))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    Surface(
        modifier = modifier,
        color    = color.copy(alpha = 0.12f),
        shape    = RoundedCornerShape(6.dp),
        border   = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            displayName,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color
        )
    }
}

// ── Texte avec mentions colorées ──────────────────────────────────────────────
@Composable
fun MentionText(
    text: String,
    style: TextStyle   = MaterialTheme.typography.bodyMedium,
    color: Color       = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val primary  = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val annotated = buildAnnotatedString {
        text.split(" ").forEachIndexed { i, word ->
            when {
                word == "@everyone" || word == "@tout_le_monde" || word == "@tous" -> {
                    withStyle(SpanStyle(
                        color      = tertiary,
                        fontWeight = FontWeight.Bold,
                        background = tertiary.copy(alpha = 0.1f)
                    )) { append(word) }
                }
                word.startsWith("@") && word.length > 1 -> {
                    withStyle(SpanStyle(
                        color      = primary,
                        fontWeight = FontWeight.SemiBold,
                        background = primary.copy(alpha = 0.08f)
                    )) { append(word) }
                }
                else -> withStyle(SpanStyle(color = color)) { append(word) }
            }
            if (i < text.split(" ").lastIndex) append(" ")
        }
    }
    Text(text = annotated, style = style, modifier = modifier)
}

// ── Lecteur vidéo — jamais d'autoplay ────────────────────────────────────────
@OptIn(UnstableApi::class)
@Composable
fun AskipVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player  = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false   // jamais d'autoplay
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    AndroidView(
        factory  = { ctx ->
            PlayerView(ctx).apply {
                this.player   = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

// ── Lecteur audio ─────────────────────────────────────────────────────────────
@Composable
fun AskipAudioPlayer(
    url: String, duration: Int, isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress  by remember { mutableStateOf(0f) }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url)); prepare()
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            val dur = player.duration.takeIf { it > 0 } ?: (duration.coerceAtLeast(1) * 1000L)
            val pos = player.currentPosition
            progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
            if (pos >= dur - 200L) {
                player.seekTo(0); player.pause()
                isPlaying = false; progress = 0f
            }
            delay(100L)
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    val tint  = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
    val track = if (isMe) Color.White.copy(0.25f) else MaterialTheme.colorScheme.outline

    Row(
        modifier              = modifier.widthIn(min = 160.dp, max = 240.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🎤", fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(2.dp),
                color      = tint,
                trackColor = track
            )
            Text(
                formatAudioDuration(if (isPlaying) (progress * duration).toInt() else duration),
                style  = MaterialTheme.typography.labelSmall,
                color  = tint.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
        IconButton(
            onClick  = {
                if (isPlaying) { player.pause(); isPlaying = false }
                else { if (progress >= 0.99f) { player.seekTo(0); progress = 0f }; player.play(); isPlaying = true }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null, tint = tint, modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Fichier joint ─────────────────────────────────────────────────────────────
@Composable
fun AskipFileItem(name: String, url: String, isMe: Boolean, modifier: Modifier = Modifier) {
    val context   = LocalContext.current
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier              = modifier.widthIn(max = 240.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(textColor.copy(0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.InsertDriveFile, null, tint = textColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name.take(28).ifBlank { "Fichier" },
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color      = textColor,
                maxLines   = 1
            )
            Text(
                "Ouvrir ↗",
                style    = MaterialTheme.typography.labelSmall,
                color    = textColor.copy(0.55f),
                modifier = Modifier.clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }
            )
        }
    }
}

// ── Divider avec label ────────────────────────────────────────────────────────
@Composable
fun LabelDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

// ── État vide ─────────────────────────────────────────────────────────────────
@Composable
fun EmptyState(emoji: String, title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 48.sp)
        Text(
            title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Bouton primaire Askip ─────────────────────────────────────────────────────
@Composable
fun AskipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier  = Modifier,
    enabled: Boolean    = true,
    isLoading: Boolean  = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(50.dp),
        enabled  = enabled && !isLoading,
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor   = MaterialTheme.colorScheme.background
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier  = Modifier.size(18.dp),
                color     = MaterialTheme.colorScheme.background,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Champ de texte Askip ──────────────────────────────────────────────────────
@Composable
fun AskipTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier  = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    isError: Boolean    = false,
    errorMessage: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            label           = { Text(label) },
            singleLine      = singleLine,
            isError         = isError,
            leadingIcon     = leadingIcon,
            trailingIcon    = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword)
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(12.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor     = MaterialTheme.colorScheme.error
            )
        )
        if (isError && errorMessage.isNotBlank()) {
            Text(
                errorMessage,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
            )
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────
fun formatTs(ts: Long): String = runCatching {
    SimpleDateFormat("dd MMM · HH:mm", Locale.FRENCH).format(Date(ts))
}.getOrElse { "" }

fun formatAudioDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)