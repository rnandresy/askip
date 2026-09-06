package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude

/**
 * Le contenu d'une capsule scellée.
 *
 * Il vit dans une sous-collection à part, et les règles Firestore refusent la
 * lecture tant que `request.time` n'a pas dépassé [unsealAt] — ou tant que la
 * rumeur n'a pas récolté assez de clés. Le verrou est donc **réel** : cacher le
 * texte à l'affichage n'aurait rien empêché, il suffisait de lire la base.
 *
 * Seul l'auteur peut relire sa capsule avant l'heure.
 */
data class SealedPayload(
    val authorId: String = "",
    val postId: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val unsealAt: Long = 0L,
    val keysNeeded: Int = 0
) {
    @Exclude
    fun isOpen(now: Long = System.currentTimeMillis()) = now >= unsealAt
}
