package com.rnandresy.lol.utils

// ⚠️ Remplace par ton UID Firebase (Authentication → Users)
const val ADMIN_UID        = "ckCTisQMWKWbnoElnUjO2x4vKxy2"
const val ADMIN_BADGE_NAME = "admin"   // nom réservé

val ENI_CLASSES = listOf(
    "IG 1ère année",
    "IG 2ème année",
    "IG 3ème année",
    "GB 1ère année",
    "GB 2ème année",
    "GB 3ème année",
    "SR 1ère année",
    "SR 2ème année",
    "SR 3ème année",
    "GB 4ème année",
    "GB 5ème année",
    "SR 4ère année",
    "SR 5ème année",
    "IG 4ème année",
    "IG 5ème année"
)

val STORY_COLORS = listOf(
    "#7C4DFF", "#E91E63", "#2196F3", "#4CAF50",
    "#FF9800", "#F44336", "#009688", "#9C27B0",
    "#FF5722", "#607D8B", "#00BCD4", "#8BC34A"
)

val STORY_EMOJIS = listOf(
    "💭", "🔥", "😂", "😱", "❤️", "👀", "🎭", "🤫",
    "💀", "🎉", "😈", "🤔", "😤", "💅", "🙃", "🤯"
)

val AVATAR_FRAMES = linkedMapOf(
    "none"    to "Aucun",
    "fire"    to "🔥 Flammes",
    "star"    to "⭐ Étoiles",
    "rainbow" to "🌈 Rainbow",
    "gold"    to "👑 Or"
)

val BADGE_COLORS = listOf(
    "#E91E63", "#9C27B0", "#3F51B5", "#2196F3",
    "#009688", "#4CAF50", "#FF9800", "#FF5722",
    "#F44336", "#FFEB3B", "#7C4DFF", "#607D8B"
)

fun isAdmin(uid: String) = uid == ADMIN_UID