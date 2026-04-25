package com.rnandresy.lol.model

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val age: Int = 0,
    val photoUrl: String = "",
    val relationshipStatus: String = "",
    val classeENI: String = "",
    val bio: String = "",
    val badgeIds: List<String> = emptyList(),
    val fcmToken: String = "",
    @get:PropertyName("hasBadgeENI") @set:PropertyName("hasBadgeENI")
    var hasBadgeENI: Boolean = false,
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false
)