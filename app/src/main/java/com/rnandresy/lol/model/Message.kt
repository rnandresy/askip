package com.rnandresy.lol.model

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderPhotoUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList()
)