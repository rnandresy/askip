package com.rnandresy.lol.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ── AUTH ──────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: error("UID null")
    }

    fun logout() = auth.signOut()

    suspend fun updateEmail(newEmail: String, password: String) {
        val user = auth.currentUser ?: error("Non connecté")
        val cred = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(cred).await()
        user.verifyBeforeUpdateEmail(newEmail).await()
    }

    suspend fun updatePassword(current: String, newPassword: String) {
        val user = auth.currentUser ?: error("Non connecté")
        val cred = EmailAuthProvider.getCredential(user.email!!, current)
        user.reauthenticate(cred).await()
        user.updatePassword(newPassword).await()
    }

    // ── PROFILES ──────────────────────────────────────────────────────────────

    suspend fun createDefaultProfile(uid: String, username: String) {
        db.collection("profiles").document(uid).set(
            mapOf(
                "userId" to uid,
                "username" to username,
                "age" to 0,
                "photoUrl" to "",
                "relationshipStatus" to "",
                "classeENI" to "",
                "bio" to "",
                "badgeIds" to emptyList<String>(),
                "fcmToken" to "",
                "hasBadgeENI" to false,
                "isAdmin" to false
            )
        ).await()
    }

    fun listenToProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val l = db.collection("profiles").document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(UserProfile::class.java))
            }
        awaitClose { l.remove() }
    }

    suspend fun updateProfile(uid: String, data: Map<String, Any>) {
        db.collection("profiles").document(uid).update(data).await()
    }

    fun listenToAllProfiles(): Flow<List<UserProfile>> = callbackFlow {
        val l = db.collection("profiles")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(UserProfile::class.java) } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        try {
            db.collection("profiles").document(uid).update("fcmToken", token).await()
        } catch (_: Exception) {}
    }

    // ── POSTS ─────────────────────────────────────────────────────────────────

    fun listenToPosts(): Flow<List<Post>> = callbackFlow {
        val l = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val posts = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { l.remove() }
    }

    suspend fun createPost(post: Map<String, Any>) {
        val ref = db.collection("posts").document()
        ref.set(post + mapOf("id" to ref.id)).await()
    }

    suspend fun toggleLike(postId: String, userId: String, liked: Boolean) {
        val ref = db.collection("posts").document(postId)
        if (liked) {
            ref.update("likedBy", FieldValue.arrayRemove(userId), "likes", FieldValue.increment(-1)).await()
        } else {
            ref.update("likedBy", FieldValue.arrayUnion(userId), "likes", FieldValue.increment(1)).await()
        }
    }

    suspend fun togglePin(postId: String, pinned: Boolean) {
        db.collection("posts").document(postId).update("isPinned", !pinned).await()
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
    }

    // ── COMMENTS ──────────────────────────────────────────────────────────────

    fun listenToComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val l = db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Comment::class.java)?.copy(id = it.id) } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun addComment(postId: String, comment: Map<String, Any>) {
        val ref = db.collection("posts").document(postId).collection("comments").document()
        ref.set(comment + mapOf("id" to ref.id)).await()
        db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        db.collection("posts").document(postId).collection("comments").document(commentId).delete().await()
        db.collection("posts").document(postId).update("commentCount", FieldValue.increment(-1)).await()
    }

    // ── CONVERSATIONS ─────────────────────────────────────────────────────────

    fun listenToConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val l = db.collection("conversations")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Conversation::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.lastTimestamp } ?: emptyList()
                trySend(list)
            }
        awaitClose { l.remove() }
    }

    suspend fun startOrGetConversation(
        meId: String, meUsername: String, mePhoto: String,
        otherId: String, otherUsername: String, otherPhoto: String
    ): String {
        val ids = listOf(meId, otherId).sorted()
        val convId = "${ids[0]}_${ids[1]}"
        val ref = db.collection("conversations").document(convId)
        if (!ref.get().await().exists()) {
            ref.set(mapOf(
                "id" to convId,
                "participants" to ids,
                "participantNames" to mapOf(meId to meUsername, otherId to otherUsername),
                "participantPhotos" to mapOf(meId to mePhoto, otherId to otherPhoto),
                "lastMessage" to "",
                "lastSenderId" to "",
                "lastTimestamp" to 0L,
                "unreadCounts" to mapOf(meId to 0L, otherId to 0L)
            )).await()
        }
        return convId
    }

    fun listenToMessages(convId: String): Flow<List<Message>> = callbackFlow {
        val l = db.collection("conversations").document(convId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    suspend fun sendMessage(convId: String, message: Map<String, Any>, receiverId: String) {
        val ref = db.collection("conversations").document(convId).collection("messages").document()
        ref.set(message + mapOf("id" to ref.id)).await()
        db.collection("conversations").document(convId).update(
            "lastMessage", message["content"],
            "lastSenderId", message["senderId"],
            "lastTimestamp", message["timestamp"],
            "unreadCounts.$receiverId", FieldValue.increment(1)
        ).await()
    }

    suspend fun markConversationRead(convId: String, uid: String) {
        try {
            db.collection("conversations").document(convId).update("unreadCounts.$uid", 0L).await()
        } catch (_: Exception) {}
    }

    // ── BADGES ────────────────────────────────────────────────────────────────

    fun listenToAllBadges(): Flow<List<Badge>> = callbackFlow {
        val l = db.collection("badges")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Badge::class.java) } ?: emptyList())
            }
        awaitClose { l.remove() }
    }

    /** Vérifie si un badge avec ce nom (lowercase) existe déjà, retourne son ID ou null */
    suspend fun findBadgeByName(name: String): Badge? {
        val snap = db.collection("badges")
            .whereEqualTo("name", name.trim().lowercase())
            .get().await()
        return snap.documents.firstOrNull()?.toObject(Badge::class.java)
    }

    suspend fun createBadge(badge: Badge, userId: String): String {
        val ref = db.collection("badges").document()
        val b = badge.copy(
            id = ref.id,
            name = badge.displayName.trim().lowercase(),
            displayName = badge.displayName.trim(),
            createdBy = userId,
            createdAt = System.currentTimeMillis()
        )
        ref.set(b).await()
        // L'utilisateur porte ce badge
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
        // Supprimer le badge
        db.collection("badges").document(badgeId).delete().await()
        // Retirer ce badge de TOUS les profils qui le portent
        val profiles = db.collection("profiles")
            .whereArrayContains("badgeIds", badgeId)
            .get().await()
        profiles.documents.forEach { doc ->
            doc.reference.update("badgeIds", FieldValue.arrayRemove(badgeId))
        }
    }

    /** Ajouter un badge existant au profil (le "porter") */
    suspend fun wearBadge(badgeId: String, userId: String) {
        db.collection("profiles").document(userId)
            .update("badgeIds", FieldValue.arrayUnion(badgeId)).await()
    }

    /** Retirer un badge du profil (ne plus le porter, sans supprimer le badge) */
    suspend fun unwearBadge(badgeId: String, userId: String) {
        db.collection("profiles").document(userId)
            .update("badgeIds", FieldValue.arrayRemove(badgeId)).await()
    }
}