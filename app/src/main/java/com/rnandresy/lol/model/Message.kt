package com.rnandresy.lol.model

/**
 * Un message privé.
 *
 * [replyTo*] recopie l'extrait cité plutôt que de le relire : un message
 * supprimé laisse ainsi sa citation intacte, et afficher une réponse ne coûte
 * pas une lecture de plus.
 *
 * [reactions] associe un UID à un emoji — une réaction par personne, comme sur
 * les rumeurs. La forme « une clé par personne » permet à la règle Firestore
 * de vérifier que chacun ne touche que la sienne.
 */
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
    val replyToId: String = "",
    val replyToUsername: String = "",
    val replyToContent: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L
) {
    fun hasMedia() = mediaUrl.isNotBlank()
    fun isAudio()  = mediaType == "audio"
    fun isImage()  = mediaType == "image"
    fun isVideo()  = mediaType == "video"
    fun isFile()   = mediaType == "file"

    fun isReply() = replyToId.isNotBlank()

    /** L'emoji posé par [uid], s'il y en a un. */
    fun myReaction(uid: String): String? = reactions[uid]

    /** Les emojis présents et leur nombre, du plus posé au moins posé. */
    fun reactionCounts(): List<Pair<String, Int>> =
        reactions.values.groupingBy { it }.eachCount()
            .toList()
            .sortedByDescending { it.second }

    /** Ce qu'on cite quand on répond à ce message. */
    fun quote(): String = when {
        content.isNotBlank() -> content
        isImage() -> "📷 Photo"
        isVideo() -> "🎥 Vidéo"
        isAudio() -> "🎤 Message vocal"
        isFile() -> "📎 ${mediaName.ifBlank { "Fichier" }}"
        else -> ""
    }
}
