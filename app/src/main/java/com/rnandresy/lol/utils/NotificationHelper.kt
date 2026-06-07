package com.rnandresy.lol.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rnandresy.lol.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CH_MESSAGES       = "askip_messages"
        const val CH_POSTS          = "askip_posts"
        const val CH_MENTIONS       = "askip_mentions"
        const val CH_ADMIN          = "askip_admin"
        private var idCounter = 1000
    }

    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_MESSAGES, "Messages privés", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nouveaux messages reçus"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_POSTS, "Nouveaux posts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Activité du fil Askip"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_MENTIONS, "Mentions", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Quand quelqu'un te mentionne"
                enableVibration(true)
            }
        )
        NotificationChannel(CH_ADMIN, "Admin Askip", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Annonces et messages de l'administration"
            enableVibration(true)
            enableLights(true)
        }.also { nm.createNotificationChannel(it) }
    }

    private fun hasPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        else true

    private fun tapIntent() = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // ── Notification admin (OBLIGATOIRE — ne peut pas être désactivée) ─────────
    fun showAdminPostNotification(username: String, content: String) {
        if (!hasPermission()) return
        runCatching {
            NotificationManagerCompat.from(context).notify(idCounter++,
                NotificationCompat.Builder(context, CH_ADMIN)
                    .setSmallIcon(android.R.drawable.ic_menu_share)
                    .setContentTitle("📢 Annonce de $username")
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(tapIntent())
                    .build()
            )
        }
    }

    // ── Mention @everyone par l'admin (OBLIGATOIRE) ───────────────────────────
    fun showEveryoneMentionNotification(fromUsername: String, content: String) {
        if (!hasPermission()) return
        runCatching {
            NotificationManagerCompat.from(context).notify(idCounter++,
                NotificationCompat.Builder(context, CH_ADMIN)
                    .setSmallIcon(android.R.drawable.ic_menu_share)
                    .setContentTitle("📣 $fromUsername vous interpelle à tous !")
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(tapIntent())
                    .build()
            )
        }
    }

    // ── Mention personnelle ───────────────────────────────────────────────────
    fun showMentionNotification(fromUsername: String, content: String, fromAdmin: Boolean) {
        if (!hasPermission()) return
        val channel = if (fromAdmin) CH_ADMIN else CH_MENTIONS
        runCatching {
            NotificationManagerCompat.from(context).notify(idCounter++,
                NotificationCompat.Builder(context, channel)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(if (fromAdmin) "👑 $fromUsername te mentionne !" else "💬 $fromUsername te mentionne")
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setAutoCancel(true)
                    .setPriority(if (fromAdmin) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(tapIntent())
                    .build()
            )
        }
    }

    // ── Message ───────────────────────────────────────────────────────────────
    fun showMessageNotification(sender: String, body: String, fromAdmin: Boolean) {
        if (!hasPermission()) return
        val channel = if (fromAdmin) CH_ADMIN else CH_MESSAGES
        runCatching {
            NotificationManagerCompat.from(context).notify(idCounter++,
                NotificationCompat.Builder(context, channel)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle(if (fromAdmin) "👑 $sender" else "💬 $sender")
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setPriority(if (fromAdmin) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(tapIntent())
                    .build()
            )
        }
    }

    // ── Nouveau post (utilisateur) ────────────────────────────────────────────
    fun showPostNotification(username: String, content: String) {
        if (!hasPermission()) return
        runCatching {
            NotificationManagerCompat.from(context).notify(idCounter++,
                NotificationCompat.Builder(context, CH_POSTS)
                    .setSmallIcon(android.R.drawable.ic_menu_share)
                    .setContentTitle("🔥 $username a posté")
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(tapIntent())
                    .build()
            )
        }
    }
}