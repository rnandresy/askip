package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    /** Note vocale — seule forme de réponse acceptée dans l'actualité vocale. */
    val audioUrl: String = "",
    val audioDuration: Int = 0,
    /** Sous un post anonyme, on commente aussi masqué. */
    @get:PropertyName("isAnonymous")
    @set:PropertyName("isAnonymous")
    var isAnonymous: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val timestamp: Long = 0L
) {
    @Exclude
    fun likeCount() = likedBy.size

    fun isLikedBy(uid: String) = uid in likedBy

    @Exclude
    fun isVoice() = audioUrl.isNotBlank()
}
