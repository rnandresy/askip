package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable
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
/**
 * Ces modèles sont des valeurs : une fois désérialisés par Firestore, plus
 * personne ne les modifie — l'app passe systématiquement par `copy()`. Les
 * quelques `var` ne sont là que pour `@set:PropertyName`, dont Firestore a
 * besoin pour écrire le champ.
 *
 * Sans le dire à Compose, une classe portant un `var` ou une `List` est jugée
 * *instable* : le compilateur ne peut plus sauter une carte dont les données
 * n'ont pas bougé, et le moindre changement d'état ailleurs redessine tout le
 * fil. `@Immutable` est la promesse qui débloque ça — et ici c'en est une
 * vraie.
 */
@Immutable
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
