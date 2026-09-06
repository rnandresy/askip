package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Un maillon du Téléphone arabe.
 *
 * L'auteur lance une phrase, quelqu'un d'autre en ajoute une, et ainsi de suite
 * jusqu'à sept. Personne ne peut poser deux maillons : la rumeur doit traverser
 * le campus pour grandir, exactement comme une vraie.
 */
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
