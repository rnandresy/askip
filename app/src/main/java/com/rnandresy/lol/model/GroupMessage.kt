package com.rnandresy.lol.model

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
    val timestamp: Long = 0L
) {
    fun hasMedia()  = mediaUrl.isNotBlank()
    fun isImage()   = mediaType == "image"
    fun isVideo()   = mediaType == "video"
    fun isAudio()   = mediaType == "audio"
    fun isFile()    = mediaType == "file"
}