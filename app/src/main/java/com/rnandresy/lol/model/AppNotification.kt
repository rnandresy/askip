package com.rnandresy.lol.model

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
    val isRead: Boolean = false,
    val timestamp: Long = 0L,
    val fromIsAdmin: Boolean = false
) {
    fun isFromAdmin() = fromIsAdmin || fromUserId == com.rnandresy.lol.utils.ADMIN_UID

    fun isMandatory() = type == "new_post_admin"
            || type == "mention_everyone"
            || (type == "mention" && isFromAdmin())
            || (type == "message" && isFromAdmin())
}