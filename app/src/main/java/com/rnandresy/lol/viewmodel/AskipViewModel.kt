package com.rnandresy.lol.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rnandresy.lol.model.Achievement
import com.rnandresy.lol.model.AppNotification
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.GroupMessage
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.FirebaseRepository
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.utils.ADMIN_BADGE_NAME
import com.rnandresy.lol.utils.CloudinaryUploader
import com.rnandresy.lol.utils.DataUsageTracker
import com.rnandresy.lol.utils.NotificationHelper
import com.rnandresy.lol.utils.SettingsRepository
import com.rnandresy.lol.utils.VoiceRecorder
import com.rnandresy.lol.utils.isAdmin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AskipViewModel(application: Application) : AndroidViewModel(application) {

    private val repo     = FirebaseRepository()
    private val notif    = NotificationHelper(application)
    private val settings = SettingsRepository(application)
    val dataTracker      = DataUsageTracker()

    // ── Auth ──────────────────────────────────────────────────────────────────

    val isLoggedIn     = MutableStateFlow(repo.isLoggedIn())
    val currentUserId get() = repo.currentUserId()
    val currentEmail  get() = repo.currentEmail()

    // ── Profiles ──────────────────────────────────────────────────────────────

    private val _myProfile      = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile

    private val _viewedProfile  = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile

    private val _profilesMap    = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val profilesMap: StateFlow<Map<String, UserProfile>> = _profilesMap

    val allProfiles: StateFlow<List<UserProfile>> = _profilesMap
        .map { it.values.filter { p -> p.userId != currentUserId }.toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Posts ─────────────────────────────────────────────────────────────────

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    val feedPosts: StateFlow<List<Post>> = combine(_allPosts, _profilesMap) { posts, profiles ->
        posts
            .filter { it.postType != "confession" }
            .map { post ->
                if (post.isAnonymous) post
                else {
                    val p = profiles[post.userId]
                    post.copy(
                        username     = p?.username?.takeIf { it.isNotBlank() } ?: post.username,
                        userPhotoUrl = p?.photoUrl ?: post.userPhotoUrl
                    )
                }
            }
            .sortedWith(compareByDescending<Post> { it.isPinned }.thenByDescending { it.timestamp })
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val confessions: StateFlow<List<Post>> = _allPosts
        .map { it.filter { p -> p.postType == "confession" }.sortedByDescending { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _stories      = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // ── Comments ──────────────────────────────────────────────────────────────

    private val _rawComments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = combine(_rawComments, _profilesMap) { list, profiles ->
        list.map { c ->
            val live = profiles[c.userId]?.username
            if (live != null && live != c.username) c.copy(username = live) else c
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Conversations ─────────────────────────────────────────────────────────

    private val _rawConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = combine(
        _rawConversations, _profilesMap
    ) { convs, profiles ->
        convs.map { conv ->
            val updated = conv.participantNames.toMutableMap()
            conv.participants.forEach { uid -> profiles[uid]?.username?.let { updated[uid] = it } }
            if (updated != conv.participantNames) conv.copy(participantNames = updated) else conv
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _rawMessages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = combine(_rawMessages, _profilesMap) { msgs, profiles ->
        msgs.map { msg ->
            val live = profiles[msg.senderId]?.username
            if (live != null && live != msg.senderUsername) msg.copy(senderUsername = live) else msg
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Groupes ───────────────────────────────────────────────────────────────

    private val _rawGroups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _rawGroups
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages

    // ── Badges ────────────────────────────────────────────────────────────────

    private val _allBadges = MutableStateFlow<List<Badge>>(emptyList())
    val allBadges: StateFlow<List<Badge>> = _allBadges

    private val _myBadges = MutableStateFlow<List<Badge>>(emptyList())
    val myBadges: StateFlow<List<Badge>> = _myBadges

    // ── Achievements ──────────────────────────────────────────────────────────

    private val _myAchievements     = MutableStateFlow<List<Achievement>>(emptyList())
    val myAchievements: StateFlow<List<Achievement>> = _myAchievements

    private val _viewedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val viewedAchievements: StateFlow<List<Achievement>> = _viewedAchievements

    // ── Notifications ─────────────────────────────────────────────────────────

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    val unreadNotifCount: StateFlow<Int> = _notifications
        .map { it.count { n -> !n.isRead } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Recherche ─────────────────────────────────────────────────────────────

    val searchQuery = MutableStateFlow("")

    data class SearchResults(
        val posts:  List<Post>        = emptyList(),
        val users:  List<UserProfile> = emptyList(),
        val groups: List<Group>       = emptyList()
    )

    val searchResults: StateFlow<SearchResults> = combine(
        searchQuery, _allPosts, _profilesMap, _rawGroups
    ) { query, posts, profiles, grps ->
        if (query.isBlank()) SearchResults()
        else {
            val q = query.trim().lowercase()
            SearchResults(
                posts  = posts.filter {
                    it.content.lowercase().contains(q) ||
                            (!it.isAnonymous && it.username.lowercase().contains(q))
                }.take(20),
                users  = profiles.values.filter {
                    it.username.lowercase().contains(q) ||
                            it.classeENI.lowercase().contains(q) ||
                            it.bio.lowercase().contains(q)
                }.take(15),
                groups = grps.filter {
                    it.name.lowercase().contains(q) ||
                            it.description.lowercase().contains(q)
                }.take(10)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, SearchResults())

    // ── Settings ──────────────────────────────────────────────────────────────

    val notifyMessages   = settings.notifyMessages.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifyPosts      = settings.notifyPosts.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val notifyMentions   = settings.notifyMentions.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val totalBytesStored = settings.totalBytes.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val appTheme         = settings.appTheme.stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.BLACK_WHITE)

    // ── UI ────────────────────────────────────────────────────────────────────

    val error     = MutableStateFlow<String?>(null)
    val loading   = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

    private val _isRecording      = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private var voiceRecorder:     VoiceRecorder? = null
    private var recordingTimerJob: Job? = null

    // ── Jobs ──────────────────────────────────────────────────────────────────

    private var postsJob:    Job? = null
    private var storiesJob:  Job? = null
    private var convJob:     Job? = null
    private var groupJob:    Job? = null
    private var groupMsgJob: Job? = null
    private var msgJob:      Job? = null
    private var commentJob:  Job? = null
    private var badgesJob:   Job? = null
    private var profileJob:  Job? = null
    private var profilesJob: Job? = null
    private var notifJob:    Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        FirebaseAuth.getInstance().addAuthStateListener { fa ->
            isLoggedIn.value = fa.currentUser != null
            if (fa.currentUser != null) startAll() else stopAll()
        }
        if (repo.isLoggedIn()) startAll()
    }

    private fun startAll() {
        listenMyProfile()
        listenAllProfiles()
        listenPosts()
        listenStories()
        listenConversations()
        listenGroups()
        listenAllBadges()
        listenNotifications()
    }

    private fun stopAll() {
        listOf(
            postsJob, storiesJob, convJob, groupJob, groupMsgJob, msgJob,
            commentJob, badgesJob, profileJob, profilesJob, notifJob
        ).forEach { it?.cancel() }
        _allPosts.value = emptyList()
        _stories.value  = emptyList()
        _rawConversations.value = emptyList()
        _rawMessages.value      = emptyList()
        _rawGroups.value        = emptyList()
        _groupMessages.value    = emptyList()
        _myProfile.value        = null
        _profilesMap.value      = emptyMap()
        _allBadges.value        = emptyList()
        _myBadges.value         = emptyList()
        _notifications.value    = emptyList()
    }

    // ── AUTH ──────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) = viewModelScope.launch {
        loading.value = true
        runCatching { repo.login(email.trim(), password.trim()) }
            .onFailure { error.value = friendly(it.message) }
        loading.value = false
    }

    fun register(email: String, password: String, username: String) = viewModelScope.launch {
        loading.value = true
        runCatching {
            val uid = repo.register(email.trim(), password.trim())
            repo.createDefaultProfile(uid, username.trim())
        }.onFailure { error.value = friendly(it.message) }
        loading.value = false
    }

    fun logout() = viewModelScope.launch {
        val bytes = dataTracker.getSessionBytes()
        if (bytes > 0) settings.addBytes(bytes)
        runCatching { repo.logout() }
    }

    fun updateEmail(e: String, p: String, onDone: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            runCatching { repo.updateEmail(e, p) }
                .onSuccess { onDone(true, null) }
                .onFailure { onDone(false, it.message) }
        }

    fun updatePassword(c: String, n: String, onDone: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            runCatching { repo.updatePassword(c, n) }
                .onSuccess { onDone(true, null) }
                .onFailure { onDone(false, it.message) }
        }

    private fun friendly(msg: String?) = when {
        msg == null -> "Erreur inconnue"
        "password" in msg.lowercase() -> "Mot de passe incorrect"
        "already"  in msg.lowercase() -> "Email déjà utilisé"
        "no user"  in msg.lowercase() -> "Aucun compte trouvé"
        "network"  in msg.lowercase() -> "Vérifiez votre connexion"
        "invalid"  in msg.lowercase() -> "Email ou mot de passe invalide"
        "weak"     in msg.lowercase() -> "Mot de passe trop faible (6 min)"
        else -> msg
    }

    // ── PROFILES ─────────────────────────────────────────────────────────────

    private fun listenMyProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            repo.listenToProfile(currentUserId).collect { profile ->
                _myProfile.value = profile
                profile?.let {
                    _myBadges.value  = _allBadges.value.filter { b -> b.id in it.badgeIds }
                    _profilesMap.value = _profilesMap.value + (it.userId to it)
                    checkAchievements(it)
                }
            }
        }
        viewModelScope.launch {
            _myAchievements.value = repo.getAchievements(currentUserId)
        }
    }

    private fun listenAllProfiles() {
        profilesJob?.cancel()
        profilesJob = viewModelScope.launch {
            repo.listenToAllProfiles().collect { profiles ->
                // Double filtre : username non vide ET userId non vide
                val valid = profiles.filter {
                    it.userId.isNotBlank() && it.username.isNotBlank()
                }
                _profilesMap.value = valid.associateBy { it.userId }
                _myProfile.value?.let { me ->
                    _myBadges.value = _allBadges.value.filter { b -> b.id in me.badgeIds }
                }
            }
        }
    }

    fun loadProfile(uid: String) = viewModelScope.launch {
        _viewedProfile.value      = _profilesMap.value[uid]
        repo.getProfile(uid)?.let { _viewedProfile.value = it }
        _viewedAchievements.value = repo.getAchievements(uid)
    }

    fun resolveUsername(userId: String, fallback: String = ""): String =
        _profilesMap.value[userId]?.username ?: fallback

    fun resolvePhotoUrl(userId: String): String =
        _profilesMap.value[userId]?.photoUrl ?: ""

    fun updateProfile(data: Map<String, Any?>, onDone: (() -> Unit)? = null) =
        viewModelScope.launch {
            runCatching {
                val oldUsername = _myProfile.value?.username ?: ""
                val newUsername = (data["username"] as? String)?.trim() ?: ""
                repo.updateProfile(currentUserId, data)
                if (newUsername.isNotBlank() && newUsername != oldUsername) {
                    isSyncing.value = true
                    launch(Dispatchers.IO) {
                        repo.syncUsername(currentUserId, newUsername)
                        isSyncing.value = false
                    }
                }
                onDone?.invoke()
            }.onFailure { isSyncing.value = false; error.value = it.message }
        }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        loading.value = true; _uploadProgress.value = 0
        runCatching {
            val url = CloudinaryUploader.uploadImage(getApplication(), uri) { _uploadProgress.value = it }
            repo.updateProfile(currentUserId, mapOf("photoUrl" to url))
        }.onFailure { error.value = it.message }
        loading.value = false; _uploadProgress.value = 0
    }

    fun uploadCover(uri: Uri) = viewModelScope.launch {
        loading.value = true; _uploadProgress.value = 0
        runCatching {
            val url = CloudinaryUploader.uploadImage(getApplication(), uri) { _uploadProgress.value = it }
            repo.updateProfile(currentUserId, mapOf("coverUrl" to url))
        }.onFailure { error.value = it.message }
        loading.value = false; _uploadProgress.value = 0
    }

    fun deleteProfilePhoto() = viewModelScope.launch {
        runCatching { repo.updateProfile(currentUserId, mapOf("photoUrl" to "")) }
            .onFailure { error.value = it.message }
    }

    fun deleteCoverPhoto() = viewModelScope.launch {
        runCatching { repo.updateProfile(currentUserId, mapOf("coverUrl" to "")) }
            .onFailure { error.value = it.message }
    }

    // ── ACHIEVEMENTS ──────────────────────────────────────────────────────────

    private fun checkAchievements(profile: UserProfile) = viewModelScope.launch {
        val uid      = currentUserId
        val fetched  = repo.getAchievements(uid)
        _myAchievements.value = fetched
        val unlocked = fetched.map { it.id }.toSet()

        suspend fun unlock(id: String) {
            if (id !in unlocked) {
                repo.unlockAchievement(uid, id)
                _myAchievements.value = _myAchievements.value +
                        Achievement(id = id, unlockedAt = System.currentTimeMillis())
            }
        }

        if (profile.postsCount >= 1)   unlock("first_post")
        if (profile.postsCount >= 10)  unlock("ten_posts")
        if (profile.postsCount >= 25)  unlock("twenty_five_p")
        if (profile.postsCount >= 50)  unlock("fifty_posts")
        if (profile.commentsCount >= 1)  unlock("first_comment")
        if (profile.commentsCount >= 20) unlock("commentator")
        if (profile.commentsCount >= 50) unlock("deep_comment")
        if (profile.confessionsCount >= 1) unlock("confessor")
        if (profile.confessionsCount >= 5) unlock("dark_confessor")
        if (profile.pollsCount >= 3)   unlock("poll_creator")
        if (profile.pollsCount >= 10)  unlock("poll_master")
        if (profile.storiesCount >= 1)  unlock("first_story")
        if (profile.storiesCount >= 10) unlock("storyteller")
        if (profile.convsStarted >= 5)  unlock("social")
        if (profile.convsStarted >= 20) unlock("social_plus")
        if (profile.streak >= 3)  unlock("streak_3")
        if (profile.streak >= 7)  unlock("streak_7")
        if (profile.streak >= 30) unlock("streak_30")
        if (profile.hasBadgeENI)            unlock("eni_pride")
        if (profile.badgeIds.isNotEmpty())  unlock("badge_maker")
        if (profile.moodEmoji.isNotBlank()) unlock("mood_master")
    }

    // ── BADGES ────────────────────────────────────────────────────────────────

    private fun listenAllBadges() {
        badgesJob?.cancel()
        badgesJob = viewModelScope.launch {
            repo.listenToAllBadges().collect { badges ->
                _allBadges.value = badges
                val myIds = _myProfile.value?.badgeIds ?: emptyList()
                _myBadges.value = badges.filter { it.id in myIds }
            }
        }
    }

    fun createOrWearBadge(
        displayName: String, colorHex: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val profile     = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            try {
                val trimmed = displayName.trim()
                if (trimmed.isBlank()) return@launch onError("Nom vide")
                if (trimmed.lowercase() == ADMIN_BADGE_NAME) return@launch onError("Nom réservé")
                val existing = repo.findBadgeByName(trimmed)
                if (existing != null) {
                    if (_myBadges.value.any { it.id == existing.id }) return@launch onError("Déjà porté !")
                    if (!userIsAdmin && _myBadges.value.isNotEmpty()) return@launch onError("Retire le badge actuel d'abord")
                    repo.wearBadge(existing.id, currentUserId)
                } else {
                    if (!userIsAdmin && _myBadges.value.isNotEmpty()) return@launch onError("Retire le badge actuel d'abord")
                    repo.createBadge(trimmed, colorHex, currentUserId)
                }
                repo.getProfile(currentUserId)?.let { checkAchievements(it) }
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    fun wearExistingBadge(badgeId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val profile     = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            try {
                val badge = _allBadges.value.find { it.id == badgeId }
                    ?: return@launch onError("Badge introuvable")
                if (badge.name == ADMIN_BADGE_NAME) return@launch onError("Badge réservé")
                if (_myBadges.value.any { it.id == badgeId }) return@launch onError("Déjà porté !")
                if (!userIsAdmin && _myBadges.value.isNotEmpty()) return@launch onError("Retire le badge actuel avant")
                repo.wearBadge(badgeId, currentUserId)
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    fun unwearBadge(badgeId: String) = viewModelScope.launch {
        runCatching { repo.unwearBadge(badgeId, currentUserId) }
    }

    fun updateBadge(
        badgeId: String, displayName: String, colorHex: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        val profile     = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            try {
                val badge = _allBadges.value.find { it.id == badgeId }
                    ?: return@launch onError("Badge introuvable")
                if (!userIsAdmin && badge.createdBy != currentUserId) return@launch onError("Interdit")
                val trimmed = displayName.trim()
                if (trimmed.isBlank()) return@launch onError("Nom vide")
                if (trimmed.lowercase() != badge.name && repo.findBadgeByName(trimmed) != null)
                    return@launch onError("Ce nom existe déjà")
                repo.updateBadge(badgeId, trimmed, colorHex)
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    fun deleteBadge(badgeId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val profile     = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            try {
                val badge = _allBadges.value.find { it.id == badgeId }
                    ?: return@launch onError("Badge introuvable")
                if (!userIsAdmin && badge.createdBy != currentUserId) return@launch onError("Interdit")
                repo.deleteBadge(badgeId)
                onSuccess()
            } catch (e: Exception) { onError(e.message ?: "Erreur") }
        }
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────

    private fun listenNotifications() {
        notifJob?.cancel()
        notifJob = viewModelScope.launch {
            repo.listenToNotifications(currentUserId)
                .catch { /* index manquant ou erreur réseau — silencieux */ }
                .collect { notifs ->
                    // Résolution des pseudos sans écraser l'état isRead de Firestore
                    _notifications.value = notifs.map { n ->
                        val live = _profilesMap.value[n.fromUserId]?.username
                        if (live != null && live != n.fromUsername)
                            n.copy(fromUsername = live)
                        else n
                    }
                }
        }
    }

    fun markNotificationRead(notifId: String) = viewModelScope.launch {
        // 1. Optimiste local immédiat
        val prev = _notifications.value
        _notifications.value = prev.map {
            if (it.id == notifId) it.copy(isRead = true) else it
        }
        // 2. Persistance Firestore — revert si échec
        runCatching {
            repo.markNotificationRead(notifId)
        }.onFailure {
            _notifications.value = prev   // revert local
        }
    }

    fun markAllNotificationsRead() = viewModelScope.launch {
        // 1. Optimiste local
        val prev = _notifications.value
        _notifications.value = prev.map { it.copy(isRead = true) }
        // 2. Persistance Firestore — revert si échec
        runCatching {
            repo.markAllNotificationsRead(currentUserId)
        }.onFailure {
            _notifications.value = prev   // revert local
        }
    }

    fun deleteNotification(notifId: String) = viewModelScope.launch {
        _notifications.value = _notifications.value.filter { it.id != notifId }
        repo.deleteNotification(notifId)
    }

    // ── SUPPRESSION DE COMPTE ─────────────────────────────────────────────────
    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        loading.value = true
        runCatching {
            val bytes = dataTracker.getSessionBytes()
            if (bytes > 0) settings.addBytes(bytes)
            repo.deleteAccount(currentUserId, password)
        }.onSuccess {
            stopAll()
            onSuccess()
        }.onFailure { e ->
            val msg = when {
                e.message?.contains("password", ignoreCase = true) == true ->
                    "Mot de passe incorrect"
                e.message?.contains("recent", ignoreCase = true) == true   ->
                    "Reconnecte-toi d'abord et réessaie"
                else -> e.message ?: "Erreur lors de la suppression"
            }
            onError(msg)
        }
        loading.value = false
    }

    private suspend fun handleMentions(
        text: String, fromUserId: String, fromUsername: String,
        fromAdmin: Boolean, postId: String = "", convId: String = ""
    ) {
        val now = System.currentTimeMillis()
        if (fromAdmin && Regex(
                "@(everyone|tout_le_monde|tous)",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(text)
        ) {
            repo.createNotificationsForAll(mapOf(
                "type"           to "mention_everyone",
                "fromUserId"     to fromUserId,
                "fromUsername"   to fromUsername,
                "fromIsAdmin"    to true,
                "postId"         to postId,
                "conversationId" to convId,
                "content"        to text.take(150),
                "isRead"         to false,
                "timestamp"      to now
            ), fromUserId)
            notif.showEveryoneMentionNotification(fromUsername, text)
            return
        }
        extractMentions(text).forEach { username ->
            val tp = _profilesMap.value.values.find { it.username == username }
                ?: repo.findProfileByUsername(username)
                ?: return@forEach
            if (tp.userId == fromUserId) return@forEach
            repo.createNotification(mapOf(
                "targetUserId"   to tp.userId,
                "type"           to "mention",
                "fromUserId"     to fromUserId,
                "fromUsername"   to fromUsername,
                "fromIsAdmin"    to fromAdmin,
                "postId"         to postId,
                "conversationId" to convId,
                "content"        to text.take(150),
                "isRead"         to false,
                "timestamp"      to now
            ))
            if (tp.userId == currentUserId && (fromAdmin || notifyMentions.value))
                notif.showMentionNotification(fromUsername, text, fromAdmin)
        }
    }

    private fun extractMentions(text: String): List<String> =
        Regex("@([\\w]+)").findAll(text)
            .map { it.groupValues[1] }
            .filter { it.lowercase() !in setOf("everyone", "tout_le_monde", "tous") }
            .distinct()
            .toList()

    // ── POSTS ─────────────────────────────────────────────────────────────────

    private fun listenPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            repo.listenToPosts().collect { posts ->
                _allPosts.value     = posts
                _isRefreshing.value = false
            }
        }
    }

    fun refreshFeed() {
        _isRefreshing.value = true
        listenPosts()
    }

    /** Crée un post texte simple (délègue à createPostWithMedia) */
    fun createPost(
        content: String,
        type: String = "normal",
        pollOpt1: String = "",
        pollOpt2: String = ""
    ) {
        createPostWithMedia(content, type, pollOpt1, pollOpt2, null, null, null, null)
    }

    /** Crée un post avec médias optionnels */
    fun createPostWithMedia(
        content: String,
        type: String = "normal",
        pollOpt1: String = "",
        pollOpt2: String = "",
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        audioFile: File? = null,
        fileUri: Uri? = null
    ) {
        val profile     = _myProfile.value ?: return
        val isConf      = type == "confession"
        val fromAdmin   = isAdmin(currentUserId) || profile.isAdmin
        val displayName = if (isConf) "Quelqu'un 🎭" else profile.username
        val postContent = if (type != "poll") "Askip $content" else content

        viewModelScope.launch {
            loading.value = true; _uploadProgress.value = 0
            runCatching {
                var imageUrl      = ""; var videoUrl = ""
                var audioUrl      = ""; var audioDuration = 0
                var fileUrl       = ""; var fileName = ""

                imageUri?.let {
                    imageUrl = CloudinaryUploader.uploadImage(getApplication(), it) { p -> _uploadProgress.value = p / 4 }
                }
                videoUri?.let {
                    videoUrl = CloudinaryUploader.uploadVideo(getApplication(), it) { p -> _uploadProgress.value = 25 + p / 4 }
                }
                audioFile?.let {
                    audioUrl = CloudinaryUploader.uploadAudio(it) { p -> _uploadProgress.value = 50 + p / 4 }
                    audioFile.delete()
                }
                fileUri?.let {
                    val (url, name) = CloudinaryUploader.uploadFile(getApplication(), it) { p -> _uploadProgress.value = 75 + p / 4 }
                    fileUrl = url; fileName = name
                }

                val postId = repo.createPost(mapOf(
                    "userId"       to currentUserId,
                    "username"     to displayName,
                    "userPhotoUrl" to if (isConf) "" else profile.photoUrl,
                    "content"      to postContent,
                    "postType"     to type,
                    "isAnonymous"  to isConf,
                    "imageUrl"     to imageUrl,
                    "videoUrl"     to videoUrl,
                    "audioUrl"     to audioUrl,
                    "audioDuration" to audioDuration,
                    "fileUrl"      to fileUrl,
                    "fileName"     to fileName,
                    "pollOption1"  to pollOpt1,
                    "pollOption2"  to pollOpt2,
                    "pollVotes1"   to 0,
                    "pollVotes2"   to 0,
                    "pollVoters"   to emptyList<String>(),
                    "likedBy"      to emptyList<String>(),
                    "fireBy"       to emptyList<String>(),
                    "lolBy"        to emptyList<String>(),
                    "shockBy"      to emptyList<String>(),
                    "eyesBy"       to emptyList<String>(),
                    "commentCount" to 0,
                    "isPinned"     to false,
                    "timestamp"    to System.currentTimeMillis()
                ))

                val field = when (type) {
                    "confession" -> "confessionsCount"
                    "poll"       -> "pollsCount"
                    else         -> "postsCount"
                }
                repo.incrementCounter(currentUserId, field)
                repo.updateStreak(currentUserId)
                repo.getProfile(currentUserId)?.let { checkAchievements(it) }

                if (!isConf) {
                    val now = System.currentTimeMillis()
                    repo.createNotificationsForAll(mapOf(
                        "type"           to if (fromAdmin) "new_post_admin" else "new_post",
                        "fromUserId"     to currentUserId,
                        "fromUsername"   to profile.username,
                        "fromIsAdmin"    to fromAdmin,
                        "postId"         to postId,
                        "conversationId" to "",
                        "content"        to postContent.take(150),
                        "isRead"         to false,
                        "timestamp"      to now
                    ), currentUserId)
                    if (fromAdmin) notif.showAdminPostNotification(profile.username, postContent)
                    else if (notifyPosts.value) notif.showPostNotification(profile.username, postContent)
                    handleMentions(postContent, currentUserId, displayName, fromAdmin, postId)
                }
            }.onFailure { error.value = it.message }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    fun deletePost(postId: String) = viewModelScope.launch {
        runCatching {
            val post = _allPosts.value.find { it.id == postId }
            repo.deletePost(postId)
            post?.let {
                val field = when (it.postType) {
                    "confession" -> "confessionsCount"
                    "poll"       -> "pollsCount"
                    else         -> "postsCount"
                }
                repo.incrementCounter(it.userId, field, -1L)
            }
        }.onFailure { error.value = it.message }
    }

    // ── togglePin — fix : était manquant ─────────────────────────────────────
    fun togglePin(post: Post) = viewModelScope.launch {
        runCatching { repo.togglePin(post.id, post.isPinned) }
            .onFailure { error.value = it.message }
    }

    // ── toggleReaction — update atomique, le listener Firestore fait le reste ─
    fun toggleReaction(post: Post, emoji: String) {
        val uid     = currentUserId
        val current = post.getUserReaction(uid)
        viewModelScope.launch {
            runCatching {
                if (current == emoji) repo.removeReaction(post.id, uid, emoji)
                else repo.addReaction(post.id, uid, emoji, current)
            }.onFailure { error.value = it.message }
        }
    }

    fun votePoll(postId: String, option: Int) {
        val uid  = currentUserId
        val post = _allPosts.value.find { it.id == postId } ?: return
        if (uid in post.pollVoters) return
        viewModelScope.launch { runCatching { repo.votePoll(postId, uid, option) } }
    }

    // ── STORIES ───────────────────────────────────────────────────────────────

    private fun listenStories() {
        storiesJob?.cancel()
        storiesJob = viewModelScope.launch {
            repo.listenToStories().collect { rawStories ->
                _stories.value = rawStories.map { s ->
                    val live = _profilesMap.value[s.userId]?.username
                    if (live != null && live != s.username) s.copy(username = live) else s
                }
            }
        }
    }

    fun createStory(content: String, emoji: String, bgColor: String) {
        val profile = _myProfile.value ?: return
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                repo.createStory(mapOf(
                    "userId"          to currentUserId,
                    "username"        to profile.username,
                    "content"         to content,
                    "emoji"           to emoji,
                    "backgroundColor" to bgColor,
                    "timestamp"       to now,
                    "expiresAt"       to (now + 24L * 3_600_000L)
                ))
                repo.incrementCounter(currentUserId, "storiesCount")
                repo.updateStreak(currentUserId)
                repo.getProfile(currentUserId)?.let { checkAchievements(it) }
            }.onFailure { error.value = it.message }
        }
    }

    fun deleteStory(storyId: String) = viewModelScope.launch {
        runCatching {
            // Récupère AVANT la suppression (le listener vide la liste ensuite)
            val story = _stories.value.find { it.id == storyId }
            repo.deleteStory(storyId)
            story?.let { repo.incrementCounter(it.userId, "storiesCount", -1L) }
        }.onFailure { error.value = it.message }
    }

    // ── COMMENTS ──────────────────────────────────────────────────────────────

    fun listenComments(postId: String) {
        commentJob?.cancel()
        commentJob = viewModelScope.launch {
            repo.listenToComments(postId).collect { _rawComments.value = it }
        }
    }

    fun addComment(postId: String, content: String) {
        val profile   = _myProfile.value ?: return
        val fromAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            runCatching {
                repo.addComment(postId, mapOf(
                    "postId"    to postId,
                    "userId"    to currentUserId,
                    "username"  to profile.username,
                    "content"   to content,
                    "timestamp" to System.currentTimeMillis()
                ))
                repo.incrementCounter(currentUserId, "commentsCount")
                repo.updateStreak(currentUserId)
                repo.getProfile(currentUserId)?.let { checkAchievements(it) }
                handleMentions(content, currentUserId, profile.username, fromAdmin, postId)
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) = viewModelScope.launch {
        runCatching { repo.deleteComment(postId, commentId) }
    }

    // ── CONVERSATIONS ─────────────────────────────────────────────────────────

    private fun listenConversations() {
        convJob?.cancel()
        convJob = viewModelScope.launch {
            repo.listenToConversations(currentUserId).collect { _rawConversations.value = it }
        }
    }

    fun startConversation(otherId: String, otherUsername: String, onDone: (String) -> Unit) {
        val me = _myProfile.value ?: return
        viewModelScope.launch {
            runCatching {
                val convId = repo.getOrCreateConversation(
                    currentUserId, me.username,
                    otherId, resolveUsername(otherId, otherUsername)
                )
                repo.incrementCounter(currentUserId, "convsStarted")
                repo.getProfile(currentUserId)?.let { checkAchievements(it) }
                onDone(convId)
            }.onFailure { error.value = it.message }
        }
    }

    fun listenMessages(convId: String) {
        msgJob?.cancel()
        msgJob = viewModelScope.launch {
            repo.listenToMessages(convId).collect { newMsgs ->
                val prev = _rawMessages.value
                if (prev.isNotEmpty()) {
                    newMsgs.firstOrNull { n ->
                        prev.none { it.id == n.id } && n.senderId != currentUserId
                    }?.let { msg ->
                        val fromAdmin = isAdmin(msg.senderId)
                        if (fromAdmin || notifyMessages.value) {
                            notif.showMessageNotification(
                                resolveUsername(msg.senderId, msg.senderUsername),
                                when {
                                    msg.isAudio() -> "🎤 Message vocal"
                                    msg.isImage() -> "📸 Photo"
                                    msg.isVideo() -> "🎥 Vidéo"
                                    msg.isFile()  -> "📎 ${msg.mediaName}"
                                    else          -> msg.content
                                },
                                fromAdmin
                            )
                        }
                    }
                }
                _rawMessages.value = newMsgs
            }
        }
    }

    fun sendMessage(convId: String, content: String) {
        sendMessageWithMedia(convId, content = content)
    }

    fun sendMessageWithMedia(
        convId: String,
        content: String = "",
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        fileUri: Uri? = null
    ) {
        val profile    = _myProfile.value ?: return
        val conv       = _rawConversations.value.find { it.id == convId } ?: return
        val receiverId = conv.participants.firstOrNull { it != currentUserId } ?: return
        val fromAdmin  = isAdmin(currentUserId) || profile.isAdmin

        viewModelScope.launch {
            if (imageUri != null || videoUri != null || fileUri != null) {
                loading.value = true; _uploadProgress.value = 0
            }
            runCatching {
                var mediaUrl = ""; var mediaType = ""; var mediaName = ""
                when {
                    imageUri != null -> {
                        mediaUrl  = CloudinaryUploader.uploadImage(getApplication(), imageUri) { _uploadProgress.value = it }
                        mediaType = "image"
                    }
                    videoUri != null -> {
                        mediaUrl  = CloudinaryUploader.uploadVideo(getApplication(), videoUri) { _uploadProgress.value = it }
                        mediaType = "video"
                    }
                    fileUri  != null -> {
                        val (url, name) = CloudinaryUploader.uploadFile(getApplication(), fileUri) { _uploadProgress.value = it }
                        mediaUrl = url; mediaType = "file"; mediaName = name
                    }
                }
                val preview = when (mediaType) {
                    "image" -> "📸 Photo"
                    "video" -> "🎥 Vidéo"
                    "file"  -> "📎 $mediaName"
                    "audio" -> "🎤 Vocal"
                    else    -> content.take(80)
                }
                repo.sendMessage(convId, mapOf(
                    "conversationId" to convId,
                    "senderId"       to currentUserId,
                    "senderUsername" to profile.username,
                    "content"        to content,
                    "mediaUrl"       to mediaUrl,
                    "mediaType"      to mediaType,
                    "mediaName"      to mediaName,
                    "mediaDuration"  to 0,
                    "timestamp"      to System.currentTimeMillis()
                ), receiverId)
                repo.createNotification(mapOf(
                    "targetUserId"   to receiverId,
                    "type"           to "message",
                    "fromUserId"     to currentUserId,
                    "fromUsername"   to profile.username,
                    "fromIsAdmin"    to fromAdmin,
                    "postId"         to "",
                    "conversationId" to convId,
                    "content"        to preview,
                    "isRead"         to false,
                    "timestamp"      to System.currentTimeMillis()
                ))
            }.onFailure { error.value = it.message }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    // ── markRead — fix : était manquant ──────────────────────────────────────
    fun markRead(convId: String) = viewModelScope.launch {
        repo.markRead(convId, currentUserId)
    }

    // ── getUnread — fix : était manquant ─────────────────────────────────────
    fun getUnread(conv: Conversation): Int =
        (conv.unreadCounts[currentUserId] ?: 0L).toInt()

    // ── VOCAL ─────────────────────────────────────────────────────────────────

    fun startVoiceRecording(context: android.content.Context) {
        if (_isRecording.value) return
        voiceRecorder = VoiceRecorder(context)
        runCatching { voiceRecorder?.start() }
            .onSuccess {
                _isRecording.value    = true
                _recordingSeconds.value = 0
                recordingTimerJob = viewModelScope.launch {
                    while (isActive) {
                        delay(1000L)
                        _recordingSeconds.value++
                        if (_recordingSeconds.value >= 120) cancelVoiceRecording()
                    }
                }
            }
            .onFailure {
                error.value = "Micro non disponible"
                voiceRecorder = null
            }
    }

    fun stopAndSendVoice(convId: String) {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null; _isRecording.value = false
        if (result == null || convId.isBlank()) return
        val (file, durationSec) = result
        val profile    = _myProfile.value ?: run { file.delete(); return }
        val conv       = _rawConversations.value.find { it.id == convId } ?: run { file.delete(); return }
        val receiverId = conv.participants.firstOrNull { it != currentUserId } ?: run { file.delete(); return }
        val fromAdmin  = isAdmin(currentUserId) || profile.isAdmin

        viewModelScope.launch {
            loading.value = true; _uploadProgress.value = 0
            runCatching {
                val url = CloudinaryUploader.uploadAudio(file) { _uploadProgress.value = it }
                file.delete()
                repo.sendMessage(convId, mapOf(
                    "conversationId" to convId,
                    "senderId"       to currentUserId,
                    "senderUsername" to profile.username,
                    "content"        to "",
                    "mediaUrl"       to url,
                    "mediaType"      to "audio",
                    "mediaName"      to "vocal_${durationSec}s.m4a",
                    "mediaDuration"  to durationSec,
                    "timestamp"      to System.currentTimeMillis()
                ), receiverId)
                repo.createNotification(mapOf(
                    "targetUserId"   to receiverId,
                    "type"           to "message",
                    "fromUserId"     to currentUserId,
                    "fromUsername"   to profile.username,
                    "fromIsAdmin"    to fromAdmin,
                    "postId"         to "",
                    "conversationId" to convId,
                    "content"        to "🎤 Message vocal (${durationSec}s)",
                    "isRead"         to false,
                    "timestamp"      to System.currentTimeMillis()
                ))
            }.onFailure { error.value = it.message; file.delete() }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    fun stopRecordingForPost(): Pair<File, Int>? {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null
        _isRecording.value      = false
        _recordingSeconds.value = 0
        return result
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        voiceRecorder?.cancel()
        voiceRecorder           = null
        _isRecording.value      = false
        _recordingSeconds.value = 0
    }

    // ── GROUPES ───────────────────────────────────────────────────────────────

    private fun listenGroups() {
        groupJob?.cancel()
        groupJob = viewModelScope.launch {
            repo.listenToGroups(currentUserId).collect { _rawGroups.value = it }
        }
    }

    fun listenGroupMessages(groupId: String) {
        groupMsgJob?.cancel()
        groupMsgJob = viewModelScope.launch {
            repo.listenToGroupMessages(groupId).collect { msgs ->
                _groupMessages.value = msgs.map { msg ->
                    val live = _profilesMap.value[msg.senderId]?.username
                    if (live != null && live != msg.senderUsername) msg.copy(senderUsername = live)
                    else msg
                }
            }
        }
    }

    fun createGroup(
        name: String, description: String, emoji: String,
        memberIds: List<String>, onDone: (String) -> Unit
    ) {
        val profile      = _myProfile.value ?: return
        val memberNames  = memberIds.associate { it to resolveUsername(it, it) }
        val memberPhotos = memberIds.associate { it to resolvePhotoUrl(it) }
        viewModelScope.launch {
            runCatching {
                val id = repo.createGroup(
                    name, description, emoji,
                    currentUserId, profile.username, profile.photoUrl,
                    memberIds, memberNames, memberPhotos
                )
                onDone(id)
            }.onFailure { error.value = it.message }
        }
    }

    fun sendGroupMessage(groupId: String, content: String) {
        sendGroupMessageWithMedia(groupId, content)
    }

    fun sendGroupMessageWithMedia(
        groupId: String,
        content: String = "",
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        fileUri: Uri? = null
    ) {
        val profile = _myProfile.value ?: return
        viewModelScope.launch {
            if (imageUri != null || videoUri != null || fileUri != null) {
                loading.value = true; _uploadProgress.value = 0
            }
            runCatching {
                var mediaUrl = ""; var mediaType = ""; var mediaName = ""
                when {
                    imageUri != null -> {
                        mediaUrl  = CloudinaryUploader.uploadImage(getApplication(), imageUri) { _uploadProgress.value = it }
                        mediaType = "image"
                    }
                    videoUri != null -> {
                        mediaUrl  = CloudinaryUploader.uploadVideo(getApplication(), videoUri) { _uploadProgress.value = it }
                        mediaType = "video"
                    }
                    fileUri  != null -> {
                        val (url, name) = CloudinaryUploader.uploadFile(getApplication(), fileUri) { _uploadProgress.value = it }
                        mediaUrl = url; mediaType = "file"; mediaName = name
                    }
                }
                repo.sendGroupMessage(groupId, mapOf(
                    "groupId"        to groupId,
                    "senderId"       to currentUserId,
                    "senderUsername" to profile.username,
                    "content"        to content,
                    "mediaUrl"       to mediaUrl,
                    "mediaType"      to mediaType,
                    "mediaName"      to mediaName,
                    "mediaDuration"  to 0,
                    "timestamp"      to System.currentTimeMillis()
                ))
            }.onFailure { error.value = it.message }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    fun stopAndSendGroupVoice(groupId: String) {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null; _isRecording.value = false
        if (result == null) return
        val (file, durationSec) = result
        val profile = _myProfile.value ?: run { file.delete(); return }

        viewModelScope.launch {
            loading.value = true; _uploadProgress.value = 0
            runCatching {
                val url = CloudinaryUploader.uploadAudio(file) { _uploadProgress.value = it }
                file.delete()
                repo.sendGroupMessage(groupId, mapOf(
                    "groupId"        to groupId,
                    "senderId"       to currentUserId,
                    "senderUsername" to profile.username,
                    "content"        to "",
                    "mediaUrl"       to url,
                    "mediaType"      to "audio",
                    "mediaName"      to "vocal_${durationSec}s.m4a",
                    "mediaDuration"  to durationSec,
                    "timestamp"      to System.currentTimeMillis()
                ))
            }.onFailure { error.value = it.message; file.delete() }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    fun leaveGroup(groupId: String) = viewModelScope.launch {
        runCatching { repo.removeGroupMember(groupId, currentUserId) }
            .onFailure { error.value = it.message }
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch {
        runCatching { repo.deleteGroup(groupId) }
            .onFailure { error.value = it.message }
    }

    // ── SETTINGS ──────────────────────────────────────────────────────────────

    fun setNotifyMessages(v: Boolean) = viewModelScope.launch { settings.setNotifyMessages(v) }
    fun setNotifyPosts(v: Boolean)    = viewModelScope.launch { settings.setNotifyPosts(v) }
    fun setNotifyMentions(v: Boolean) = viewModelScope.launch { settings.setNotifyMentions(v) }
    fun setTheme(t: AppTheme)         = viewModelScope.launch { settings.setTheme(t) }
    fun clearError() { error.value = null }
}