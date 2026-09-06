package com.rnandresy.lol.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.FeedRepository
import com.rnandresy.lol.repository.NotificationRepository
import com.rnandresy.lol.repository.ProfileRepository
import com.rnandresy.lol.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FeedFilter(
    val showConfessions: Boolean = false,   // false = feed normal, true = confessions
    val searchQuery: String = ""
)

/**
 * Feed : posts paginés, stories, réactions, sondages, création.
 * La pagination évite de charger 500 posts d'un coup.
 */
class FeedViewModel(
    app: Application,
    private val currentUid: String,
    private val currentUsername: String,
    private val currentPhotoUrl: String
) : AndroidViewModel(app) {

    private val repo        = FeedRepository()
    private val profileRepo = ProfileRepository()
    private val notifRepo   = NotificationRepository()
    private val settings    = SettingsRepository(app)

    // ── Feed paginé ───────────────────────────────────────────────────────────
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _feedState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val feedState: StateFlow<UiState<Unit>> = _feedState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── Confessions (chargées séparément) ─────────────────────────────────────
    private val _confessions = MutableStateFlow<List<Post>>(emptyList())
    val confessions: StateFlow<List<Post>> = _confessions.asStateFlow()

    // ── Stories ───────────────────────────────────────────────────────────────
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    // ── Profils (pour les mentions) ───────────────────────────────────────────
    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles.asStateFlow()

    // ── Recherche ─────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val visiblePosts: StateFlow<List<Post>> = combine(
        _posts, _searchQuery
    ) { list, q ->
        if (q.isBlank()) list
        else list.filter {
            it.content.contains(q, ignoreCase = true) ||
                    (!it.isAnonymous && it.username.contains(q, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Action en cours ───────────────────────────────────────────────────────
    private val _action = MutableStateFlow<ActionState>(ActionState.Idle)
    val action: StateFlow<ActionState> = _action.asStateFlow()

    // ── Brouillon ─────────────────────────────────────────────────────────────
    val draft: StateFlow<String> = settings.postDraft
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        loadFirstPage()
        observeStories()
        observeProfiles()
    }

    // ── Chargement paginé ─────────────────────────────────────────────────────
    fun loadFirstPage() = viewModelScope.launch {
        _feedState.value = UiState.Loading
        runCatching { repo.getPostsPage(limit = PAGE_SIZE_FEED, after = null) }
            .onSuccess { page ->
                _posts.value    = page
                _hasMore.value  = page.size >= PAGE_SIZE_FEED
                _feedState.value = if (page.isEmpty())
                    UiState.Empty("Aucun post pour le moment")
                else UiState.Success(Unit)
            }
            .onFailure {
                _feedState.value = UiState.Error(it.toFrenchMessage())
            }
    }

    fun loadMore() = viewModelScope.launch {
        if (_isLoadingMore.value || !_hasMore.value) return@launch
        val last = _posts.value.lastOrNull() ?: return@launch

        _isLoadingMore.value = true
        runCatching { repo.getPostsPage(limit = PAGE_SIZE_FEED, after = last) }
            .onSuccess { page ->
                if (page.isEmpty()) {
                    _hasMore.value = false
                } else {
                    // Déduplication par id — protège contre les doublons
                    val existing = _posts.value.map { it.id }.toSet()
                    _posts.value = _posts.value + page.filter { it.id !in existing }
                    _hasMore.value = page.size >= PAGE_SIZE_FEED
                }
            }
        _isLoadingMore.value = false
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        runCatching { repo.getPostsPage(limit = PAGE_SIZE_FEED, after = null) }
            .onSuccess { page ->
                _posts.value   = page
                _hasMore.value = page.size >= PAGE_SIZE_FEED
                _feedState.value = if (page.isEmpty())
                    UiState.Empty("Aucun post pour le moment")
                else UiState.Success(Unit)
            }
        _isRefreshing.value = false
    }

    fun loadConfessions() = viewModelScope.launch {
        runCatching { repo.getConfessions(limit = PAGE_SIZE_FEED) }
            .onSuccess { _confessions.value = it }
    }

    private fun observeStories() = viewModelScope.launch {
        repo.listenToActiveStories().collect { _stories.value = it }
    }

    private fun observeProfiles() = viewModelScope.launch {
        profileRepo.listenToAllProfiles(limit = PAGE_SIZE_MEMBERS)
            .collect { _allProfiles.value = it }
    }

    // ── Création de post ──────────────────────────────────────────────────────
    fun createPost(
        content: String,
        type: String = POST_TYPE_NORMAL,
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        pollOption1: String = "",
        pollOption2: String = ""
    ) = viewModelScope.launch {

        val clean = InputValidator.sanitize(content)
        InputValidator.postContent(clean)?.let {
            if (imageUri == null && videoUri == null) {
                _action.value = ActionState.Error(it); return@launch
            }
        }

        RateLimiter.check("post", MIN_MS_BETWEEN_POSTS)?.let {
            _action.value = ActionState.Error(it); return@launch
        }
        RateLimiter.checkHourly("post_hourly", MAX_POSTS_PER_HOUR)?.let {
            _action.value = ActionState.Error(it); return@launch
        }

        _action.value = ActionState.Loading(0)
        runCatching {
            val ctx = getApplication<Application>()

            var imageUrl = ""
            var savedKb  = 0
            imageUri?.let {
                val res = CloudinaryUploader.uploadImage(ctx, it) { pct ->
                    _action.value = ActionState.Loading(pct)
                }
                imageUrl = res.url
                savedKb  = res.savedKb
                settings.addBytes(res.bytesUploaded.toLong())
            }

            var videoUrl = ""
            videoUri?.let {
                val res = CloudinaryUploader.uploadVideo(ctx, it) { pct ->
                    _action.value = ActionState.Loading(pct)
                }
                videoUrl = res.url
                settings.addBytes(res.bytesUploaded.toLong())
            }

            val isConfession = type == POST_TYPE_CONFESSION
            val postId = repo.createPost(
                userId       = currentUid,
                username     = if (isConfession) "" else currentUsername,
                userPhotoUrl = if (isConfession) "" else currentPhotoUrl,
                content      = clean,
                postType     = type,
                imageUrl     = imageUrl,
                videoUrl     = videoUrl,
                isAnonymous  = isConfession,
                pollOption1  = pollOption1.trim(),
                pollOption2  = pollOption2.trim()
            )

            // Notifications de mention
            if (!isConfession) {
                notifRepo.notifyMentions(
                    content    = clean,
                    fromUid    = currentUid,
                    fromName   = currentUsername,
                    postId     = postId,
                    profiles   = _allProfiles.value,
                    senderIsAdmin = isAdmin(currentUid)
                )
            }

            settings.clearPostDraft()
            savedKb
        }.onSuccess { saved ->
            _action.value = ActionState.Success(
                if (saved > 0) "Publié ✓ ($saved Ko économisés)" else "Publié ✓"
            )
            refresh()
        }.onFailure {
            _action.value = ActionState.Error(it.toFrenchMessage())
        }
    }

    // ── Édition ───────────────────────────────────────────────────────────────
    fun editPost(post: Post, newContent: String) = viewModelScope.launch {
        val clean = InputValidator.sanitize(newContent)
        InputValidator.postContent(clean)?.let {
            _action.value = ActionState.Error(it); return@launch
        }
        if (post.userId != currentUid) {
            _action.value = ActionState.Error("Vous ne pouvez modifier que vos posts")
            return@launch
        }
        runCatching { repo.editPost(post.id, clean) }
            .onSuccess {
                _posts.value = _posts.value.map {
                    if (it.id == post.id) it.copy(content = clean, isEdited = true) else it
                }
                _action.value = ActionState.Success("Post modifié ✓")
            }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    // ── Réactions (optimiste — pas d'attente réseau) ──────────────────────────
    fun toggleReaction(post: Post, emoji: String) = viewModelScope.launch {
        val previous = post.getUserReaction(currentUid)
        val newEmoji = if (previous == emoji) null else emoji

        // Mise à jour immédiate en local
        _posts.value = _posts.value.map { p ->
            if (p.id == post.id) p.withReaction(currentUid, newEmoji) else p
        }
        _confessions.value = _confessions.value.map { p ->
            if (p.id == post.id) p.withReaction(currentUid, newEmoji) else p
        }

        // Puis synchro serveur — rollback si échec
        runCatching { repo.setReaction(post.id, currentUid, newEmoji) }
            .onFailure {
                _posts.value = _posts.value.map { p ->
                    if (p.id == post.id) p.withReaction(currentUid, previous) else p
                }
                _action.value = ActionState.Error("Réaction non enregistrée")
            }
    }

    // ── Sondage ───────────────────────────────────────────────────────────────
    fun votePoll(post: Post, option: Int) = viewModelScope.launch {
        if (currentUid in post.pollVoters) return@launch

        _posts.value = _posts.value.map { p ->
            if (p.id == post.id) p.withVote(currentUid, option) else p
        }
        runCatching { repo.votePoll(post.id, currentUid, option) }
            .onFailure {
                _action.value = ActionState.Error("Vote non enregistré")
                refresh()
            }
    }

    // ── Modération ────────────────────────────────────────────────────────────
    fun togglePin(post: Post) = viewModelScope.launch {
        if (!isAdmin(currentUid)) return@launch
        runCatching { repo.setPinned(post.id, !post.isPinned) }
            .onSuccess { refresh() }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun deletePost(post: Post) = viewModelScope.launch {
        if (post.userId != currentUid && !isAdmin(currentUid)) {
            _action.value = ActionState.Error("Action non autorisée")
            return@launch
        }
        _posts.value       = _posts.value.filterNot { it.id == post.id }
        _confessions.value = _confessions.value.filterNot { it.id == post.id }

        runCatching { repo.deletePost(post.id) }
            .onSuccess { _action.value = ActionState.Success("Post supprimé") }
            .onFailure {
                _action.value = ActionState.Error(it.toFrenchMessage())
                refresh()
            }
    }

    fun reportPost(post: Post, reason: String, details: String = "") = viewModelScope.launch {
        runCatching {
            repo.reportPost(
                postId     = post.id,
                reporterId = currentUid,
                reason     = reason,
                details    = details.trim()
            )
        }.onSuccess {
            _action.value = ActionState.Success("Signalement envoyé. Merci.")
        }.onFailure {
            _action.value = ActionState.Error(it.toFrenchMessage())
        }
    }

    // ── Stories ───────────────────────────────────────────────────────────────
    fun createStory(content: String, emoji: String, colorHex: String) =
        viewModelScope.launch {
            val clean = InputValidator.sanitize(content)
            if (clean.isBlank()) {
                _action.value = ActionState.Error("La story ne peut pas être vide")
                return@launch
            }
            RateLimiter.check("story", MIN_MS_BETWEEN_POSTS)?.let {
                _action.value = ActionState.Error(it); return@launch
            }
            runCatching {
                repo.createStory(
                    userId       = currentUid,
                    username     = currentUsername,
                    userPhotoUrl = currentPhotoUrl,
                    content      = clean.take(MAX_STORY_LENGTH),
                    emoji        = emoji,
                    colorHex     = colorHex
                )
            }.onSuccess { _action.value = ActionState.Success("Story publiée ✓") }
                .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
        }

    fun deleteStory(storyId: String) = viewModelScope.launch {
        runCatching { repo.deleteStory(storyId) }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    // ── Brouillon ─────────────────────────────────────────────────────────────
    fun saveDraft(text: String) = viewModelScope.launch { settings.setPostDraft(text) }
    fun clearDraft()            = viewModelScope.launch { settings.clearPostDraft() }

    fun setSearch(q: String) { _searchQuery.value = q }
    fun clearAction()        { _action.value = ActionState.Idle }

    class Factory(
        private val app: Application,
        private val uid: String,
        private val username: String,
        private val photoUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FeedViewModel(app, uid, username, photoUrl) as T
    }
}