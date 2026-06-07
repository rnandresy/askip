package com.rnandresy.lol.model

data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val emoji: String = "💭",
    val backgroundColor: String = "#7C4DFF",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L
) {
    fun isActive() = expiresAt > System.currentTimeMillis()
}