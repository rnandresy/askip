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
        const val CHANNEL_MESSAGES = "askip_messages"
        const val CHANNEL_POSTS = "askip_posts"
        private var msgNotifId = 1000
        private var postNotifId = 2000
    }

    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canal messages — HIGH priority
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages privés",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les nouveaux messages"
                enableVibration(true)
                enableLights(true)
            }
        )

        // Canal posts — DEFAULT priority
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_POSTS,
                "Nouveaux posts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les nouveaux posts du fil"
            }
        )
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun launchIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showMessageNotification(senderName: String, content: String) {
        if (!hasPermission()) return
        val notif = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("💬 $senderName")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launchIntent())
            .build()
        NotificationManagerCompat.from(context).notify(msgNotifId++, notif)
    }

    fun showPostNotification(username: String, content: String) {
        if (!hasPermission()) return
        val notif = NotificationCompat.Builder(context, CHANNEL_POSTS)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("📢 Askip — $username a posté")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(launchIntent())
            .build()
        NotificationManagerCompat.from(context).notify(postNotifId++, notif)
    }
}