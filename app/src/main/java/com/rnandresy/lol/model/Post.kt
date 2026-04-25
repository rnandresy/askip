package com.rnandresy.lol.model

import com.google.firebase.firestore.PropertyName

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentCount: Int = 0,
    val timestamp: Long = 0L,
    @get:PropertyName("isPinned") @set:PropertyName("isPinned")
    var isPinned: Boolean = false
)