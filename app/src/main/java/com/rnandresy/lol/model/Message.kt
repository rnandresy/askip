package com.rnandresy.lol.model

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val content: String = "",
    //  joindre Média
    val mediaUrl: String = "",        // URL Cloudinary
    val mediaType: String = "",       // "image" | "video" | "audio" | "file"
    val mediaName: String = "",       // nom original (pour fichiers)
    val mediaDuration: Int = 0,       // secondes (audio/video)
    val timestamp: Long = 0L
) {
    fun hasMedia() = mediaUrl.isNotBlank()
    fun isAudio()  = mediaType == "audio"
    fun isImage()  = mediaType == "image"
    fun isVideo()  = mediaType == "video"
    fun isFile()   = mediaType == "file"
}