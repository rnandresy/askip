package com.rnandresy.lol.model

/** Un message de groupe. Mêmes règles que [Message] pour citations et réactions. */
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
