package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable

/** Un message de groupe. Mêmes règles que [Message] pour citations et réactions. */
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
data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val content: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val mediaName: String = "",
    val mediaDuration: Int = 0,
    val replyToId: String = "",
    val replyToUsername: String = "",
    val replyToContent: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L
) {
    fun hasMedia()  = mediaUrl.isNotBlank()
    fun isImage()   = mediaType == "image"
    fun isVideo()   = mediaType == "video"
    fun isAudio()   = mediaType == "audio"
    fun isFile()    = mediaType == "file"

    fun isReply() = replyToId.isNotBlank()

    fun myReaction(uid: String): String? = reactions[uid]

    fun reactionCounts(): List<Pair<String, Int>> =
        reactions.values.groupingBy { it }.eachCount()
            .toList()
            .sortedByDescending { it.second }

    fun quote(): String = when {
        content.isNotBlank() -> content
        isImage() -> "Photo"
        isVideo() -> "Vidéo"
        isAudio() -> "Message vocal"
        isFile() -> "${mediaName.ifBlank { "Fichier" }}"
        else -> ""
    }
}
