package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.rnandresy.lol.utils.levelForXp
import com.rnandresy.lol.utils.levelTitle
import com.rnandresy.lol.utils.xpForLevel

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
    val customBadgeName: String = "",
    val customBadgeColor: String = "#7C4DFF",
    // ── Compteurs d'activité ──────────────────────────────────────────────────
    val postsCount: Int = 0,
    val commentsCount: Int = 0,
    val confessionsCount: Int = 0,
    val storiesCount: Int = 0,
    val pollsCount: Int = 0,
    val convsStarted: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String = "",
    // ── Réputation d'informateur ──────────────────────────────────────────────
    /** Le clout monte quand on croit tes rumeurs, il tombe quand on les démonte. */
    val clout: Long = 0L,
    val xp: Long = 0L,
    val confirmedRumors: Int = 0,
    val debunkedRumors: Int = 0,
    /** Jour (yyyy-MM-dd) où le dernier Scoop a été dépensé. */
    val lastScoopDate: String = "",
    // ── Les paris ─────────────────────────────────────────────────────────────
    /** Jour du dernier pari — sert à recharger les jetons à minuit. */
    val lastBetDate: String = "",
    /** Jetons déjà dépensés aujourd'hui. */
    val betsToday: Int = 0,
    val betsWon: Int = 0,
    val betsLost: Int = 0,
    /** Jeton Firebase Messaging de l'appareil, pour les notifications push. */
    val fcmToken: String = "",
    // ── Statut ────────────────────────────────────────────────────────────────
    @get:PropertyName("hasBadgeENI")
    @set:PropertyName("hasBadgeENI")
    var hasBadgeENI: Boolean = false,
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    @get:PropertyName("isBanned")
    @set:PropertyName("isBanned")
    var isBanned: Boolean = false
) {

    @Exclude
    fun level(): Int = levelForXp(xp)

    @Exclude
    fun title(): String = levelTitle(level())

    /** Progression dans le niveau courant, entre 0 et 1. */
    @Exclude
    fun levelProgress(): Float {
        val lvl = level()
        val floor = xpForLevel(lvl)
        val ceiling = xpForLevel(lvl + 1)
        if (ceiling <= floor) return 1f
        return ((xp - floor).toFloat() / (ceiling - floor)).coerceIn(0f, 1f)
    }

    @Exclude
    fun xpToNextLevel(): Long = (xpForLevel(level() + 1) - xp).coerceAtLeast(0L)

    /** Fiabilité affichée sur le profil : part de rumeurs confirmées. */
    @Exclude
    fun reliability(): Float {
        val total = confirmedRumors + debunkedRumors
        return if (total == 0) 0f else confirmedRumors.toFloat() / total
    }

    /** Réussite aux paris — le vrai indicateur de flair. */
    @Exclude
    fun betAccuracy(): Float {
        val total = betsWon + betsLost
        return if (total == 0) 0f else betsWon.toFloat() / total
    }

    /**
     * Jetons de pari restants aujourd'hui.
     * Le compteur se remet à zéro dès que la date change : aucune tâche
     * planifiée à faire tourner, la date fait tout le travail.
     */
    @Exclude
    fun betTokensLeft(today: String, perDay: Int): Int =
        if (lastBetDate != today) perDay else (perDay - betsToday).coerceAtLeast(0)

    @Exclude
    fun hasCustomBadge() = customBadgeName.isNotBlank()
}
