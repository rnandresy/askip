package com.rnandresy.lol.model

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
data class MentionReply(
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val postId: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
