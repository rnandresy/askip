package com.rnandresy.lol.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rnandresy.lol.utils.NotificationHelper

class AskipMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AskipFCM"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message reçu de: ${message.from}")

        val helper = NotificationHelper(applicationContext)

        // Données du message (priorité sur notification pour quand l'app est en foreground)
        val title = message.data["title"] ?: message.notification?.title ?: "Askip"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val type = message.data["type"] ?: "message"

        if (body.isBlank()) return

        when (type) {
            "post" -> helper.showPostNotification(title, body)
            else -> helper.showMessageNotification(
                title, body,
                fromAdmin = TODO()
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Nouveau token FCM: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d(TAG, "Token mis à jour") }
            .addOnFailureListener { Log.e(TAG, "Erreur token: ${it.message}") }
    }
}