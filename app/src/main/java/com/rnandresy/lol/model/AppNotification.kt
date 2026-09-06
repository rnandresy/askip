package com.rnandresy.lol.model

import com.google.firebase.firestore.PropertyName

data class AppNotification(
    val id: String = "",
    val targetUserId: String = "",
    // "message" | "mention" | "mention_everyone" | "new_post_admin" | "new_post"
    val type: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val postId: String = "",
    val conversationId: String = "",
    val content: String = "",
    // Sans ces annotations, Firestore ampute le préfixe `is` du getter et
    // cherche un champ nommé `read` — qui n'existe pas. La notification
    // revenait donc toujours non lue : « Tout lire » écrivait bien sur le
    // serveur, mais chaque relecture repartait de zéro.
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val timestamp: Long = 0L,
    val fromIsAdmin: Boolean = false
) {
    fun isFromAdmin() = fromIsAdmin || fromUserId == com.rnandresy.lol.utils.ADMIN_UID

    fun isMandatory() = type == "new_post_admin"
            || type == "mention_everyone"
            || (type == "mention" && isFromAdmin())
            || (type == "message" && isFromAdmin())
}