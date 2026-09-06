package com.rnandresy.lol.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.rnandresy.lol.model.Achievement
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.utils.BETS_PER_DAY
import com.rnandresy.lol.utils.COL_ACHIEVEMENTS
import com.rnandresy.lol.utils.COL_BADGES
import com.rnandresy.lol.utils.COL_PROFILES
import com.rnandresy.lol.utils.MAX_PROFILES_CACHED
import com.rnandresy.lol.utils.RumorEngine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Profils, badges, succès, réputation.
 */
class ProfileRepository {

    private val profiles get() = db.collection(COL_PROFILES)

    // ── Lecture ───────────────────────────────────────────────────────────────

    suspend fun createDefaultProfile(uid: String, username: String) {
        profiles.document(uid).set(
            mapOf(
                "userId" to uid, "username" to username, "age" to 0, "bio" to "",
                "classeENI" to "", "relationshipStatus" to "",
                "photoUrl" to "", "coverUrl" to "",
                "themeColor" to "#7C4DFF", "avatarFrame" to "none",
                "moodEmoji" to "", "moodText" to "",
                "badgeIds" to emptyList<String>(),
                "customBadgeName" to "", "customBadgeColor" to "#7C4DFF",
                "postsCount" to 0, "commentsCount" to 0, "confessionsCount" to 0,
                "storiesCount" to 0, "pollsCount" to 0, "convsStarted" to 0,
                "streak" to 0, "bestStreak" to 0, "lastActiveDate" to "",
                "clout" to 0L, "xp" to 0L,
                "confirmedRumors" to 0, "debunkedRumors" to 0, "lastScoopDate" to "",
                "lastBetDate" to "", "betsToday" to 0, "betsWon" to 0, "betsLost" to 0,
                // Jeton push et préférences lues par la Cloud Function d'envoi.
                "fcmToken" to "",
                "notifyMessages" to true, "notifyMentions" to true, "notifyPosts" to true,
                "hasBadgeENI" to false, "isAdmin" to false, "isBanned" to false
            )
        ).await()
    }

