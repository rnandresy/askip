package com.rnandresy.lol.repository

import com.rnandresy.lol.model.AppNotification
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.utils.COL_NOTIFICATIONS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Notifications in-app.
 *
 * Le fan-out des mentions se fait ici, côté client : pour un campus de quelques
 * centaines de personnes c'est acceptable, et ça évite d'exiger le plan payant
 * qu'imposeraient les Cloud Functions.
 */
class NotificationRepository {

    private val notifications get() = db.collection(COL_NOTIFICATIONS)

    fun listenToNotifications(uid: String, limit: Int = 80): Flow<List<AppNotification>> =
        callbackFlow {
            if (uid.isBlank()) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            val reg = notifications
                .whereEqualTo("targetUserId", uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    trySend(
                        snap?.documents.orEmpty()
                            .mapNotNull { doc ->
                                doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                            }
                            .sortedByDescending { it.timestamp }
                            .take(limit)
                    )
                }
            awaitClose { reg.remove() }
        }

    suspend fun create(data: Map<String, Any>) {
        runCatching {
            val ref = notifications.document()
            ref.set(data + mapOf("id" to ref.id)).await()
        }
    }

    /** Envoie la même notification à tout le monde sauf [excludeUserId]. */
    suspend fun createForAll(base: Map<String, Any>, excludeUserId: String) {
        runCatching {
            ProfileRepository().getAllUserIds()
                .filter { it != excludeUserId }
                .chunked(BATCH_SIZE)
                .forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { uid ->
                        val ref = notifications.document()
                        batch.set(ref, base + mapOf("id" to ref.id, "targetUserId" to uid))
                    }
                    batch.commit().await()
                }
        }
    }

    /**
     * Prévient les personnes mentionnées dans [content].
     * `@everyone` (et ses variantes françaises) prévient tout le campus.
     */
    suspend fun notifyMentions(
        content: String,
        fromUid: String,
        fromName: String,
        postId: String,
        profiles: List<UserProfile>,
        senderIsAdmin: Boolean
    ) {
        val words = content.split(Regex("\\s+"))
        val base = mapOf(
            "fromUserId" to fromUid,
            "fromUsername" to fromName,
            "fromIsAdmin" to senderIsAdmin,
            "postId" to postId,
            "conversationId" to "",
            "content" to content.take(140),
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )

        // `@everyone` reste réservé aux admins : ouvert à tout le monde, une
        // seule personne pourrait réveiller le campus entier à volonté.
        val mentionsEveryone = words.any {
            val w = it.lowercase()
            w == "@everyone" || w == "@tout_le_monde" || w == "@tous"
        }
        if (mentionsEveryone) {
            if (senderIsAdmin) {
                createForAll(base + mapOf("type" to "mention_everyone"), fromUid)
                return
            }
            // Pour les autres, la mention est simplement ignorée — les
            // mentions nominatives du message continuent d'être traitées.
        }

        val handles = words
            .filter { it.startsWith("@") && it.length > 1 }
            .map { it.removePrefix("@").trimEnd('.', ',', '!', '?', ':', ';').lowercase() }
            .filterNot { it in setOf("everyone", "tout_le_monde", "tous") }
            .distinct()
        if (handles.isEmpty()) return

        profiles
            .filter { it.username.lowercase() in handles && it.userId != fromUid }
            .forEach { target ->
                create(base + mapOf("type" to "mention", "targetUserId" to target.userId))
            }
    }

    /** Prévient le campus qu'une nouvelle rumeur est en ligne. */
    suspend fun notifyNewPost(
        fromUid: String,
        fromName: String,
        postId: String,
        preview: String,
        senderIsAdmin: Boolean
    ) {
        createForAll(
            mapOf(
                "type" to if (senderIsAdmin) "new_post_admin" else "new_post",
                "fromUserId" to fromUid,
                "fromUsername" to fromName,
                "fromIsAdmin" to senderIsAdmin,
                "postId" to postId,
                "conversationId" to "",
                "content" to preview.take(140),
                "isRead" to false,
                "timestamp" to System.currentTimeMillis()
            ),
            excludeUserId = fromUid
        )
    }

    suspend fun markRead(notifId: String) {
        runCatching { notifications.document(notifId).update("isRead", true).await() }
    }

    suspend fun markAllRead(uid: String) {
        runCatching {
            notifications.whereEqualTo("targetUserId", uid).get().await()
                .documents
                // Filtre côté client : Firestore facturerait un index composite
                // pour un `whereNotEqualTo` ici, et la liste est déjà courte.
                .filter { it.getBoolean("isRead") != true }
                .commitInChunks { batch, doc -> batch.update(doc.reference, "isRead", true) }
        }
    }

    suspend fun delete(notifId: String) {
        runCatching { notifications.document(notifId).delete().await() }
    }
}
