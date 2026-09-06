package com.rnandresy.lol.repository

import com.google.firebase.firestore.DocumentSnapshot
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

    /**
     * Envoie la même notification à tout le monde sauf [excludeUserId].
     *
     * [targetIds] vient de l'appelant, qui tient déjà la liste des profils en
     * mémoire — l'app les écoute en direct. Aller la redemander à Firestore
     * coûtait une lecture par membre du campus, à chaque rumeur publiée, pour
     * des identifiants déjà connus. On ne retombe sur la requête que si le
     * cache est encore vide, au tout premier lancement.
     */
    suspend fun createForAll(
        base: Map<String, Any>,
        excludeUserId: String,
        targetIds: List<String> = emptyList()
    ) {
        runCatching {
            targetIds.ifEmpty { ProfileRepository().getAllUserIds() }
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
                createForAll(
                    base + mapOf("type" to "mention_everyone"),
                    fromUid,
                    profiles.map { it.userId }
                )
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
        senderIsAdmin: Boolean,
        targetIds: List<String> = emptyList()
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
            excludeUserId = fromUid,
            targetIds = targetIds
        )
    }

    // ── Marquage ──────────────────────────────────────────────────────────
    // Ces trois-là laissent remonter leurs erreurs. Le ViewModel applique le
    // changement à l'écran avant d'écrire, et il a besoin de savoir que
    // l'écriture a échoué pour revenir en arrière : un `runCatching` posé ici
    // rendait ce retour en arrière inatteignable, et l'écran affichait un
    // succès que le serveur n'avait jamais accordé.

    suspend fun markRead(notifId: String) {
        notifications.document(notifId).update("isRead", true).await()
    }

    /** Marque comme lues toutes les notifications non lues de [uid]. */
    suspend fun markAllRead(uid: String) {
        val unread = notifications.whereEqualTo("targetUserId", uid).get().await()
            .documents
            // Filtre côté client : Firestore facturerait un index composite
            // pour un `whereNotEqualTo` ici, et la liste est déjà courte.
            .filter { it.getBoolean("isRead") != true }

        if (unread.isEmpty()) return

        val marquer: suspend (List<DocumentSnapshot>) -> Unit = { docs ->
            docs.commitInChunks { batch, doc -> batch.update(doc.reference, "isRead", true) }
        }

        // `update` fait échouer tout le lot si un seul document a disparu entre
        // la lecture et l'écriture — or supprimer une notification est le geste
        // voisin de « Tout lire ». On relit la liste et on retente une fois
        // avant d'abandonner ; si ça échoue encore, l'erreur remonte.
        runCatching { marquer(unread) }.getOrElse {
            val encoreLa = unread.filter { doc ->
                runCatching { doc.reference.get().await().exists() }.getOrDefault(false)
            }
            if (encoreLa.isNotEmpty()) marquer(encoreLa)
        }
    }

    suspend fun delete(notifId: String) {
        notifications.document(notifId).delete().await()
    }
}
