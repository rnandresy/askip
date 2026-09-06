package com.rnandresy.lol.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.rnandresy.lol.model.Bet
import com.rnandresy.lol.model.ChainLink
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.MentionReply
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.SealedPayload
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.utils.COL_BETS
import com.rnandresy.lol.utils.COL_COMMENTS
import com.rnandresy.lol.utils.COL_LINKS
import com.rnandresy.lol.utils.COL_POSTS
import com.rnandresy.lol.utils.COL_REPLIES
import com.rnandresy.lol.utils.COL_REPORTS
import com.rnandresy.lol.utils.COL_SEALED
import com.rnandresy.lol.utils.COL_STORIES
import com.rnandresy.lol.utils.DOC_SEALED
import com.rnandresy.lol.utils.HOT_WINDOW_SIZE
import com.rnandresy.lol.utils.COMMENTS_WINDOW
import com.rnandresy.lol.utils.PAGE_SIZE_FEED
import com.rnandresy.lol.utils.STORY_DURATION_MS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Le fil : rumeurs, confessions, sondages, commentaires, stories.
 *
 * Le classement « ça chauffe » se calcule côté client (voir `RumorEngine`) sur
 * une fenêtre de posts récents. Firestore ne sait pas trier par un score qui
 * décroît avec le temps sans Cloud Function, et pour un campus cette fenêtre
 * suffit largement — c'est instantané et ça ne coûte aucune lecture en plus.
 */
class FeedRepository {

    private val posts get() = db.collection(COL_POSTS)

    // ── Lecture paginée ───────────────────────────────────────────────────────

