package com.rnandresy.lol.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val emoji: String = "👥",
    val createdBy: String = "",
    val createdByUsername: String = "",
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val memberPhotos: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastSenderUsername: String = "",
    val lastTimestamp: Long = 0L,
    val timestamp: Long = 0L
)