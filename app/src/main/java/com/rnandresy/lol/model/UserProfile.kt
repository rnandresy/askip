package com.rnandresy.lol.model

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val age: Int = 0,
    val bio: String = "",
    val classeENI: String = "",
    val relationshipStatus: String = "",
    // ── Médias ────────────────────────────────────────────────────────────────
    val photoUrl: String = "",
    val coverUrl: String = "",
    // ── Personnalisation ──────────────────────────────────────────────────────
    val themeColor: String = "#7C4DFF",
    val avatarFrame: String = "none",
    val moodEmoji: String = "",
    val moodText: String = "",
    // ── Badges ────────────────────────────────────────────────────────────────
    val badgeIds: List<String> = emptyList(),
    // ── Compteurs d'activité ──────────────────────────────────────────────────
    val postsCount: Int = 0,
    val commentsCount: Int = 0,
    val confessionsCount: Int = 0,
    val storiesCount: Int = 0,
    val pollsCount: Int = 0,
    val convsStarted: Int = 0,
    val streak: Int = 0,
    val lastActiveDate: String = "",
    @get:PropertyName("hasBadgeENI")
    @set:PropertyName("hasBadgeENI")
    var hasBadgeENI: Boolean = false,
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false
)