package com.rnandresy.lol.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rnandresy.lol.model.Achievement
import com.rnandresy.lol.model.AppNotification
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.FirebaseRepository
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

    // ── States ────────────────────────────────────────────────────────────────

    val isLoggedIn    = MutableStateFlow(repo.isLoggedIn())
    val currentUserId get() = repo.currentUserId()
    val currentEmail  get() = repo.currentEmail()

    private val _myProfile      = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile

    private val _viewedProfile  = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile

    // Tous les profils indexés par userId pour résolution instantanée
    private val _profilesMap    = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val profilesMap: StateFlow<Map<String, UserProfile>> = _profilesMap

    val allProfiles: StateFlow<List<UserProfile>> = _profilesMap
        .map { it.values.filter { p -> p.userId != currentUserId }.toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    // Posts du feed avec pseudo résolu en temps réel
    val feedPosts: StateFlow<List<Post>> = combine(_allPosts, _profilesMap) { posts, profiles ->
        posts
            .filter { it.postType != "confession" }
            .map { post ->
                if (post.isAnonymous) post
                else {
                    val profile      = profiles[post.userId]
                    val liveName     = profile?.username?.takeIf { it.isNotBlank() }
                    val livePhotoUrl = profile?.photoUrl ?: ""
                    post.copy(
                        username     = liveName ?: post.username,
                        userPhotoUrl = livePhotoUrl.ifBlank { post.userPhotoUrl }
                    )
                }
            }
            .sortedWith(
                compareByDescending<Post> { it.isPinned }.thenByDescending { it.timestamp }
            )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val confessions: StateFlow<List<Post>> = _allPosts
        .map { it.filter { p -> p.postType == "confession" }.sortedByDescending { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _stories      = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Commentaires avec pseudo résolu
    private val _rawComments  = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = combine(_rawComments, _profilesMap) { list, profiles ->
        list.map { c ->
            val live = profiles[c.userId]?.username
            if (live != null && live != c.username) c.copy(username = live) else c
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Conversations avec noms résolus
    private val _rawConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = combine(
        _rawConversations, _profilesMap
    ) { convs, profiles ->
        convs.map { conv ->
            val updated = conv.participantNames.toMutableMap()
            conv.participants.forEach { uid ->
                profiles[uid]?.username?.let { updated[uid] = it }
            }
            if (updated != conv.participantNames) conv.copy(participantNames = updated) else conv
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── UPLOAD CLOUDINARY ─────────────────────────────────────────────────────

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

    private val _isRecording     = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private var voiceRecorder: VoiceRecorder? = null
    private var recordingTimerJob: Job? = null

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        loading.value = true
        _uploadProgress.value = 0
        runCatching {
            val url = CloudinaryUploader.uploadImage(
                context    = getApplication(),
                uri        = uri,
                onProgress = { _uploadProgress.value = it }
            )
            repo.updateProfile(currentUserId, mapOf("photoUrl" to url))
        }.onFailure { error.value = "Erreur upload photo: ${it.message}" }
        loading.value = false
        _uploadProgress.value = 0
    }

    fun uploadCover(uri: Uri) = viewModelScope.launch {
        loading.value = true
        _uploadProgress.value = 0
        runCatching {
            val url = CloudinaryUploader.uploadImage(
                context    = getApplication(),
                uri        = uri,
                onProgress = { _uploadProgress.value = it }
            )
            repo.updateProfile(currentUserId, mapOf("coverUrl" to url))
        }.onFailure { error.value = "Erreur upload couverture: ${it.message}" }
        loading.value = false
        _uploadProgress.value = 0
    }

    fun createPostWithMedia(
        content: String,
        type: String = "normal",
        pollOpt1: String = "",
        pollOpt2: String = "",
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        audioFile: File? = null,          // ← note vocale dans le post
        fileUri: Uri? = null
    ) {
        val profile    = _myProfile.value ?: return
        val isConf     = type == "confession"
        val fromAdmin  = isAdmin(currentUserId) || profile.isAdmin
        val displayName = if (isConf) "Quelqu'un 🎭" else profile.username
        val postContent = if (type != "poll") "Askip $content" else content

        viewModelScope.launch {
            loading.value = true; _uploadProgress.value = 0
            runCatching {
                var imageUrl      = ""; var videoUrl = ""; var audioUrl = ""
                var audioDuration = 0; var fileUrl = ""; var fileName = ""

                imageUri?.let {
                    imageUrl = CloudinaryUploader.uploadImage(getApplication(), it) { p -> _uploadProgress.value = p / 4 }
                }
                videoUri?.let {
                    videoUrl = CloudinaryUploader.uploadVideo(getApplication(), it) { p -> _uploadProgress.value = p / 4 }
                }
                audioFile?.let {
                    audioUrl      = CloudinaryUploader.uploadAudio(it) { p -> _uploadProgress.value = 50 + p / 4 }
                    audioDuration = 0  // durée connue à l'enregistrement
                    audioFile.delete()
                }
                fileUri?.let {
                    val (url, name) = CloudinaryUploader.uploadFile(getApplication(), it) { p -> _uploadProgress.value = 75 + p / 4 }
                    fileUrl = url; fileName = name
                }

                val postId = repo.createPost(mapOf(
                    "userId" to currentUserId, "username" to displayName,
                    "userPhotoUrl" to if (isConf) "" else profile.photoUrl,
                    "content" to postContent, "postType" to type, "isAnonymous" to isConf,
                    "imageUrl" to imageUrl, "videoUrl" to videoUrl,
                    "audioUrl" to audioUrl, "audioDuration" to audioDuration,
                    "fileUrl" to fileUrl, "fileName" to fileName,
                    "pollOption1" to pollOpt1, "pollOption2" to pollOpt2,
                    "pollVotes1" to 0, "pollVotes2" to 0, "pollVoters" to emptyList<String>(),
                    "likedBy" to emptyList<String>(), "fireBy" to emptyList<String>(),
                    "lolBy" to emptyList<String>(), "shockBy" to emptyList<String>(), "eyesBy" to emptyList<String>(),
                    "commentCount" to 0, "isPinned" to false, "timestamp" to System.currentTimeMillis()
                ))

                val counterField = when (type) { "confession" -> "confessionsCount"; "poll" -> "pollsCount"; else -> "postsCount" }
                repo.incrementCounter(currentUserId, counterField)
                repo.updateStreak(currentUserId)
                repo.getProfile(currentUserId)?.let { checkAchievements(it) }

                if (!isConf) {
                    val now = System.currentTimeMillis()
                    repo.createNotificationsForAll(mapOf(
                        "type" to if (fromAdmin) "new_post_admin" else "new_post",
                        "fromUserId" to currentUserId, "fromUsername" to profile.username,
                        "fromIsAdmin" to fromAdmin, "postId" to postId, "conversationId" to "",
                        "content" to postContent.take(150), "isRead" to false, "timestamp" to now
                    ), currentUserId)
                    if (fromAdmin) notif.showAdminPostNotification(profile.username, postContent)
                    else if (notifyPosts.value) notif.showPostNotification(profile.username, postContent)
                    handleMentions(postContent, currentUserId, displayName, fromAdmin, postId)
                }
            }.onFailure { error.value = it.message }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    // Messages avec pseudo résolu
    private val _rawMessages  = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = combine(_rawMessages, _profilesMap) { msgs, profiles ->
        msgs.map { msg ->
            val live = profiles[msg.senderId]?.username
            if (live != null && live != msg.senderUsername) msg.copy(senderUsername = live) else msg
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _allBadges = MutableStateFlow<List<Badge>>(emptyList())
    val allBadges: StateFlow<List<Badge>> = _allBadges

    private val _myBadges = MutableStateFlow<List<Badge>>(emptyList())
    val myBadges: StateFlow<List<Badge>> = _myBadges

    private val _myAchievements     = MutableStateFlow<List<Achievement>>(emptyList())
    val myAchievements: StateFlow<List<Achievement>> = _myAchievements

    private val _viewedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val viewedAchievements: StateFlow<List<Achievement>> = _viewedAchievements

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    val unreadNotifCount: StateFlow<Int> = _notifications
        .map { it.count { n -> !n.isRead } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Settings ──────────────────────────────────────────────────────────────

    val notifyMessages   = settings.notifyMessages.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifyPosts      = settings.notifyPosts.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val notifyMentions   = settings.notifyMentions.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val totalBytesStored = settings.totalBytes.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val error     = MutableStateFlow<String?>(null)
    val loading   = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)

    private var postsJob:    Job? = null
    private var storiesJob:  Job? = null
    private var convJob:     Job? = null
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
        listenAllBadges()
        listenNotifications()
    }

    private fun stopAll() {
        listOf(postsJob, storiesJob, convJob, msgJob, commentJob,
            badgesJob, profileJob, profilesJob, notifJob).forEach { it?.cancel() }
        _allPosts.value = emptyList()
        _stories.value  = emptyList()
        _rawConversations.value = emptyList()
        _rawMessages.value = emptyList()
        _myProfile.value = null
        _profilesMap.value = emptyMap()
        _allBadges.value = emptyList()
        _myBadges.value  = emptyList()
        _notifications.value = emptyList()
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
        "already" in msg.lowercase()  -> "Email déjà utilisé"
        "no user" in msg.lowercase()  -> "Aucun compte trouvé"
        "network" in msg.lowercase()  -> "Vérifiez votre connexion"
        "invalid" in msg.lowercase()  -> "Email ou mot de passe invalide"
        "weak" in msg.lowercase()     -> "Mot de passe trop faible (6 min)"
        else -> msg
    }

    // ── PROFILES ─────────────────────────────────────────────────────────────

    private fun listenMyProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            repo.listenToProfile(currentUserId).collect { profile ->
                _myProfile.value = profile
                profile?.let {
                    _myBadges.value = _allBadges.value.filter { b -> b.id in it.badgeIds }
                    _profilesMap.value = _profilesMap.value + (it.userId to it)
                    // ← Fix : vérifie les achievements à chaque mise à jour du profil
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
                _profilesMap.value = profiles.associateBy { it.userId }
                _myProfile.value?.let { me ->
                    _myBadges.value = _allBadges.value.filter { b -> b.id in me.badgeIds }
                }
            }
        }
    }

    fun loadProfile(uid: String) = viewModelScope.launch {
        // D'abord depuis le cache (instantané)
        _viewedProfile.value = _profilesMap.value[uid]
        // Puis depuis Firestore pour avoir les données fraîches
        repo.getProfile(uid)?.let { _viewedProfile.value = it }
        _viewedAchievements.value = repo.getAchievements(uid)
    }

    /**
     * Résout le username actuel d'un userId depuis le cache local.
     * Utilisé partout dans l'UI pour afficher le bon pseudo.
     */
    fun resolveUsername(userId: String, fallback: String): String =
        _profilesMap.value[userId]?.username ?: fallback

    fun updateProfile(data: Map<String, Any?>, onDone: (() -> Unit)? = null) =
        viewModelScope.launch {
            runCatching {
                val oldUsername  = _myProfile.value?.username ?: ""
                val newUsername  = (data["username"] as? String)?.trim() ?: ""
                val newMoodEmoji = (data["moodEmoji"] as? String) ?: ""

                repo.updateProfile(currentUserId, data)

                if (newUsername.isNotBlank() && newUsername != oldUsername) {
                    isSyncing.value = true
                    launch(Dispatchers.IO) {
                        repo.syncUsername(currentUserId, newUsername)
                        isSyncing.value = false
                    }
                }
                if (newMoodEmoji.isNotBlank()) {
                    repo.getProfile(currentUserId)?.let { checkAchievements(it) }
                }
                onDone?.invoke()
            }.onFailure {
                isSyncing.value = false
                error.value = it.message
            }
        }

    // ── ACHIEVEMENTS ──────────────────────────────────────────────────────────

    private fun checkAchievements(profile: UserProfile) = viewModelScope.launch {
        val uid      = currentUserId
        // Recharge depuis Firestore pour avoir l'état le plus frais
        val fetched  = repo.getAchievements(uid)
        _myAchievements.value = fetched
        val unlocked = fetched.map { it.id }.toSet()

        suspend fun unlock(id: String) {
            if (id !in unlocked) {
                repo.unlockAchievement(uid, id)
                // Mise à jour locale immédiate
                _myAchievements.value = _myAchievements.value +
                        Achievement(id = id, unlockedAt = System.currentTimeMillis())
            }
        }

        // Poster
        if (profile.postsCount >= 1)  unlock("first_post")
        if (profile.postsCount >= 10) unlock("ten_posts")
        if (profile.postsCount >= 25) unlock("twenty_five_p")
        if (profile.postsCount >= 50) unlock("fifty_posts")

        // Commenter
        if (profile.commentsCount >= 1)  unlock("first_comment")
        if (profile.commentsCount >= 20) unlock("commentator")
        if (profile.commentsCount >= 50) unlock("deep_comment")

        // Confessions
        if (profile.confessionsCount >= 1) unlock("confessor")
        if (profile.confessionsCount >= 5) unlock("dark_confessor")

        // Sondages
        if (profile.pollsCount >= 3)  unlock("poll_creator")
        if (profile.pollsCount >= 10) unlock("poll_master")

        // Stories
        if (profile.storiesCount >= 1)  unlock("first_story")
        if (profile.storiesCount >= 10) unlock("storyteller")

        // Social
        if (profile.convsStarted >= 5)  unlock("social")
        if (profile.convsStarted >= 20) unlock("social_plus")

        // Streak
        if (profile.streak >= 3)  unlock("streak_3")
        if (profile.streak >= 7)  unlock("streak_7")
        if (profile.streak >= 30) unlock("streak_30")

        // Identité
        if (profile.hasBadgeENI)                unlock("eni_pride")
        if (profile.badgeIds.isNotEmpty())       unlock("badge_maker")
        if (profile.moodEmoji.isNotBlank())      unlock("mood_master")
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
                if (trimmed.isBlank()) return@launch onError("Le nom ne peut pas être vide")
                if (trimmed.lowercase() == ADMIN_BADGE_NAME) return@launch onError("Ce nom est réservé")
                val existing = repo.findBadgeByName(trimmed)
                if (existing != null) {
                    if (_myBadges.value.any { it.id == existing.id }) return@launch onError("Tu portes déjà ce badge !")
                    if (!userIsAdmin && _myBadges.value.isNotEmpty()) return@launch onError("Retire ton badge actuel d'abord")
                    repo.wearBadge(existing.id, currentUserId)
                } else {
                    if (!userIsAdmin && _myBadges.value.isNotEmpty()) return@launch onError("Retire ton badge actuel d'abord")
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
                if (_myBadges.value.any { it.id == badgeId }) return@launch onError("Tu portes déjà ce badge !")
                if (!userIsAdmin && _myBadges.value.isNotEmpty()) return@launch onError("Retire ton badge actuel avant")
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
                if (!userIsAdmin && badge.createdBy != currentUserId)
                    return@launch onError("Tu ne peux modifier que tes badges")
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
                if (!userIsAdmin && badge.createdBy != currentUserId)
                    return@launch onError("Tu ne peux supprimer que tes badges")
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
                .catch { }
                .collect { notifs ->
                    _notifications.value = notifs.map { n ->
                        val live = _profilesMap.value[n.fromUserId]?.username
                        if (live != null && live != n.fromUsername) n.copy(fromUsername = live) else n
                    }
                }
        }
    }

    fun markNotificationRead(notifId: String) = viewModelScope.launch {
        _notifications.value = _notifications.value.map { n ->
            if (n.id == notifId) n.copy(isRead = true) else n
        }
        repo.markNotificationRead(notifId)
    }

    fun markAllNotificationsRead() = viewModelScope.launch {
        _notifications.value = _notifications.value.map { n ->
            if (!n.isRead) n.copy(isRead = true) else n
        }
        repo.markAllNotificationsRead(currentUserId)
    }

    fun deleteNotification(notifId: String) = viewModelScope.launch {
        repo.deleteNotification(notifId)
    }

    private suspend fun handleMentions(
        text: String, fromUserId: String, fromUsername: String,
        fromAdmin: Boolean, postId: String = "", convId: String = ""
    ) {
        val now = System.currentTimeMillis()

        if (fromAdmin && Regex("@(everyone|tout_le_monde|tous)", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            val base = mapOf(
                "type" to "mention_everyone", "fromUserId" to fromUserId,
                "fromUsername" to fromUsername, "fromIsAdmin" to true,
                "postId" to postId, "conversationId" to convId,
                "content" to text.take(150), "isRead" to false, "timestamp" to now
            )
            repo.createNotificationsForAll(base, fromUserId)
            notif.showEveryoneMentionNotification(fromUsername, text)
            return
        }

        extractMentions(text).forEach { username ->
            val targetProfile = _profilesMap.value.values.find { it.username == username }
                ?: repo.findProfileByUsername(username)
                ?: return@forEach
            if (targetProfile.userId == fromUserId) return@forEach

            repo.createNotification(mapOf(
                "targetUserId" to targetProfile.userId, "type" to "mention",
                "fromUserId" to fromUserId, "fromUsername" to fromUsername,
                "fromIsAdmin" to fromAdmin, "postId" to postId,
                "conversationId" to convId, "content" to text.take(150),
                "isRead" to false, "timestamp" to now
            ))
            if (targetProfile.userId == currentUserId && (fromAdmin || notifyMentions.value)) {
                notif.showMentionNotification(fromUsername, text, fromAdmin)
            }
        }
    }

    private fun extractMentions(text: String): List<String> =
        Regex("@([\\w]+)").findAll(text)
            .map { it.groupValues[1] }
            .filter { it.lowercase() !in setOf("everyone", "tout_le_monde", "tous") }
            .distinct().toList()

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

    /**
     * Crée un post texte seul (utilisé pour les confessions via dialog rapide).
     */
    fun createPost(
        content: String,
        type: String = "normal",
        pollOpt1: String = "",
        pollOpt2: String = ""
    ) {
        createPostWithMedia(content, type, pollOpt1, pollOpt2, null, null)
    }

    /**
     * Crée un post avec ou sans média — fonction unifiée.
     */


    // ── deletePost : décrémente le compteur ───────────────────────────────────
    fun deletePost(postId: String) = viewModelScope.launch {
        runCatching {
            val post = _allPosts.value.find { it.id == postId }
            repo.deletePost(postId)
            // ← Fix : décrémente le compteur du bon auteur
            post?.let {
                val field = when (it.postType) {
                    "confession" -> "confessionsCount"; "poll" -> "pollsCount"; else -> "postsCount"
                }
                repo.incrementCounter(it.userId, field, -1L)
            }
        }
    }

    // In AskipViewModel.kt

    fun togglePin(post: Post) {
        val newState = !post.isPinned
        // On utilise repo.db car c'est là que l'instance Firestore est définie
        repo.db.collection("posts").document(post.id)
            .update("isPinned", newState)
            .addOnFailureListener { e ->
                // Optionnel : logger l'erreur
                Log.e("AskipViewModel", "Erreur pin: ${e.message}")
            }
    }

    // ── toggleReaction : update optimiste immédiat ────────────────────────────
    fun toggleReaction(post: Post, emoji: String) {
        val uid     = currentUserId
        val current = post.getUserReaction(uid)
        // Mise à jour optimiste
        _allPosts.value = _allPosts.value.map { p ->
            if (p.id != post.id) return@map p
            val removed = when (current) {
                "❤️" -> p.copy(likedBy = p.likedBy - uid); "🔥" -> p.copy(fireBy  = p.fireBy  - uid)
                "😂" -> p.copy(lolBy   = p.lolBy   - uid); "😱" -> p.copy(shockBy = p.shockBy - uid)
                "👀" -> p.copy(eyesBy  = p.eyesBy  - uid); else -> p
            }
            if (current == emoji) removed
            else when (emoji) {
                "❤️" -> removed.copy(likedBy = removed.likedBy + uid); "🔥" -> removed.copy(fireBy  = removed.fireBy  + uid)
                "😂" -> removed.copy(lolBy   = removed.lolBy   + uid); "😱" -> removed.copy(shockBy = removed.shockBy + uid)
                "👀" -> removed.copy(eyesBy  = removed.eyesBy  + uid); else -> removed
            }
        }
        viewModelScope.launch {
            runCatching {
                if (current == emoji) repo.removeReaction(post.id, uid, emoji)
                else repo.addReaction(
                    post.id, uid, emoji, current,
                    postOwnerId = TODO()
                )
            }
        }
    }

    // ── Photo de profil : résolution URL depuis le cache ──────────────────────
    fun resolvePhotoUrl(userId: String): String =
        _profilesMap.value[userId]?.photoUrl ?: ""

    // ── Suppression photo de profil ───────────────────────────────────────────
    fun deleteProfilePhoto() = viewModelScope.launch {
        runCatching {
            repo.updateProfile(currentUserId, mapOf("photoUrl" to ""))
        }.onFailure { error.value = it.message }
    }

    // ── Suppression couverture ────────────────────────────────────────────────
    fun deleteCoverPhoto() = viewModelScope.launch {
        runCatching {
            repo.updateProfile(currentUserId, mapOf("coverUrl" to ""))
        }.onFailure { error.value = it.message }
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
                // Résout les pseudos dans les stories
                _stories.value = rawStories.map { s ->
                    val liveUsername = _profilesMap.value[s.userId]?.username
                    if (liveUsername != null && liveUsername != s.username)
                        s.copy(username = liveUsername)
                    else s
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
            repo.deleteStory(storyId)
            // Décrémente le compteur
            val story = _stories.value.find { it.id == storyId }
            story?.let { repo.incrementCounter(it.userId, "storiesCount", -1L) }
        }
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
                // Utilise le pseudo courant de l'autre user depuis le cache
                val liveOtherUsername = resolveUsername(otherId, otherUsername)
                val convId = repo.getOrCreateConversation(
                    currentUserId, me.username, otherId, liveOtherUsername
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
                            val liveUsername = resolveUsername(msg.senderId, msg.senderUsername)
                            notif.showMessageNotification(liveUsername, msg.content, fromAdmin)
                        }
                    }
                }
                _rawMessages.value = newMsgs
            }
        }
    }

    fun sendMessage(convId: String, content: String) {
        val profile    = _myProfile.value ?: return
        val conv       = _rawConversations.value.find { it.id == convId } ?: return
        val receiverId = conv.participants.firstOrNull { it != currentUserId } ?: return
        val fromAdmin  = isAdmin(currentUserId) || profile.isAdmin

        viewModelScope.launch {
            runCatching {
                repo.sendMessage(convId, mapOf(
                    "conversationId" to convId,
                    "senderId"       to currentUserId,
                    "senderUsername" to profile.username,
                    "content"        to content,
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
                    "content"        to content.take(100),
                    "isRead"         to false,
                    "timestamp"      to System.currentTimeMillis()
                ))
            }
        }
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
            loading.value = true; _uploadProgress.value = 0
            runCatching {
                var mediaUrl = ""; var mediaType = ""; var mediaName = ""; var mediaDuration = 0

                when {
                    imageUri != null -> {
                        mediaUrl  = CloudinaryUploader.uploadImage(getApplication(), imageUri) { _uploadProgress.value = it }
                        mediaType = "image"
                    }
                    videoUri != null -> {
                        mediaUrl  = CloudinaryUploader.uploadVideo(getApplication(), videoUri) { _uploadProgress.value = it }
                        mediaType = "video"
                    }
                    fileUri != null -> {
                        val (url, name) = CloudinaryUploader.uploadFile(getApplication(), fileUri) { _uploadProgress.value = it }
                        mediaUrl  = url; mediaType = "file"; mediaName = name
                    }
                }

                val notifContent = when (mediaType) {
                    "image" -> "📸 Photo"; "video" -> "🎥 Vidéo"
                    "file"  -> "📎 $mediaName"; "audio" -> "🎤 Message vocal"
                    else    -> content.take(100)
                }

                repo.sendMessage(convId, mapOf(
                    "conversationId" to convId, "senderId" to currentUserId,
                    "senderUsername" to profile.username, "content" to content,
                    "mediaUrl" to mediaUrl, "mediaType" to mediaType,
                    "mediaName" to mediaName, "mediaDuration" to mediaDuration,
                    "timestamp" to System.currentTimeMillis()
                ), receiverId)

                repo.createNotification(mapOf(
                    "targetUserId" to receiverId, "type" to "message",
                    "fromUserId" to currentUserId, "fromUsername" to profile.username,
                    "fromIsAdmin" to fromAdmin, "postId" to "", "conversationId" to convId,
                    "content" to notifContent, "isRead" to false,
                    "timestamp" to System.currentTimeMillis()
                ))
            }.onFailure { error.value = it.message }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    fun startVoiceRecording(context: Context) {
        if (_isRecording.value) return
        voiceRecorder = VoiceRecorder(context)
        runCatching { voiceRecorder?.start() }
            .onSuccess {
                _isRecording.value = true; _recordingSeconds.value = 0
                recordingTimerJob = viewModelScope.launch {
                    while (isActive) {
                        delay(1000L)
                        _recordingSeconds.value++
                        if (_recordingSeconds.value >= 120) stopAndSendVoice("") // sécurité 2min
                    }
                }
            }
            .onFailure { error.value = "Micro non disponible"; voiceRecorder = null }
    }

    fun stopAndSendVoice(convId: String) {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null
        _isRecording.value = false

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
                    "conversationId" to convId, "senderId" to currentUserId,
                    "senderUsername" to profile.username, "content" to "",
                    "mediaUrl" to url, "mediaType" to "audio",
                    "mediaName" to "vocal_${durationSec}s.m4a", "mediaDuration" to durationSec,
                    "timestamp" to System.currentTimeMillis()
                ), receiverId)
                repo.createNotification(mapOf(
                    "targetUserId" to receiverId, "type" to "message",
                    "fromUserId" to currentUserId, "fromUsername" to profile.username,
                    "fromIsAdmin" to fromAdmin, "postId" to "", "conversationId" to convId,
                    "content" to "🎤 Message vocal (${durationSec}s)",
                    "isRead" to false, "timestamp" to System.currentTimeMillis()
                ))
            }.onFailure { error.value = it.message; file.delete() }
            loading.value = false; _uploadProgress.value = 0
        }
    }

    fun stopRecordingForPost(): Pair<File, Int>? {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null
        _isRecording.value    = false
        _recordingSeconds.value = 0
        return result
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        voiceRecorder?.cancel()
        voiceRecorder = null
        _isRecording.value = false
        _recordingSeconds.value = 0
    }

    fun markRead(convId: String) = viewModelScope.launch {
        repo.markRead(convId, currentUserId)
    }

    fun getUnread(conv: Conversation) = (conv.unreadCounts[currentUserId] ?: 0L).toInt()

    // ── SETTINGS ──────────────────────────────────────────────────────────────

    fun setNotifyMessages(v: Boolean) = viewModelScope.launch { settings.setNotifyMessages(v) }
    fun setNotifyPosts(v: Boolean)    = viewModelScope.launch { settings.setNotifyPosts(v) }
    fun setNotifyMentions(v: Boolean) = viewModelScope.launch { settings.setNotifyMentions(v) }
    fun clearError() { error.value = null }
}