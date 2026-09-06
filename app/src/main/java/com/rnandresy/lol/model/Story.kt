package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude

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
data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    val emoji: String = "star",
    val backgroundColor: String = "#7C4DFF",
    /** Qui l'a déjà ouverte — l'anneau s'éteint une fois vue. */
    val viewedBy: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L
) {
    @Exclude
    fun isActive() = expiresAt > System.currentTimeMillis()

    fun isSeenBy(uid: String) = uid in viewedBy

    /** Heures restantes avant disparition, pour l'afficher en plein écran. */
    @Exclude
    fun hoursLeft(): Int =
        ((expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) / 3_600_000L).toInt()
}
