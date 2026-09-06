package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable
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
data class AppNotification(
    val id: String = "",
    val targetUserId: String = "",
    // "message" | "mention" | "mention_everyone" | "new_post_admin" | "new_post"
    val type: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val postId: String = "",
    val conversationId: String = "",
    val content: String = "",
    // Sans ces annotations, Firestore ampute le préfixe `is` du getter et
    // cherche un champ nommé `read` — qui n'existe pas. La notification
    // revenait donc toujours non lue : « Tout lire » écrivait bien sur le
    // serveur, mais chaque relecture repartait de zéro.
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val timestamp: Long = 0L,
    val fromIsAdmin: Boolean = false
) {
    fun isFromAdmin() = fromIsAdmin || fromUserId == com.rnandresy.lol.utils.ADMIN_UID

    fun isMandatory() = type == "new_post_admin"
            || type == "mention_everyone"
            || (type == "mention" && isFromAdmin())
            || (type == "message" && isFromAdmin())
}