    /**
     * Une page de rumeurs, de la plus récente à la plus ancienne.
     * [after] est le dernier post déjà affiché — passer null pour la première page.
     */
    suspend fun getPostsPage(limit: Long = PAGE_SIZE_FEED, after: Post? = null): List<Post> {
        var query: Query = posts
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
        if (after != null) query = query.startAfter(after.timestamp)
        return query.get().await().documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * La fenêtre analysée pour « ça chauffe », les tendances et le panthéon.
     * En écoute temps réel : réactions et commentaires arrivent tout seuls.
     */
    fun listenToRecentPosts(limit: Long = HOT_WINDOW_SIZE): Flow<List<Post>> = callbackFlow {
        val reg = posts
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty().mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun getConfessions(limit: Long = PAGE_SIZE_FEED): List<Post> = runCatching {
        posts.whereEqualTo("isAnonymous", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await().documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
    }.getOrElse { emptyList() }

    suspend fun getPostsByUser(uid: String, limit: Long = 20): List<Post> = runCatching {
        posts.whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await().documents
            .mapNotNull { doc -> doc.toObject(Post::class.java)?.copy(id = doc.id) }
            // Les confessions restent anonymes, même sur le profil de leur auteur.
            .filterNot { it.isAnonymous }
    }.getOrElse { emptyList() }

    suspend fun getPostsByTag(tag: String, limit: Long = 40): List<Post> = runCatching {
        posts.whereArrayContains("tags", tag.lowercase())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await().documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
    }.getOrElse { emptyList() }

    suspend fun getPost(postId: String): Post? = runCatching {
        posts.document(postId).get().await().let { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
    }.getOrNull()

    // ── Écriture ──────────────────────────────────────────────────────────────

    suspend fun createPost(data: Map<String, Any>): String {
        val ref = posts.document()
        ref.set(data + mapOf("id" to ref.id)).await()
        return ref.id
    }

    suspend fun editPost(postId: String, newContent: String, tags: List<String>) {
        posts.document(postId).update(
            mapOf(
                "content" to newContent,
                "tags" to tags,
                "isEdited" to true
            )
        ).await()
    }

    suspend fun deletePost(postId: String) {
        // Firestore ne supprime pas les sous-collections avec le document parent :
        // sans ce nettoyage, commentaires, paris et capsules resteraient orphelins.
        val ref = posts.document(postId)
        ref.collection(COL_COMMENTS).deleteAll()
        ref.collection(COL_SEALED).deleteAll()
        ref.collection(COL_BETS).deleteAll()
        ref.collection(COL_LINKS).deleteAll()
        ref.collection(COL_REPLIES).deleteAll()
        ref.delete().await()
    }

    suspend fun setPinned(postId: String, pinned: Boolean) {
        posts.document(postId).update("isPinned", pinned).await()
    }

    // ── Réactions ─────────────────────────────────────────────────────────────

    /**
     * Pose la réaction de [uid] en un seul appel : l'ancienne est retirée et la
     * nouvelle ajoutée dans le même update, sinon un aller-retour raté laisserait
     * la personne avec deux réactions.
     * [emoji] null = on retire simplement la réaction.
     */
    suspend fun setReaction(postId: String, uid: String, emoji: String?, previous: String?) {
        val updates = hashMapOf<String, Any>()
        if (previous != null && previous != emoji) {
            updates[Post.reactionFieldFor(previous)] = FieldValue.arrayRemove(uid)
        }
        if (emoji != null) {
            updates[Post.reactionFieldFor(emoji)] = FieldValue.arrayUnion(uid)
        }
        if (updates.isNotEmpty()) posts.document(postId).update(updates).await()
    }

    // ── Rumeur-mètre ──────────────────────────────────────────────────────────

    /**
     * Tranche sur une rumeur : crédible, bidon, ou retrait du vote.
     * On écrit les deux tableaux d'un coup pour qu'un vote ne puisse jamais
     * compter des deux côtés.
     */
    suspend fun setVerdictVote(postId: String, uid: String, credible: Boolean?) {
        val updates = hashMapOf<String, Any>(
            "credibleBy" to if (credible == true) FieldValue.arrayUnion(uid)
            else FieldValue.arrayRemove(uid),
            "fakeBy" to if (credible == false) FieldValue.arrayUnion(uid)
            else FieldValue.arrayRemove(uid)
        )
        posts.document(postId).update(updates).await()
    }

    /** Offre le Scoop du jour à une rumeur. */
    suspend fun giveScoop(postId: String, uid: String) {
        posts.document(postId).update("scoopBy", FieldValue.arrayUnion(uid)).await()
    }

    // ── Sondage ───────────────────────────────────────────────────────────────

    suspend fun votePoll(postId: String, uid: String, option: Int) {
        val field = if (option == 1) "pollVotes1" else "pollVotes2"
        posts.document(postId).update(
            mapOf(
                "pollVoters" to FieldValue.arrayUnion(uid),
                field to FieldValue.increment(1)
            )
        ).await()
    }

    // ── Signalement ───────────────────────────────────────────────────────────

    suspend fun reportPost(postId: String, reporterId: String, reason: String, details: String) {
        val ref = db.collection(COL_REPORTS).document()
        ref.set(
            mapOf(
                "id" to ref.id,
                "postId" to postId,
                "reporterId" to reporterId,
                "reason" to reason,
                "details" to details,
                "handled" to false,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
    }

    // ── Commentaires ──────────────────────────────────────────────────────────

    fun listenToComments(postId: String, limit: Long = COMMENTS_WINDOW): Flow<List<Comment>> =
        callbackFlow {
            val reg = posts.document(postId).collection(COL_COMMENTS)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit)
                .addSnapshotListener { snap, _ ->
                    trySend(
                        snap?.documents.orEmpty().mapNotNull { doc ->
                            doc.toObject(Comment::class.java)?.copy(id = doc.id)
                        }
                    )
                }
            awaitClose { reg.remove() }
        }

    suspend fun addComment(postId: String, data: Map<String, Any>): String {
        val ref = posts.document(postId).collection(COL_COMMENTS).document()
        ref.set(data + mapOf("id" to ref.id)).await()
        posts.document(postId).update("commentCount", FieldValue.increment(1)).await()
        return ref.id
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        posts.document(postId).collection(COL_COMMENTS).document(commentId).delete().await()
        posts.document(postId).update("commentCount", FieldValue.increment(-1)).await()
    }

    suspend fun toggleCommentLike(postId: String, commentId: String, uid: String, liked: Boolean) {
        posts.document(postId).collection(COL_COMMENTS).document(commentId).update(
            "likedBy",
            if (liked) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        ).await()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  La Capsule scellée
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Range le contenu d'une capsule hors de portée.
     *
     * Le texte ne va **pas** dans le document du post : il vit dans une
     * sous-collection que les règles Firestore refusent de servir avant l'heure.
     * Masquer le texte à l'affichage n'aurait rien protégé — il suffisait
     * d'ouvrir la base pour le lire.
     */
    suspend fun sealContent(
        postId: String,
        authorId: String,
        content: String,
        imageUrl: String,
        unsealAt: Long,
        keysNeeded: Int
    ) {
        posts.document(postId).collection(COL_SEALED).document(DOC_SEALED).set(
            mapOf(
                "authorId" to authorId,
                "postId" to postId,
                "content" to content,
                "imageUrl" to imageUrl,
                "unsealAt" to unsealAt,
                "keysNeeded" to keysNeeded
            )
        ).await()
    }

    /**
     * Tente de lire une capsule.
     * Renvoie null quand le serveur refuse — c'est le cas normal avant l'heure,
     * pas une erreur à remonter à l'utilisateur.
     */
    suspend fun readSealed(postId: String): SealedPayload? = runCatching {
        posts.document(postId).collection(COL_SEALED).document(DOC_SEALED)
            .get().await().toObject(SealedPayload::class.java)
    }.getOrNull()

    /** Donne sa clé à une capsule. Assez de clés et elle s'ouvre pour tout le monde. */
    suspend fun giveKey(postId: String, uid: String) {
        posts.document(postId).update("keysBy", FieldValue.arrayUnion(uid)).await()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Le Téléphone arabe
    // ═════════════════════════════════════════════════════════════════════════

    fun listenToChain(postId: String): Flow<List<ChainLink>> = callbackFlow {
        val reg = posts.document(postId).collection(COL_LINKS)
            .orderBy("index", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty().mapNotNull { doc ->
                        doc.toObject(ChainLink::class.java)?.copy(id = doc.id)
                    }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun getChain(postId: String): List<ChainLink> = runCatching {
        posts.document(postId).collection(COL_LINKS)
            .orderBy("index", Query.Direction.ASCENDING)
            .get().await().documents.mapNotNull { doc ->
                doc.toObject(ChainLink::class.java)?.copy(id = doc.id)
            }
    }.getOrElse { emptyList() }

    /**
     * Ajoute un maillon.
     * L'index vient du compteur porté par le post : deux personnes qui écrivent
     * en même temps peuvent tomber sur le même numéro, mais l'ordre d'affichage
     * retombe alors sur l'horodatage — sans transaction, c'est le compromis
     * acceptable pour une chaîne de sept maillons.
     */
    suspend fun addChainLink(
        postId: String,
        index: Int,
        userId: String,
        username: String,
        userPhotoUrl: String,
        content: String,
        isAnonymous: Boolean
    ) {
        val ref = posts.document(postId).collection(COL_LINKS).document()
        ref.set(
            mapOf(
                "id" to ref.id,
                "postId" to postId,
                "index" to index,
                "userId" to userId,
                "username" to if (isAnonymous) "" else username,
                "userPhotoUrl" to if (isAnonymous) "" else userPhotoUrl,
                "content" to content,
                "isAnonymous" to isAnonymous,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
        posts.document(postId).update(
            mapOf(
                "chainCount" to FieldValue.increment(1),
                "chainAuthors" to FieldValue.arrayUnion(userId),
                "chainLastContent" to content,
                "chainLastAuthor" to if (isAnonymous) "" else username
            )
        ).await()
    }

    /** Tous mes paris en une seule écoute, plutôt qu'un écouteur par rumeur. */
    fun listenToMyBets(uid: String): Flow<Map<String, Bet>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyMap())
            awaitClose { }
            return@callbackFlow
        }
        val reg = db.collectionGroup(COL_BETS)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty()
                        .mapNotNull { it.toObject(Bet::class.java) }
                        .associateBy { it.postId }
                )
            }
        awaitClose { reg.remove() }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Le Pari
    // ═════════════════════════════════════════════════════════════════════════

    fun listenToMyBet(postId: String, uid: String): Flow<Bet?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val reg = posts.document(postId).collection(COL_BETS).document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(Bet::class.java))
            }
        awaitClose { reg.remove() }
    }

    suspend fun getMyBet(postId: String, uid: String): Bet? = runCatching {
        posts.document(postId).collection(COL_BETS).document(uid)
            .get().await().toObject(Bet::class.java)
    }.getOrNull()

    /** Le document porte l'UID comme identifiant : impossible de parier deux fois. */
    suspend fun placeBet(bet: Bet) {
        posts.document(bet.postId).collection(COL_BETS).document(bet.userId).set(
            mapOf(
                "userId" to bet.userId,
                "postId" to bet.postId,
                "onCredible" to bet.onCredible,
                "stake" to bet.stake,
                "ratioAtBet" to bet.ratioAtBet,
                "votesAtBet" to bet.votesAtBet,
                "odds" to bet.odds,
                "settled" to false,
                "won" to false,
                "payout" to 0L,
                "timestamp" to bet.timestamp
            )
        ).await()
    }

    /** Marque un pari comme réglé. Chacun ne règle que le sien. */
    suspend fun settleBet(postId: String, uid: String, won: Boolean, payout: Long) {
        posts.document(postId).collection(COL_BETS).document(uid).update(
            mapOf(
                "settled" to true,
                "won" to won,
                "payout" to payout
            )
        ).await()
    }

    /** Tous les paris d'une rumeur — pour montrer comment le campus s'est positionné. */
    suspend fun getBets(postId: String, limit: Long = 50): List<Bet> = runCatching {
        posts.document(postId).collection(COL_BETS).limit(limit)
            .get().await().documents.mapNotNull { it.toObject(Bet::class.java) }
    }.getOrElse { emptyList() }

    // ═════════════════════════════════════════════════════════════════════════
    //  Le droit de réponse
    // ═════════════════════════════════════════════════════════════════════════

    fun listenToReplies(postId: String): Flow<List<MentionReply>> = callbackFlow {
        val reg = posts.document(postId).collection(COL_REPLIES)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty()
                        .mapNotNull { it.toObject(MentionReply::class.java) }
                        .sortedBy { it.timestamp }
                )
            }
        awaitClose { reg.remove() }
    }

    /** L'id du document est l'UID : chacun n'écrit que sa propre réponse. */
    suspend fun setMentionReply(reply: MentionReply) {
        posts.document(reply.postId).collection(COL_REPLIES).document(reply.userId).set(
            mapOf(
                "userId" to reply.userId,
                "username" to reply.username,
                "userPhotoUrl" to reply.userPhotoUrl,
                "postId" to reply.postId,
                "content" to reply.content,
                "timestamp" to reply.timestamp
            )
        ).await()
        // Marqué sur le post pour que le fil puisse afficher le badge sans
        // ouvrir la sous-collection de chaque carte.
        posts.document(reply.postId)
            .update("repliedBy", FieldValue.arrayUnion(reply.userId)).await()
    }

    suspend fun deleteMentionReply(postId: String, uid: String) {
        posts.document(postId).collection(COL_REPLIES).document(uid).delete().await()
        posts.document(postId).update("repliedBy", FieldValue.arrayRemove(uid)).await()
    }

    // ── Stories ───────────────────────────────────────────────────────────────

    fun listenToActiveStories(): Flow<List<Story>> = callbackFlow {
        val reg = db.collection(COL_STORIES)
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents.orEmpty().mapNotNull { doc ->
                        doc.toObject(Story::class.java)?.copy(id = doc.id)
                    }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun createStory(
        userId: String,
        username: String,
        userPhotoUrl: String,
        content: String,
        emoji: String,
        colorHex: String
    ): String {
        val now = System.currentTimeMillis()
        val ref = db.collection(COL_STORIES).document()
        ref.set(
            mapOf(
                "id" to ref.id,
                "userId" to userId,
                "username" to username,
                "userPhotoUrl" to userPhotoUrl,
                "content" to content,
                "emoji" to emoji,
                "backgroundColor" to colorHex,
                "timestamp" to now,
                "expiresAt" to now + STORY_DURATION_MS
            )
        ).await()
        return ref.id
    }

    suspend fun deleteStory(storyId: String) {
        db.collection(COL_STORIES).document(storyId).delete().await()
    }
}
