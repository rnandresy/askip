package com.rnandresy.lol.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rnandresy.lol.utils.NotificationHelper
import com.rnandresy.lol.utils.isAdmin

class AskipMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AskipFCM"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val helper = NotificationHelper(applicationContext)

        // On privilégie `data` sur `notification` : c'est le seul des deux qui
        // arrive jusqu'ici quand l'app est au premier plan.
        val title = message.data["title"] ?: message.notification?.title ?: "Askip"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val type = message.data["type"] ?: "message"
        val senderId = message.data["fromUserId"].orEmpty()

        if (body.isBlank()) return

        // L'expéditeur est admin soit parce que le serveur le dit, soit parce
        // que son UID est dans la liste locale.
        val fromAdmin = message.data["fromIsAdmin"]?.toBoolean() == true || isAdmin(senderId)

        when (type) {
            "post", "new_post" -> helper.showPostNotification(title, body)
            "new_post_admin" -> helper.showAdminPostNotification(title, body)
            "mention" -> helper.showMentionNotification(title, body, fromAdmin)
            "mention_everyone" -> helper.showEveryoneMentionNotification(title, body)
            else -> helper.showMessageNotification(title, body, fromAdmin)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Nouveau token FCM")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { Log.e(TAG, "Token non enregistré : ${it.message}") }
    }
}
