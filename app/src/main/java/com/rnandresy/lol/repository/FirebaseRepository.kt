package com.rnandresy.lol.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rnandresy.lol.model.Achievement
import com.rnandresy.lol.model.AppNotification
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    val db           = FirebaseFirestore.getInstance()

    // ── AUTH ──────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String): String {
        return auth.createUserWithEmailAndPassword(email, password).await()
            .user?.uid ?: error("UID null")
    }

    suspend fun logout() = auth.signOut()

    suspend fun updateEmail(newEmail: String, password: String) {
        val user = auth.currentUser ?: error("Non connecté")
        user.reauthenticate(EmailAuthProvider.getCredential(user.email!!, password)).await()
        user.verifyBeforeUpdateEmail(newEmail).await()
    }

    suspend fun updatePassword(current: String, newPwd: String) {
        val user = auth.currentUser ?: error("Non connecté")
        user.reauthenticate(EmailAuthProvider.getCredential(user.email!!, current)).await()
        user.updatePassword(newPwd).await()
    }

    fun currentUserId() = auth.currentUser?.uid ?: ""
    fun currentEmail()  = auth.currentUser?.email ?: ""
    fun isLoggedIn()    = auth.currentUser != null

    // ── PROFILES ──────────────────────────────────────────────────────────────

    suspend fun createDefaultProfile(uid: String, username: String) {
        db.collection("profiles").document(uid).set(mapOf(
            "userId"           to uid,
            "username"         to username,
            "age"              to 0,
            "bio"              to "",
            "classeENI"        to "",
            "relationshipStatus" to "",
            "themeColor"       to "#7C4DFF",
            "avatarFrame"      to "none",
            "moodEmoji"        to "",
            "moodText"         to "",
            "badgeIds"         to emptyList<String>(),
            "postsCount"       to 0,
            "commentsCount"    to 0,
            "confessionsCount" to 0,
            "storiesCount"     to 0,
            "pollsCount"       to 0,
            "convsStarted"     to 0,
            "streak"           to 0,
            "lastActiveDate"   to "",
            "hasBadgeENI"      to false,
            "isAdmin"          to false
            // ❌ totalReactionsReceived supprimé
        )).await()
    }

    fun listenToProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val l = db.collection("profiles").document(uid)
            .addSnapshotListener { s, _ -> trySend(s?.toObject(UserProfile::class.java)) }
        awaitClose { l.remove() }
    }

    suspend fun getProfile(uid: String): UserProfile? = runCatching {
        db.collection("profiles").document(uid).get().await()
            .toObject(UserProfile::class.java)
    }.getOrNull()

    fun listenToAllProfiles(): Flow<List<UserProfile>> = callbackFlow {
        val l = db.collection("profiles")
            .addSnapshotListener { s, _ ->
                trySend(
                    s?.documents?.mapNotNull { it.toObject(UserProfile::class.java) }
                        ?: emptyList()
                )
            }
        awaitClose { l.remove() }
    }

    suspend fun getAllUserIds(): List<String> = runCatching {
        db.collection("profiles").get().await().documents.map { it.id }
    }.getOrElse { emptyList() }

    suspend fun findProfileByUsername(username: String): UserProfile? = runCatching {
        db.collection("profiles")
            .whereEqualTo("username", username)
            .get().await()
            .documents.firstOrNull()?.toObject(UserProfile::class.java)
    }.getOrNull()

    suspend fun updateProfile(uid: String, data: Map<String, Any?>) {
        val clean = data.filterValues { it != null } as Map<String, Any>
        if (clean.isNotEmpty())
            db.collection("profiles").document(uid).update(clean).await()
    }

    suspend fun incrementCounter(uid: String, field: String, by: Long = 1L) {
        runCatching {
            db.collection("profiles").document(uid)
                .update(field, FieldValue.increment(by)).await()
        }
    }

    suspend fun updateStreak(uid: String) {
        val profile = getProfile(uid) ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (profile.lastActiveDate == today) return
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - 86_400_000L))
        val newStreak = if (profile.lastActiveDate == yesterday) profile.streak + 1 else 1
        db.collection("profiles").document(uid)
            .update("streak", newStreak, "lastActiveDate", today).await()
    }

    // ── SYNCHRONISATION DU PSEUDO ─────────────────────────────────────────────

    suspend fun syncUsername(userId: String, newUsername: String) {
        runCatching {
            // 1. Posts (non anonymes)
            val posts = db.collection("posts")
                .whereEqualTo("userId", userId).get().await()
            posts.documents.filter {
                it.getBoolean("isAnonymous") != true
            }.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "username", newUsername) }
                batch.commit().await()
            }

            // 2. Commentaires (collection group)
            val comments = db.collectionGroup("comments")
                .whereEqualTo("userId", userId).get().await()
            comments.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "username", newUsername) }
                batch.commit().await()
            }

            // 3. Stories
            val stories = db.collection("stories")
                .whereEqualTo("userId", userId).get().await()
            stories.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "username", newUsername) }
                batch.commit().await()
            }

            // 4. Conversations (participantNames map)
            val convs = db.collection("conversations")
                .whereArrayContains("participants", userId).get().await()
            convs.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, "participantNames.$userId", newUsername)
                }
                batch.commit().await()
            }

            // 5. Messages (collection group)
            val messages = db.collectionGroup("messages")
                .whereEqualTo("senderId", userId).get().await()
            messages.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "senderUsername", newUsername) }
                batch.commit().await()
            }

            // 6. Notifications envoyées par cet user
            val notifsSent = db.collection("notifications")
                .whereEqualTo("fromUserId", userId).get().await()
            notifsSent.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "fromUsername", newUsername) }
                batch.commit().await()
            }
        }
    }

    // ── ACHIEVEMENTS ──────────────────────────────────────────────────────────

    suspend fun getAchievements(uid: String): List<Achievement> = runCatching {
        db.collection("profiles").document(uid).collection("achievements")
            .get().await().documents
            .mapNotNull { it.toObject(Achievement::class.java) }
    }.getOrElse { emptyList() }

    suspend fun unlockAchievement(uid: String, id: String) {
        val ref = db.collection("profiles").document(uid)
            .collection("achievements").document(id)
        if (!ref.get().await().exists()) {
            ref.set(mapOf("id" to id, "unlockedAt" to System.currentTimeMillis())).await()
        }
    }

    // ── BADGES ────────────────────────────────────────────────────────────────

    fun listenToAllBadges(): Flow<List<Badge>> = callbackFlow {
        val l = db.collection("badges").addSnapshotListener { s, _ ->
            trySend(s?.documents?.mapNotNull { it.toObject(Badge::class.java) } ?: emptyList())
        }
        awaitClose { l.remove() }
    }

    suspend fun findBadgeByName(name: String): Badge? = runCatching {
        db.collection("badges")
            .whereEqualTo("name", name.trim().lowercase())
            .get().await().documents.firstOrNull()?.toObject(Badge::class.java)
    }.getOrNull()

    suspend fun createBadge(displayName: String, colorHex: String, userId: String): String {
        val ref = db.collection("badges").document()
        ref.set(mapOf(
            "id" to ref.id, "name" to displayName.trim().lowercase(),
            "displayName" to displayName.trim(), "colorHex" to colorHex,
            "createdBy" to userId, "createdAt" to System.currentTimeMillis()
        )).await()
        db.collection("profiles").document(userId)
            .update("badgeIds", FieldValue.arrayUnion(ref.id)).await()
        return ref.id
    }

    suspend fun updateBadge(badgeId: String, displayName: String, colorHex: String) {
        db.collection("badges").document(badgeId).update(
            "displayName", displayName.trim(),
            "name", displayName.trim().lowercase(),
            "colorHex", colorHex
        ).await()
    }

    suspend fun deleteBadge(badgeId: String) {
        val profiles = db.collection("profiles")
            .whereArrayContains("badgeIds", badgeId).get().await()
        profiles.documents.forEach {
            it.reference.update("badgeIds", FieldValue.arrayRemove(badgeId))
        }
        db.collection("badges").document(badgeId).delete().await()
    }

    suspend fun wearBadge(badgeId: String, userId: String) {
        db.collection("profiles").document(userId)
            .update("badgeIds", FieldValue.arrayUnion(badgeId)).await()
    }

    suspend fun unwearBadge(badgeId: String, userId: String) {
        db.collection("profiles").document(userId)
            .update("badgeIds", FieldValue.arrayRemove(badgeId)).await()
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────

    fun listenToNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val l = db.collection("notifications")
            .whereEqualTo("targetUserId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                    }
                    ?.sortedByDescending { it.timestamp }
                    ?.take(80)
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { l.remove() }
    }

    suspend fun createNotification(data: Map<String, Any>) {
        runCatching {
            val ref = db.collection("notifications").document()
            ref.set(data + mapOf("id" to ref.id)).await()
        }
    }

    suspend fun createNotificationsForAll(base: Map<String, Any>, excludeUserId: String) {
        runCatching {
            val ids = getAllUserIds().filter { it != excludeUserId }
            ids.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { uid ->
                    val ref = db.collection("notifications").document()
                    batch.set(ref, base + mapOf("id" to ref.id, "targetUserId" to uid))
                }
                batch.commit().await()
            }
        }
    }

    suspend fun markNotificationRead(notifId: String) {
        runCatching {
            db.collection("notifications")
                .document(notifId)
                .update("isRead", true)
                .await()
        }
    }

    suspend fun markAllNotificationsRead(uid: String) {
        runCatching {
            // 1 seule condition whereEqualTo → pas d'index composite requis
            val snap = db.collection("notifications")
                .whereEqualTo("targetUserId", uid)
                .get()
                .await()

            // Filtre côté client pour ne prendre que les non lus
            val unreadDocs = snap.documents.filter {
                it.getBoolean("isRead") == false
            }
            if (unreadDocs.isEmpty()) return@runCatching

            // Batch updates (max 400 par batch)
            unreadDocs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "isRead", true) }
                batch.commit().await()
            }
        }
    }

    suspend fun deleteNotification(notifId: String) {
        runCatching {
            db.collection("notifications").document(notifId).delete().await()
        }
    }

    // ── POSTS ─────────────────────────────────────────────────────────────────

    fun listenToPosts(): Flow<List<Post>> = callbackFlow {
        val l = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.mapNotNull {
                    it.toObject(Post::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun createPost(data: Map<String, Any>): String {
        val ref = db.collection("posts").document()
        ref.set(data + mapOf("id" to ref.id)).await()
        return ref.id
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun togglePin(postId: String, pinned: Boolean) {
        db.collection("posts").document(postId).update("isPinned", !pinned).await()
    }

    suspend fun addReaction(
        postId: String, uid: String, emoji: String,
        oldEmoji: String?, postOwnerId: String
    ) {
        val ref   = db.collection("posts").document(postId)
        val batch = db.batch()
        if (oldEmoji != null)
            batch.update(ref, Post.reactionFieldFor(oldEmoji), FieldValue.arrayRemove(uid))
        batch.update(ref, Post.reactionFieldFor(emoji), FieldValue.arrayUnion(uid))
        batch.commit().await()
    }

    suspend fun removeReaction(postId: String, uid: String, emoji: String) {
        db.collection("posts").document(postId)
            .update(Post.reactionFieldFor(emoji), FieldValue.arrayRemove(uid)).await()
    }

    suspend fun votePoll(postId: String, uid: String, option: Int) {
        val field = if (option == 1) "pollVotes1" else "pollVotes2"
        db.collection("posts").document(postId).update(
            "pollVoters", FieldValue.arrayUnion(uid),
            field, FieldValue.increment(1)
        ).await()
    }

    // ── COMMENTS ──────────────────────────────────────────────────────────────

    fun listenToComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val l = db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.mapNotNull {
                    it.toObject(Comment::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun addComment(postId: String, data: Map<String, Any>) {
        val ref = db.collection("posts").document(postId).collection("comments").document()
        ref.set(data + mapOf("id" to ref.id)).await()
        db.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        db.collection("posts").document(postId)
            .collection("comments").document(commentId).delete().await()
        db.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(-1)).await()
    }

    // ── STORIES ───────────────────────────────────────────────────────────────

    fun listenToStories(): Flow<List<Story>> = callbackFlow {
        val l = db.collection("stories")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.mapNotNull {
                    it.toObject(Story::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun createStory(data: Map<String, Any>) {
        val ref = db.collection("stories").document()
        ref.set(data + mapOf("id" to ref.id)).await()
    }

    suspend fun deleteStory(storyId: String) {
        db.collection("stories").document(storyId).delete().await()
    }

    // ── CONVERSATIONS ─────────────────────────────────────────────────────────

    fun listenToConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val l = db.collection("conversations")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { s, _ ->
                trySend(
                    (s?.documents?.mapNotNull {
                        it.toObject(Conversation::class.java)?.copy(id = it.id)
                    } ?: emptyList()).sortedByDescending { it.lastTimestamp }
                )
            }
        awaitClose { l.remove() }
    }

    suspend fun getOrCreateConversation(
        meId: String, meUsername: String,
        otherId: String, otherUsername: String
    ): String {
        val ids   = listOf(meId, otherId).sorted()
        val convId = "${ids[0]}_${ids[1]}"
        val ref   = db.collection("conversations").document(convId)
        if (!ref.get().await().exists()) {
            ref.set(mapOf(
                "id"               to convId,
                "participants"     to ids,
                "participantNames" to mapOf(meId to meUsername, otherId to otherUsername),
                "lastMessage"      to "",
                "lastSenderId"     to "",
                "lastTimestamp"    to 0L,
                "unreadCounts"     to mapOf(meId to 0L, otherId to 0L)
            )).await()
        }
        return convId
    }

    fun listenToMessages(convId: String): Flow<List<Message>> = callbackFlow {
        val l = db.collection("conversations").document(convId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.mapNotNull {
                    it.toObject(Message::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun sendMessage(convId: String, data: Map<String, Any>, receiverId: String) {
        val ref = db.collection("conversations").document(convId)
            .collection("messages").document()
        ref.set(data + mapOf("id" to ref.id)).await()
        db.collection("conversations").document(convId).update(
            "lastMessage",              data["content"] ?: "",
            "lastSenderId",             data["senderId"] ?: "",
            "lastTimestamp",            data["timestamp"] ?: 0L,
            "unreadCounts.$receiverId", FieldValue.increment(1)
        ).await()
    }

    suspend fun markRead(convId: String, uid: String) {
        runCatching {
            db.collection("conversations").document(convId)
                .update("unreadCounts.$uid", 0L).await()
        }
    }
}