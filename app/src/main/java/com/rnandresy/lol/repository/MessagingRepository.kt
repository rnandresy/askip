package com.rnandresy.lol.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.GroupMessage
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.utils.COL_CONVERSATIONS
import com.rnandresy.lol.utils.COL_GROUPS
import com.rnandresy.lol.utils.COL_MESSAGES
import com.rnandresy.lol.utils.MESSAGES_WINDOW
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Messages privés et groupes.
 */
class MessagingRepository {

    private val conversations get() = db.collection(COL_CONVERSATIONS)
    private val groups get() = db.collection(COL_GROUPS)

    // ── Conversations ─────────────────────────────────────────────────────────

    fun listenToConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = conversations.whereArrayContains("participants", uid)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty()
                        .mapNotNull { doc ->
                            doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                        }
                        .sortedByDescending { it.lastTimestamp }
                )
            }
        awaitClose { reg.remove() }
    }

    /**
     * L'id d'une conversation est déterministe : les deux UID triés puis collés.
     * Deux personnes ne peuvent donc jamais créer deux fils en double.
     */
    suspend fun getOrCreateConversation(
        meId: String,
        meUsername: String,
        otherId: String,
        otherUsername: String
    ): String {
        val ids = listOf(meId, otherId).sorted()
        val convId = "${ids[0]}_${ids[1]}"
        val ref = conversations.document(convId)
        if (!ref.get().await().exists()) {
            ref.set(
                mapOf(
                    "id" to convId,
                    "participants" to ids,
                    "participantNames" to mapOf(meId to meUsername, otherId to otherUsername),
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "lastTimestamp" to 0L,
                    "unreadCounts" to mapOf(meId to 0L, otherId to 0L)
                )
            ).await()
        }
        return convId
    }

    /**
     * Les messages les plus récents d'abord côté Firestore (pour profiter de la
     * limite), puis remis dans l'ordre de lecture avant d'être envoyés à l'UI.
     */
    fun listenToMessages(convId: String, limit: Long = MESSAGES_WINDOW): Flow<List<Message>> =
        callbackFlow {
            val reg = conversations.document(convId).collection(COL_MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snap, _ ->
                    trySend(
                        snap?.documents.orEmpty()
                            .mapNotNull { doc ->
                                doc.toObject(Message::class.java)?.copy(id = doc.id)
                            }
                            .sortedBy { it.timestamp }
                    )
                }
            awaitClose { reg.remove() }
        }

    suspend fun sendMessage(convId: String, data: Map<String, Any>, receiverId: String) {
        val ref = conversations.document(convId).collection(COL_MESSAGES).document()
        ref.set(data + mapOf("id" to ref.id)).await()
        conversations.document(convId).update(
            mapOf(
                "lastMessage" to previewOf(data),
                "lastSenderId" to (data["senderId"] ?: ""),
                "lastTimestamp" to (data["timestamp"] ?: 0L),
                "unreadCounts.$receiverId" to FieldValue.increment(1)
            )
        ).await()
    }

    suspend fun markRead(convId: String, uid: String) {
        runCatching {
            conversations.document(convId).update("unreadCounts.$uid", 0L).await()
        }
    }

    suspend fun deleteConversation(convId: String) {
        conversations.document(convId).collection(COL_MESSAGES).deleteAll()
        conversations.document(convId).delete().await()
    }

    // ── Groupes ───────────────────────────────────────────────────────────────

    fun listenToGroups(uid: String): Flow<List<Group>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = groups.whereArrayContains("members", uid)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty()
                        .mapNotNull { doc -> doc.toObject(Group::class.java)?.copy(id = doc.id) }
                        .sortedByDescending { it.lastTimestamp }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        emoji: String,
        creatorId: String,
        creatorUsername: String,
        creatorPhoto: String,
        memberIds: List<String>,
        memberNames: Map<String, String>,
        memberPhotos: Map<String, String>
    ): String {
        val ref = groups.document()
        val allMembers = (memberIds + creatorId).distinct()
        ref.set(
            mapOf(
                "id" to ref.id,
                "name" to name,
                "description" to description,
                "emoji" to emoji,
                "createdBy" to creatorId,
                "createdByUsername" to creatorUsername,
                "members" to allMembers,
                "memberNames" to memberNames + mapOf(creatorId to creatorUsername),
                "memberPhotos" to memberPhotos + mapOf(creatorId to creatorPhoto),
                "lastMessage" to "",
                "lastSenderId" to "",
                "lastSenderUsername" to "",
                "lastTimestamp" to 0L,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
        return ref.id
    }

    suspend fun addGroupMember(groupId: String, uid: String, username: String, photoUrl: String) {
        groups.document(groupId).update(
            mapOf(
                "members" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to username,
                "memberPhotos.$uid" to photoUrl
            )
        ).await()
    }

    suspend fun removeGroupMember(groupId: String, uid: String) {
        groups.document(groupId).update("members", FieldValue.arrayRemove(uid)).await()
    }

    suspend fun deleteGroup(groupId: String) {
        groups.document(groupId).collection(COL_MESSAGES).deleteAll()
        groups.document(groupId).delete().await()
    }

    fun listenToGroupMessages(
        groupId: String,
        limit: Long = MESSAGES_WINDOW
    ): Flow<List<GroupMessage>> = callbackFlow {
        val reg = groups.document(groupId).collection(COL_MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty()
                        .mapNotNull { doc ->
                            doc.toObject(GroupMessage::class.java)?.copy(id = doc.id)
                        }
                        .sortedBy { it.timestamp }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendGroupMessage(groupId: String, data: Map<String, Any>) {
        val ref = groups.document(groupId).collection(COL_MESSAGES).document()
        ref.set(data + mapOf("id" to ref.id)).await()
        groups.document(groupId).update(
            mapOf(
                "lastMessage" to previewOf(data),
                "lastSenderId" to (data["senderId"] ?: ""),
                "lastSenderUsername" to (data["senderUsername"] ?: ""),
                "lastTimestamp" to (data["timestamp"] ?: 0L)
            )
        ).await()
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    /** Aperçu affiché dans la liste des conversations. */
    // ── Suppression ───────────────────────────────────────────────────────
    // Les règles l'autorisaient déjà pour son propre message ; il manquait
    // seulement le chemin pour le faire depuis l'app.

    suspend fun deleteMessage(convId: String, messageId: String) {
        conversations.document(convId).collection(COL_MESSAGES).document(messageId)
            .delete().await()
    }

    suspend fun deleteGroupMessage(groupId: String, messageId: String) {
        groups.document(groupId).collection(COL_MESSAGES).document(messageId)
            .delete().await()
    }

    // ── Réactions ─────────────────────────────────────────────────────────
    // Une réaction par personne, rangée sous sa propre clé : la règle
    // Firestore peut ainsi vérifier que chacun ne touche que la sienne.
    // `emoji` nul retire la réaction.

    suspend fun setMessageReaction(
        convId: String,
        messageId: String,
        uid: String,
        emoji: String?
    ) {
        conversations.document(convId).collection(COL_MESSAGES).document(messageId)
            .update("reactions.$uid", emoji ?: FieldValue.delete())
            .await()
    }

    suspend fun setGroupMessageReaction(
        groupId: String,
        messageId: String,
        uid: String,
        emoji: String?
    ) {
        groups.document(groupId).collection(COL_MESSAGES).document(messageId)
            .update("reactions.$uid", emoji ?: FieldValue.delete())
            .await()
    }

    private fun previewOf(data: Map<String, Any>): String {

        val text = (data["content"] as? String).orEmpty()
        if (text.isNotBlank()) return text.take(80)
        return when (data["mediaType"] as? String) {
            "audio" -> "🎤 Message vocal"
            "image" -> "📸 Photo"
            "video" -> "🎥 Vidéo"
            "file" -> "📎 ${data["mediaName"] ?: "Fichier"}"
            else -> ""
        }
    }
}