    fun listenToProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val reg = profiles.document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(UserProfile::class.java))
            }
        awaitClose { reg.remove() }
    }

    suspend fun getProfile(uid: String): UserProfile? = runCatching {
        profiles.document(uid).get().await().toObject(UserProfile::class.java)
    }.getOrNull()

    /**
     * Les profils du campus. Les « fantômes » (pseudo vide) et les comptes
     * bannis sont écartés — ils polluaient la liste des membres et les mentions.
     */
    fun listenToAllProfiles(limit: Long = MAX_PROFILES_CACHED): Flow<List<UserProfile>> =
        callbackFlow {
            val reg = profiles.limit(limit).addSnapshotListener { snap, _ ->
                val valid = snap?.documents.orEmpty().mapNotNull { doc ->
                    if (!doc.exists()) return@mapNotNull null
                    doc.toObject(UserProfile::class.java)
                        ?.takeIf { it.username.isNotBlank() && !it.isBanned }
                }
                trySend(valid)
            }
            awaitClose { reg.remove() }
        }

    suspend fun getAllUserIds(): List<String> = runCatching {
        profiles.whereNotEqualTo("username", "").get().await().documents.map { it.id }
    }.getOrElse { emptyList() }

    suspend fun findProfileByUsername(username: String): UserProfile? = runCatching {
        profiles.whereEqualTo("username", username).get().await()
            .documents.firstOrNull()?.toObject(UserProfile::class.java)
    }.getOrNull()

    suspend fun usernameExists(username: String): Boolean =
        findProfileByUsername(username.trim()) != null

    // ── Écriture ──────────────────────────────────────────────────────────────

    suspend fun updateProfile(uid: String, data: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val clean = data.filterValues { it != null } as Map<String, Any>
        if (clean.isNotEmpty()) profiles.document(uid).update(clean).await()
    }

    suspend fun incrementCounter(uid: String, field: String, by: Long = 1L) {
        runCatching {
            profiles.document(uid).update(field, FieldValue.increment(by)).await()
        }
    }

    /** Ajoute de l'XP. Le niveau se recalcule à l'affichage — rien à stocker. */
    suspend fun addXp(uid: String, amount: Long) {
        if (uid.isBlank() || amount == 0L) return
        incrementCounter(uid, "xp", amount)
    }

    /**
     * Fait bouger le clout de l'auteur d'une rumeur.
     * Le clout peut descendre : une rumeur démontée coûte de la réputation,
     * sinon il n'y aurait aucun risque à raconter n'importe quoi.
     */
    suspend fun addClout(uid: String, amount: Long) {
        if (uid.isBlank() || amount == 0L) return
        incrementCounter(uid, "clout", amount)
    }

    /**
     * Met à jour la série de jours actifs.
     * Renvoie true si c'est la première visite de la journée — l'appelant peut
     * alors offrir le bonus quotidien.
     */
    suspend fun updateStreak(uid: String): Boolean {
        val profile = getProfile(uid) ?: return false
        val today = RumorEngine.today()
        if (profile.lastActiveDate == today) return false

        val newStreak =
            if (profile.lastActiveDate == RumorEngine.yesterday()) profile.streak + 1 else 1
        val best = maxOf(profile.bestStreak, newStreak)

        runCatching {
            profiles.document(uid).update(
                mapOf(
                    "streak" to newStreak,
                    "bestStreak" to best,
                    "lastActiveDate" to today
                )
            ).await()
        }
        return true
    }

    /** Consomme le Scoop du jour. false = déjà dépensé. */
    suspend fun consumeScoop(uid: String): Boolean {
        val profile = getProfile(uid) ?: return false
        if (!RumorEngine.canScoop(profile.lastScoopDate)) return false
        runCatching {
            profiles.document(uid).update("lastScoopDate", RumorEngine.today()).await()
        }
        return true
    }

    /**
     * Dépense [stake] jetons de pari. Renvoie false s'il n'en reste pas assez.
     *
     * Le compteur se réinitialise dès que la date stockée n'est plus celle du
     * jour : pas de tâche planifiée à maintenir, la date suffit.
     */
    suspend fun consumeBetTokens(uid: String, stake: Int): Boolean {
        val profile = getProfile(uid) ?: return false
        val today = RumorEngine.today()
        val used = if (profile.lastBetDate == today) profile.betsToday else 0
        if (used + stake > BETS_PER_DAY) return false
        return runCatching {
            profiles.document(uid).update(
                mapOf(
                    "lastBetDate" to today,
                    "betsToday" to used + stake
                )
            ).await()
            true
        }.getOrDefault(false)
    }

    /** Enregistre l'issue d'un pari : clout, compteurs, et c'est tout. */
    suspend fun recordBetResult(uid: String, won: Boolean, payout: Long) {
        runCatching {
            profiles.document(uid).update(
                mapOf(
                    "clout" to FieldValue.increment(payout),
                    (if (won) "betsWon" else "betsLost") to FieldValue.increment(1)
                )
            ).await()
        }
    }

    // ── Classements ───────────────────────────────────────────────────────────

    /** Les meilleurs informateurs, par clout. */
    suspend fun topByClout(limit: Long = 25): List<UserProfile> = runCatching {
        profiles.orderBy("clout", Query.Direction.DESCENDING).limit(limit)
            .get().await().documents
            .mapNotNull { it.toObject(UserProfile::class.java) }
            .filter { it.username.isNotBlank() && !it.isBanned }
    }.getOrElse { emptyList() }

    suspend fun topByXp(limit: Long = 25): List<UserProfile> = runCatching {
        profiles.orderBy("xp", Query.Direction.DESCENDING).limit(limit)
            .get().await().documents
            .mapNotNull { it.toObject(UserProfile::class.java) }
            .filter { it.username.isNotBlank() && !it.isBanned }
    }.getOrElse { emptyList() }

    suspend fun topByStreak(limit: Long = 25): List<UserProfile> = runCatching {
        profiles.orderBy("streak", Query.Direction.DESCENDING).limit(limit)
            .get().await().documents
            .mapNotNull { it.toObject(UserProfile::class.java) }
            .filter { it.username.isNotBlank() && !it.isBanned }
    }.getOrElse { emptyList() }

    // ── Succès ────────────────────────────────────────────────────────────────

    suspend fun getAchievements(uid: String): List<Achievement> = runCatching {
        profiles.document(uid).collection(COL_ACHIEVEMENTS).get().await()
            .documents.mapNotNull { it.toObject(Achievement::class.java) }
    }.getOrElse { emptyList() }

    /** Débloque un succès. Ne fait rien s'il l'était déjà. */
    suspend fun unlockAchievement(uid: String, id: String) {
        val ref = profiles.document(uid).collection(COL_ACHIEVEMENTS).document(id)
        if (!ref.get().await().exists()) {
            ref.set(mapOf("id" to id, "unlockedAt" to System.currentTimeMillis())).await()
        }
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    fun listenToAllBadges(): Flow<List<Badge>> = callbackFlow {
        val reg = db.collection(COL_BADGES).addSnapshotListener { snap, _ ->
            trySend(snap?.documents.orEmpty().mapNotNull { it.toObject(Badge::class.java) })
        }
        awaitClose { reg.remove() }
    }

    suspend fun findBadgeByName(name: String): Badge? = runCatching {
        db.collection(COL_BADGES).whereEqualTo("name", name.trim().lowercase())
            .get().await().documents.firstOrNull()?.toObject(Badge::class.java)
    }.getOrNull()

    suspend fun createBadge(displayName: String, colorHex: String, userId: String): String {
        val ref = db.collection(COL_BADGES).document()
        ref.set(
            mapOf(
                "id" to ref.id, "name" to displayName.trim().lowercase(),
                "displayName" to displayName.trim(), "colorHex" to colorHex,
                "createdBy" to userId, "createdAt" to System.currentTimeMillis()
            )
        ).await()
        profiles.document(userId).update("badgeIds", FieldValue.arrayUnion(ref.id)).await()
        return ref.id
    }

    suspend fun updateBadge(badgeId: String, displayName: String, colorHex: String) {
        db.collection(COL_BADGES).document(badgeId).update(
            mapOf(
                "displayName" to displayName.trim(),
                "name" to displayName.trim().lowercase(),
                "colorHex" to colorHex
            )
        ).await()
    }

    suspend fun deleteBadge(badgeId: String) {
        profiles.whereArrayContains("badgeIds", badgeId).get().await()
            .documents.commitInChunks { batch, doc ->
                batch.update(doc.reference, "badgeIds", FieldValue.arrayRemove(badgeId))
            }
        db.collection(COL_BADGES).document(badgeId).delete().await()
    }

    suspend fun wearBadge(badgeId: String, userId: String) {
        profiles.document(userId).update("badgeIds", FieldValue.arrayUnion(badgeId)).await()
    }

    suspend fun unwearBadge(badgeId: String, userId: String) {
        profiles.document(userId).update("badgeIds", FieldValue.arrayRemove(badgeId)).await()
    }

    // ── Changement de pseudo ──────────────────────────────────────────────────

    /**
     * Répercute un nouveau pseudo sur tout le contenu déjà publié.
     * Les posts anonymes sont volontairement épargnés : y écrire le pseudo
     * casserait l'anonymat.
     */
    suspend fun syncUsername(userId: String, newUsername: String) {
        runCatching {
            suspend fun rename(docs: List<DocumentSnapshot>, field: String) {
                docs.commitInChunks { batch, doc ->
                    batch.update(doc.reference, field, newUsername)
                }
            }

            rename(
                db.collection("posts").whereEqualTo("userId", userId).get().await()
                    .documents.filter { it.getBoolean("isAnonymous") != true },
                "username"
            )
            rename(
                db.collectionGroup("comments").whereEqualTo("userId", userId).get().await()
                    .documents.filter { it.getBoolean("isAnonymous") != true },
                "username"
            )
            rename(
                db.collection("stories").whereEqualTo("userId", userId).get().await().documents,
                "username"
            )
            rename(
                db.collectionGroup("messages").whereEqualTo("senderId", userId).get().await().documents,
                "senderUsername"
            )
            rename(
                db.collection("notifications").whereEqualTo("fromUserId", userId).get().await().documents,
                "fromUsername"
            )
            db.collection("conversations").whereArrayContains("participants", userId)
                .get().await().documents.commitInChunks { batch, doc ->
                    batch.update(doc.reference, "participantNames.$userId", newUsername)
                }
            db.collection("groups").whereArrayContains("members", userId)
                .get().await().documents.commitInChunks { batch, doc ->
                    batch.update(doc.reference, "memberNames.$userId", newUsername)
                }
        }
    }

    // ── Suppression de compte ─────────────────────────────────────────────────

    /**
     * Efface tout le contenu de l'utilisateur.
     * À appeler **avant** [AuthRepository.deleteAccount] : une fois déconnecté,
     * les règles Firestore bloquent ces écritures.
     */
    /**
     * Efface tout ce que [uid] a laissé, puis son profil.
     *
     * Chaque étape est isolée et renvoie son nom en cas d'échec, au lieu de
     * faire tomber les suivantes. C'est délibéré : les deux requêtes
     * `collectionGroup` échouent tant que leurs index ne sont pas déployés, et
     * une seule d'entre elles suffisait à interrompre la suppression après
     * l'effacement des rumeurs — la personne se retrouvait alors sans son
     * contenu et avec son compte toujours actif, soit le pire des deux mondes.
     *
     * Renvoie la liste des étapes qui n'ont pas abouti, vide si tout a marché.
     */
    suspend fun deleteAllUserContent(uid: String): List<String> {
        val echecs = mutableListOf<String>()

        suspend fun etape(nom: String, bloc: suspend () -> Unit) {
            runCatching { bloc() }.onFailure { echecs += nom }
        }

        etape("rumeurs") {
            val posts = db.collection("posts").whereEqualTo("userId", uid).get().await()
            posts.documents.forEach { it.reference.collection("comments").deleteAll() }
            posts.documents.commitInChunks { batch, doc -> batch.delete(doc.reference) }
        }

        etape("commentaires") {
            db.collectionGroup("comments").whereEqualTo("userId", uid).deleteAll()
        }

        etape("stories") {
            db.collection("stories").whereEqualTo("userId", uid).deleteAll()
        }

        etape("notifications") {
            db.collection("notifications").whereEqualTo("fromUserId", uid).deleteAll()
            db.collection("notifications").whereEqualTo("targetUserId", uid).deleteAll()
        }

        etape("badges") {
            db.collection(COL_BADGES).whereEqualTo("createdBy", uid).get().await()
                .documents.forEach { deleteBadge(it.id) }
        }

        etape("groupes") {
            db.collection("groups").whereArrayContains("members", uid).get().await()
                .documents.commitInChunks { batch, doc ->
                    batch.update(doc.reference, "members", FieldValue.arrayRemove(uid))
                }
        }

        // On garde le fil de discussion mais on anonymise l'expéditeur : le
        // supprimer rendrait illisibles les conversations des autres.
        etape("messages") {
            db.collectionGroup("messages").whereEqualTo("senderId", uid).get().await()
                .documents.commitInChunks { batch, doc ->
                    batch.update(doc.reference, "senderUsername", "Compte supprimé")
                }
        }

        etape("succès") {
            profiles.document(uid).collection(COL_ACHIEVEMENTS).deleteAll()
        }

        // Le profil en dernier : les étapes précédentes en ont besoin pour
        // passer le contrôle « pas banni » des règles.
        etape("profil") { profiles.document(uid).delete().await() }

        return echecs
    }
}
