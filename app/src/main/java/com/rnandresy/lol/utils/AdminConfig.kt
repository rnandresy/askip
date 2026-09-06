package com.rnandresy.lol.utils

/**
 * Administrateurs et listes de personnalisation.
 *
 * ⚠️ [isAdmin] n'est qu'un raccourci d'affichage côté client. La vraie
 * autorisation est appliquée par les règles Firestore (voir `firestore.rules`) :
 * quelqu'un qui modifierait l'APK ne pourrait toujours pas écrire les champs
 * réservés.
 *
 * Pour ajouter un admin : copier son UID depuis la console Firebase
 * (Authentication → Users) et l'ajouter à [ADMIN_UIDS].
 */
const val ADMIN_UID = "ckCTisQMWKWbnoElnUjO2x4vKxy2"

/** Nom de badge réservé — personne ne peut se l'attribuer. */
const val ADMIN_BADGE_NAME = "admin"

private val ADMIN_UIDS = setOf(
    ADMIN_UID
    // "COLLER_ICI_UN_AUTRE_UID_ADMIN",
)

fun isAdmin(uid: String): Boolean = uid.isNotBlank() && uid in ADMIN_UIDS

fun adminCount(): Int = ADMIN_UIDS.size

// ── Classes ENI ───────────────────────────────────────────────────────────────
val ENI_CLASSES = listOf(
    "IG 1ère année", "IG 2ème année", "IG 3ème année",
    "IG 4ème année", "IG 5ème année",
    "GB 1ère année", "GB 2ème année", "GB 3ème année",
    "GB 4ème année", "GB 5ème année",
    "SR 1ère année", "SR 2ème année", "SR 3ème année",
    "SR 4ème année", "SR 5ème année"
)

// ── Cadres d'avatar ───────────────────────────────────────────────────────────
val AVATAR_FRAMES = linkedMapOf(
    "none" to "Aucun",
    "fire" to "🔥 Flammes",
    "star" to "⭐ Étoiles",
    "rainbow" to "🌈 Rainbow",
    "gold" to "👑 Or"
)

// ── Couleurs de badge ─────────────────────────────────────────────────────────
val BADGE_COLORS = listOf(
    "#E91E63", "#9C27B0", "#3F51B5", "#2196F3",
    "#009688", "#4CAF50", "#FF9800", "#FF5722",
    "#F44336", "#FFEB3B", "#7C4DFF", "#607D8B"
)
