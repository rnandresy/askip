package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable

/**
 * Le droit de réponse.
 *
 * Quand une rumeur te cite nommément, tu obtiens une place **garantie** en tête
 * du post — pas un commentaire noyé dans le fil, où la version de l'accusé
 * arrive toujours trop tard et trop bas.
 *
 * L'id du document est l'UID de la personne : une réponse par personne, et les
 * règles Firestore vérifient que tu ne peux écrire que la tienne.
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
data class MentionReply(
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val postId: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
