package com.rnandresy.lol.model

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastTimestamp: Long = 0L,
    val unreadCounts: Map<String, Long> = emptyMap()
)