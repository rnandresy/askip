package com.rnandresy.lol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.FeedRepository
import com.rnandresy.lol.repository.NotificationRepository
import com.rnandresy.lol.repository.ProfileRepository
import com.rnandresy.lol.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Commentaires d'un post précis.
 * Créé à l'ouverture de l'écran, détruit à la fermeture —
 * un seul listener Firestore actif à la fois.
 */
class CommentViewModel(
    private val postId: String,
    private val currentUid: String,
    private val currentUsername: String,
    private val currentPhotoUrl: String
) : ViewModel() {

    private val repo        = FeedRepository()
    private val profileRepo = ProfileRepository()
    private val notifRepo   = NotificationRepository()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _state = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val state: StateFlow<UiState<Unit>> = _state.asStateFlow()

    private val _action = MutableStateFlow<ActionState>(ActionState.Idle)
    val action: StateFlow<ActionState> = _action.asStateFlow()

    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles.asStateFlow()

    /** Commentaire auquel on répond (threads). */
    private val _replyingTo = MutableStateFlow<Comment?>(null)
    val replyingTo: StateFlow<Comment?> = _replyingTo.asStateFlow()

    /** Commentaires racine, avec leurs réponses regroupées. */
    val threads: StateFlow<List<CommentThread>> = _comments.map { list ->
        val roots   = list.filter { it.parentId.isBlank() }
        val replies = list.filter { it.parentId.isNotBlank() }.groupBy { it.parentId }
        roots.map { root ->
            CommentThread(
                root    = root,
                replies = (replies[root.id] ?: emptyList()).sortedBy { it.timestamp }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observePost()
        observeComments()
        loadProfiles()
    }

    private fun observePost() = viewModelScope.launch {
        repo.listenToPost(postId).collect { _post.value = it }
    }

    private fun observeComments() = viewModelScope.launch {
        repo.listenToComments(postId).collect { list ->
            _comments.value = list
            _state.value = if (list.isEmpty())
                UiState.Empty("Aucun commentaire. Sois le premier !")
            else UiState.Success(Unit)
        }
    }

    private fun loadProfiles() = viewModelScope.launch {
        runCatching { profileRepo.getAllProfiles(limit = PAGE_SIZE_MEMBERS) }
            .onSuccess { _allProfiles.value = it }
    }

    fun sendComment(content: String, isAnonymous: Boolean = false) = viewModelScope.launch {
        val clean = InputValidator.sanitize(content)
        InputValidator.comment(clean)?.let {
            _action.value = ActionState.Error(it); return@launch
        }
        RateLimiter.check("comment", MIN_MS_BETWEEN_COMMENTS)?.let {
            _action.value = ActionState.Error(it); return@launch
        }

        val parent = _replyingTo.value
        _action.value = ActionState.Loading()

        runCatching {
            repo.addComment(
                postId       = postId,
                userId       = currentUid,
                username     = if (isAnonymous) "" else currentUsername,
                userPhotoUrl = if (isAnonymous) "" else currentPhotoUrl,
                content      = clean,
                isAnonymous  = isAnonymous,
                parentId     = parent?.id ?: ""
            )
            if (!isAnonymous) {
                notifRepo.notifyMentions(
                    content       = clean,
                    fromUid       = currentUid,
                    fromName      = currentUsername,
                    postId        = postId,
                    profiles      = _allProfiles.value,
                    senderIsAdmin = isAdmin(currentUid)
                )
                // Notifie l'auteur du commentaire parent
                parent?.let {
                    if (it.userId != currentUid && !it.isAnonymous) {
                        notifRepo.notifyReply(
                            targetUid = it.userId,
                            fromUid   = currentUid,
                            fromName  = currentUsername,
                            postId    = postId,
                            preview   = clean.take(60)
                        )
                    }
                }
            }
        }.onSuccess {
            _replyingTo.value = null
            _action.value     = ActionState.Success()
        }.onFailure {
            _action.value = ActionState.Error(it.toFrenchMessage())
        }
    }

    fun toggleCommentReaction(comment: Comment, emoji: String) = viewModelScope.launch {
        val previous = comment.getUserReaction(currentUid)
        val next     = if (previous == emoji) null else emoji

        _comments.value = _comments.value.map {
            if (it.id == comment.id) it.withReaction(currentUid, next) else it
        }
        runCatching { repo.setCommentReaction(postId, comment.id, currentUid, next) }
            .onFailure {
                _comments.value = _comments.value.map {
                    if (it.id == comment.id) it.withReaction(currentUid, previous) else it
                }
            }
    }

    fun deleteComment(comment: Comment) = viewModelScope.launch {
        if (comment.userId != currentUid && !isAdmin(currentUid)) {
            _action.value = ActionState.Error("Action non autorisée")
            return@launch
        }
        runCatching { repo.deleteComment(postId, comment.id) }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun startReply(comment: Comment) { _replyingTo.value = comment }
    fun cancelReply()                { _replyingTo.value = null }
    fun clearAction()                { _action.value = ActionState.Idle }

    class Factory(
        private val postId: String,
        private val uid: String,
        private val username: String,
        private val photoUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CommentViewModel(postId, uid, username, photoUrl) as T
    }
}

data class CommentThread(
    val root: Comment,
    val replies: List<Comment> = emptyList()
) {
    val replyCount: Int get() = replies.size
}