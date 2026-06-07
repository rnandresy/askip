package com.rnandresy.lol.model

import com.google.firebase.firestore.PropertyName

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    val postType: String = "normal",
    @get:PropertyName("isAnonymous")
    @set:PropertyName("isAnonymous")
    var isAnonymous: Boolean = false,
    // Médias
    val imageUrl: String = "",
    val videoUrl: String = "",
    val audioUrl: String = "",        // note vocale dans le post
    val audioDuration: Int = 0,
    val fileUrl: String = "",
    val fileName: String = "",
    // Sondage
    val pollOption1: String = "",
    val pollOption2: String = "",
    val pollVotes1: Int = 0,
    val pollVotes2: Int = 0,
    val pollVoters: List<String> = emptyList(),
    // Réactions
    val likedBy: List<String> = emptyList(),
    val fireBy: List<String>   = emptyList(),
    val lolBy: List<String>    = emptyList(),
    val shockBy: List<String>  = emptyList(),
    val eyesBy: List<String>   = emptyList(),
    // Meta
    val commentCount: Int = 0,
    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    val timestamp: Long = 0L
) {
    fun getUserReaction(uid: String): String? = when {
        uid in likedBy -> "❤️"
        uid in fireBy  -> "🔥"
        uid in lolBy   -> "😂"
        uid in shockBy -> "😱"
        uid in eyesBy  -> "👀"
        else           -> null
    }

    fun reactionCount(emoji: String) = when (emoji) {
        "❤️" -> likedBy.size
        "🔥" -> fireBy.size
        "😂" -> lolBy.size
        "😱" -> shockBy.size
        "👀" -> eyesBy.size
        else -> 0
    }

    fun hasMedia() = imageUrl.isNotBlank() || videoUrl.isNotBlank()
            || audioUrl.isNotBlank() || fileUrl.isNotBlank()

    companion object {
        fun reactionFieldFor(emoji: String) = when (emoji) {
            "❤️" -> "likedBy"; "🔥" -> "fireBy"; "😂" -> "lolBy"
            "😱" -> "shockBy"; "👀" -> "eyesBy"; else -> "likedBy"
        }
    }
}