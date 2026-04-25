package com.rnandresy.lol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.FirebaseRepository
import com.rnandresy.lol.utils.ADMIN_BADGE_NAME
import com.rnandresy.lol.utils.DataUsageTracker
import com.rnandresy.lol.utils.NotificationHelper
import com.rnandresy.lol.utils.SettingsRepository
import com.rnandresy.lol.utils.isAdmin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AskipViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseRepository()
    private val notif = NotificationHelper(application)
    val dataTracker = DataUsageTracker()
    private val settings = SettingsRepository(application)

    // ── Auth ──────────────────────────────────────────────────────────────────

    val isLoggedIn = MutableStateFlow(FirebaseAuth.getInstance().currentUser != null)
    val currentUserId get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentEmail get() = FirebaseAuth.getInstance().currentUser?.email ?: ""

    // ── Profiles ──────────────────────────────────────────────────────────────

    private val _myProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile

    private val _viewedProfile = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile

    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles

    // ── Posts ─────────────────────────────────────────────────────────────────

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // ── Comments ──────────────────────────────────────────────────────────────

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    // ── Conversations & Messages ───────────────────────────────────────────────

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // ── Badges ────────────────────────────────────────────────────────────────

    private val _allBadges = MutableStateFlow<List<Badge>>(emptyList())
    val allBadges: StateFlow<List<Badge>> = _allBadges

    // ── Settings ──────────────────────────────────────────────────────────────

    val notifyMessages: StateFlow<Boolean> = settings.notifyMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifyPosts: StateFlow<Boolean> = settings.notifyPosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val totalBytesStored: StateFlow<Long> = settings.totalBytes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    // ── UI state ──────────────────────────────────────────────────────────────

    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    private var feedJob: Job? = null
    private var convJob: Job? = null
    private var msgJob: Job? = null
    private var profileJob: Job? = null
    private var badgesJob: Job? = null
    private var profilesJob: Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            isLoggedIn.value = auth.currentUser != null
            if (auth.currentUser != null) startListeners()
            else stopListeners()
        }
        if (isLoggedIn.value) startListeners()
    }

    private fun startListeners() {
        listenMyProfile()
        listenPosts()
        listenConversations()
        listenAllBadges()
        listenAllProfiles()
        fetchFcmToken()
    }

    private fun stopListeners() {
        feedJob?.cancel(); convJob?.cancel(); msgJob?.cancel()
        profileJob?.cancel(); badgesJob?.cancel(); profilesJob?.cancel()
        _posts.value = emptyList()
        _conversations.value = emptyList()
        _messages.value = emptyList()
        _myProfile.value = null
        _allProfiles.value = emptyList()
        _allBadges.value = emptyList()
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) = viewModelScope.launch {
        loading.value = true
        try { repo.login(email.trim(), password.trim()) }
        catch (e: Exception) { error.value = translateError(e.message) }
        finally { loading.value = false }
    }

    fun register(email: String, password: String, username: String) = viewModelScope.launch {
        loading.value = true
        try {
            val uid = repo.register(email.trim(), password.trim())
            repo.createDefaultProfile(uid, username.trim())
        } catch (e: Exception) { error.value = translateError(e.message) }
        finally { loading.value = false }
    }

    fun logout() {
        viewModelScope.launch {
            val bytes = dataTracker.getSessionBytes()
            if (bytes > 0) settings.addBytes(bytes)
        }
        repo.logout()
    }

    fun updateEmail(newEmail: String, password: String, onDone: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            try { repo.updateEmail(newEmail, password); onDone(true, null) }
            catch (e: Exception) { onDone(false, e.message) }
        }

    fun updatePassword(current: String, newPass: String, onDone: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            try { repo.updatePassword(current, newPass); onDone(true, null) }
            catch (e: Exception) { onDone(false, e.message) }
        }

    private fun fetchFcmToken() = viewModelScope.launch {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            repo.updateFcmToken(currentUserId, token)
        } catch (_: Exception) {}
    }

    private fun translateError(msg: String?): String = when {
        msg == null -> "Erreur inconnue"
        "password" in msg.lowercase() -> "Mot de passe incorrect"
        "email" in msg.lowercase() && "already" in msg.lowercase() -> "Email déjà utilisé"
        "no user" in msg.lowercase() -> "Aucun compte avec cet email"
        "network" in msg.lowercase() -> "Vérifiez votre connexion"
        else -> msg
    }

    // ── Profiles ──────────────────────────────────────────────────────────────

    private fun listenMyProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            repo.listenToProfile(currentUserId).collect { _myProfile.value = it }
        }
    }

    fun loadProfile(userId: String) = viewModelScope.launch {
        // Écoute temps réel du profil visité
        repo.listenToProfile(userId).collect { _viewedProfile.value = it }
    }

    private fun listenAllProfiles() {
        profilesJob?.cancel()
        profilesJob = viewModelScope.launch {
            repo.listenToAllProfiles().collect { profiles ->
                _allProfiles.value = profiles.filter { it.userId != currentUserId }
            }
        }
    }

    fun loadAllProfiles() = listenAllProfiles()

    fun updateProfile(data: Map<String, Any>, onDone: (() -> Unit)? = null) =
        viewModelScope.launch {
            try { repo.updateProfile(currentUserId, data); onDone?.invoke() }
            catch (e: Exception) { error.value = e.message }
        }

    // ── Posts ─────────────────────────────────────────────────────────────────

    private fun listenPosts() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            repo.listenToPosts().collect { newPosts ->
                val prev = _posts.value
                // Notification nouveau post
                if (prev.isNotEmpty() && notifyPosts.value) {
                    newPosts.filter { n -> prev.none { it.id == n.id } }
                        .filter { it.userId != currentUserId }
                        .firstOrNull()
                        ?.let { notif.showPostNotification(it.username, it.content) }
                }
                _posts.value = newPosts.sortedWith(
                    compareByDescending<Post> { it.isPinned }.thenByDescending { it.timestamp }
                )
                _isRefreshing.value = false
            }
        }
    }

    fun refreshFeed() {
        _isRefreshing.value = true
        feedJob?.cancel()
        listenPosts()
    }

    fun createPost(content: String) {
        val profile = _myProfile.value ?: return
        viewModelScope.launch {
            repo.createPost(mapOf(
                "userId" to currentUserId,
                "username" to profile.username,
                "userPhotoUrl" to profile.photoUrl,
                "content" to "Askip $content",
                "likes" to 0,
                "likedBy" to emptyList<String>(),
                "commentCount" to 0,
                "isPinned" to false,
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }

    fun toggleLike(post: Post) {
        val liked = post.likedBy.contains(currentUserId)
        viewModelScope.launch { repo.toggleLike(post.id, currentUserId, liked) }
    }

    fun togglePin(post: Post) = viewModelScope.launch { repo.togglePin(post.id, post.isPinned) }
    fun deletePost(postId: String) = viewModelScope.launch { repo.deletePost(postId) }

    // ── Comments ──────────────────────────────────────────────────────────────

    fun listenComments(postId: String) = viewModelScope.launch {
        repo.listenToComments(postId).collect { _comments.value = it }
    }

    fun addComment(postId: String, content: String) {
        val profile = _myProfile.value ?: return
        viewModelScope.launch {
            repo.addComment(postId, mapOf(
                "postId" to postId,
                "userId" to currentUserId,
                "username" to profile.username,
                "userPhotoUrl" to profile.photoUrl,
                "content" to content,
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }

    fun deleteComment(postId: String, commentId: String) =
        viewModelScope.launch { repo.deleteComment(postId, commentId) }

    // ── Conversations ─────────────────────────────────────────────────────────

    private fun listenConversations() {
        convJob?.cancel()
        convJob = viewModelScope.launch {
            repo.listenToConversations(currentUserId).collect { _conversations.value = it }
        }
    }

    fun startConversation(
        otherUserId: String, otherUsername: String, otherPhotoUrl: String,
        onDone: (String) -> Unit
    ) {
        val me = _myProfile.value ?: return
        viewModelScope.launch {
            try {
                val convId = repo.startOrGetConversation(
                    currentUserId, me.username, me.photoUrl,
                    otherUserId, otherUsername, otherPhotoUrl
                )
                onDone(convId)
            } catch (e: Exception) { error.value = e.message }
        }
    }

    fun listenMessages(convId: String) {
        msgJob?.cancel()
        msgJob = viewModelScope.launch {
            repo.listenToMessages(convId).collect { newMsgs ->
                val prev = _messages.value
                // Notification nouveau message (app ouverte)
                if (prev.isNotEmpty() && notifyMessages.value) {
                    newMsgs.filter { n -> prev.none { it.id == n.id } }
                        .filter { it.senderId != currentUserId }
                        .firstOrNull()
                        ?.let { notif.showMessageNotification(it.senderUsername, it.content) }
                }
                _messages.value = newMsgs
            }
        }
    }

    fun sendMessage(convId: String, content: String) {
        val profile = _myProfile.value ?: return
        val conv = _conversations.value.find { it.id == convId } ?: return
        val receiverId = conv.participants.firstOrNull { it != currentUserId } ?: return
        viewModelScope.launch {
            repo.sendMessage(convId, mapOf(
                "conversationId" to convId,
                "senderId" to currentUserId,
                "senderUsername" to profile.username,
                "senderPhotoUrl" to profile.photoUrl,
                "content" to content,
                "timestamp" to System.currentTimeMillis(),
                "readBy" to listOf(currentUserId)
            ), receiverId)
        }
    }

    fun markConversationRead(convId: String) = viewModelScope.launch {
        try { repo.markConversationRead(convId, currentUserId) } catch (_: Exception) {}
    }

    fun getUnreadCount(conv: Conversation): Int =
        (conv.unreadCounts[currentUserId] ?: 0L).toInt()

    // ── Badges ────────────────────────────────────────────────────────────────

    private fun listenAllBadges() {
        badgesJob?.cancel()
        badgesJob = viewModelScope.launch {
            repo.listenToAllBadges().collect { _allBadges.value = it }
        }
    }

    fun getBadgesForProfile(profile: UserProfile): List<Badge> =
        _allBadges.value.filter { it.id in profile.badgeIds }

    /**
     * Logique badge :
     * - Si le nom existe → le profil le portera (sauf badge "admin")
     * - Sinon → créer + porter
     * - Non-admin : 1 badge max (mais peut choisir parmi existants, pas créer si déjà 1)
     * - Admin : illimité
     */
    fun createOrWearBadge(
        displayName: String,
        colorHex: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            try {
                val trimmedName = displayName.trim()
                if (trimmedName.isBlank()) { onError("Le nom ne peut pas être vide"); return@launch }
                if (trimmedName.lowercase() == ADMIN_BADGE_NAME) {
                    onError("Ce nom est réservé à l'administration"); return@launch
                }

                val existing = repo.findBadgeByName(trimmedName)
                if (existing != null) {
                    // Badge existe → le porter si pas déjà porté
                    if (existing.id in profile.badgeIds) {
                        onError("Tu portes déjà ce badge !"); return@launch
                    }
                    if (!userIsAdmin && profile.badgeIds.isNotEmpty()) {
                        onError("Tu dois d'abord retirer ton badge actuel pour en porter un autre")
                        return@launch
                    }
                    repo.wearBadge(existing.id, currentUserId)
                } else {
                    // Badge n'existe pas → le créer
                    if (!userIsAdmin && profile.badgeIds.isNotEmpty()) {
                        onError("Tu as déjà un badge. Retire-le ou choisis un badge existant")
                        return@launch
                    }
                    repo.createBadge(Badge(displayName = trimmedName, colorHex = colorHex), currentUserId)
                }
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    /** Porter un badge existant (depuis la liste) */
    fun wearExistingBadge(badgeId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        val badge = _allBadges.value.find { it.id == badgeId } ?: return

        viewModelScope.launch {
            try {
                if (badge.name == ADMIN_BADGE_NAME) {
                    onError("Ce badge est réservé à l'administration"); return@launch
                }
                if (badgeId in profile.badgeIds) {
                    onError("Tu portes déjà ce badge !"); return@launch
                }
                if (!userIsAdmin && profile.badgeIds.isNotEmpty()) {
                    onError("Retire ton badge actuel avant d'en porter un autre"); return@launch
                }
                repo.wearBadge(badgeId, currentUserId)
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    /** Ne plus porter un badge */
    fun unwearBadge(badgeId: String) = viewModelScope.launch {
        try { repo.unwearBadge(badgeId, currentUserId) } catch (e: Exception) { error.value = e.message }
    }

    /** Modifier un badge (seulement son créateur ou l'admin) */
    fun updateBadge(
        badgeId: String, displayName: String, colorHex: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        val badge = _allBadges.value.find { it.id == badgeId }

        viewModelScope.launch {
            try {
                if (badge == null) { onError("Badge introuvable"); return@launch }
                if (!userIsAdmin && badge.createdBy != currentUserId) {
                    onError("Tu ne peux modifier que tes propres badges"); return@launch
                }
                val trimmed = displayName.trim()
                if (trimmed.isBlank()) { onError("Nom vide"); return@launch }
                if (trimmed.lowercase() == ADMIN_BADGE_NAME) {
                    onError("Ce nom est réservé"); return@launch
                }
                // Vérifier unicité si le nom change
                if (trimmed.lowercase() != badge.name) {
                    val existing = repo.findBadgeByName(trimmed)
                    if (existing != null) { onError("Ce nom de badge existe déjà"); return@launch }
                }
                repo.updateBadge(badgeId, trimmed, colorHex)
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    /** Supprimer un badge (seulement son créateur ou l'admin) */
    fun deleteBadge(badgeId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        val badge = _allBadges.value.find { it.id == badgeId }

        viewModelScope.launch {
            try {
                if (badge == null) { onError("Badge introuvable"); return@launch }
                if (!userIsAdmin && badge.createdBy != currentUserId) {
                    onError("Tu ne peux supprimer que tes propres badges"); return@launch
                }
                repo.deleteBadge(badgeId)
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun setNotifyMessages(v: Boolean) = viewModelScope.launch { settings.setNotifyMessages(v) }
    fun setNotifyPosts(v: Boolean) = viewModelScope.launch { settings.setNotifyPosts(v) }
    fun clearError() { error.value = null }
}