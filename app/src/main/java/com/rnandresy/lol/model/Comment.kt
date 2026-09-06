package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

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
