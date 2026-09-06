package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude

data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    val emoji: String = "star",
    val backgroundColor: String = "#7C4DFF",
    /** Qui l'a déjà ouverte — l'anneau s'éteint une fois vue. */
    val viewedBy: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L
) {
    @Exclude
    fun isActive() = expiresAt > System.currentTimeMillis()

    fun isSeenBy(uid: String) = uid in viewedBy

    /** Heures restantes avant disparition, pour l'afficher en plein écran. */
    @Exclude
    fun hoursLeft(): Int =
        ((expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) / 3_600_000L).toInt()
}
