package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Un maillon du Téléphone arabe.
 *
 * L'auteur lance une phrase, quelqu'un d'autre en ajoute une, et ainsi de suite
 * jusqu'à sept. Personne ne peut poser deux maillons : la rumeur doit traverser
 * le campus pour grandir, exactement comme une vraie.
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
data class ChainLink(
    val id: String = "",
    val postId: String = "",
    /** Position dans la chaîne, à partir de 1 (le maillon d'origine). */
    val index: Int = 0,
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    @get:PropertyName("isAnonymous")
    @set:PropertyName("isAnonymous")
    var isAnonymous: Boolean = false,
    val timestamp: Long = 0L
) {
    @Exclude
    fun isOrigin() = index <= 1
}
