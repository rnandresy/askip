package com.rnandresy.lol.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.FeedRepository
import com.rnandresy.lol.repository.ProfileRepository
import com.rnandresy.lol.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Profils : le sien et ceux des autres, liste des membres,
 * achievements, recherche.
 */
class `ProfileViewModel.kt`(
    app: Application,
    private val currentUid: String
) : AndroidViewModel(app) {

    private val repo     = ProfileRepository()
    private val feedRepo = FeedRepository()
    private val settings = SettingsRepository(app)

    // ── Profil affiché ────────────────────────────────────────────────────────
    private val _viewedProfile = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile.asStateFlow()

    private val _viewedPosts = MutableStateFlow<List<Post>>(emptyList())
    val viewedPosts: StateFlow<List<Post>> = _viewedPosts.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val profileState: StateFlow<UiState<Unit>> = _profileState.asStateFlow()

    // ── Membres ───────────────────────────────────────────────────────────────
    private val _members = MutableStateFlow<List<UserProfile>>(emptyList())
    val members: StateFlow<List<UserProfile>> = _members.asStateFlow()

    private val _memberSearch = MutableStateFlow("")
    val memberSearch: StateFlow<String> = _memberSearch.asStateFlow()

    val filteredMembers: StateFlow<List<UserProfile>> = combine(
        _members, _memberSearch
    ) { list, q ->
        val base = if (q.isBlank()) list
        else list.filter {
            it.username.contains(q, true) ||
                    it.classeENI.contains(q, true) ||
                    it.bio.contains(q, true)
        }
        base.sortedWith(
            compareByDescending<UserProfile> { isAdmin(it.userId) || it.isAdmin }
                .thenByDescending { it.hasBadgeENI }
                .thenBy { it.username.lowercase() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _action = MutableStateFlow<ActionState>(ActionState.Idle)
    val action: StateFlow<ActionState> = _action.asStateFlow()

    init { observeMembers() }

    private fun observeMembers() = viewModelScope.launch {
        repo.listenToAllProfiles(limit = PAGE_SIZE_MEMBERS)
            .collect { _members.value = it }
    }

    // ── Ouvrir un profil ──────────────────────────────────────────────────────
    fun loadProfile(uid: String) = viewModelScope.launch {
        _profileState.value = UiState.Loading
        _viewedProfile.value = null
        _viewedPosts.value   = emptyList()

        launch {
            repo.listenToProfile(uid).collect { profile ->
                _viewedProfile.value = profile
                _profileState.value  = if (profile == null)
                    UiState.Error("Profil introuvable", retryable = false)
                else UiState.Success(Unit)
            }
        }
        launch {
            runCatching { feedRepo.getPostsByUser(uid, limit = 20) }
                .onSuccess { _viewedPosts.value = it }
        }
    }

    // ── Mise à jour du profil ─────────────────────────────────────────────────
    fun updateProfile(
        username: String,
        bio: String,
        classeENI: String,
        moodEmoji: String,
        moodText: String
    ) = viewModelScope.launch {
        InputValidator.username(username)?.let {
            _action.value = ActionState.Error(it); return@launch
        }
        if (bio.length > MAX_BIO_LENGTH) {
            _action.value = ActionState.Error("Bio : $MAX_BIO_LENGTH caractères max")
            return@launch
        }

        _action.value = ActionState.Loading()
        runCatching {
            val current = _viewedProfile.value
            if (current?.username != username.trim() &&
                repo.usernameExists(username.trim())
            ) error("Ce pseudo est déjà pris")

            repo.updateProfile(currentUid, mapOf(
                "username"  to username.trim(),
                "bio"       to InputValidator.sanitize(bio).take(MAX_BIO_LENGTH),
                "classeENI" to classeENI.trim(),
                "moodEmoji" to moodEmoji,
                "moodText"  to moodText.trim().take(40)
            ))
        }.onSuccess { _action.value = ActionState.Success("Profil mis à jour ✓") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun uploadPhoto(uri: Uri) = viewModelScope.launch {
        _action.value = ActionState.Loading(0)
        runCatching {
            val r = CloudinaryUploader.uploadImage(
                getApplication(), uri
            ) { p -> _action.value = ActionState.Loading(p) }
            settings.addBytes(r.bytesUploaded.toLong())
            repo.updateProfile(currentUid, mapOf("photoUrl" to r.url))
            r.savedKb
        }.onSuccess { saved ->
            _action.value = ActionState.Success(
                if (saved > 0) "Photo mise à jour ✓ ($saved Ko économisés)"
                else "Photo mise à jour ✓"
            )
        }.onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun deletePhoto() = viewModelScope.launch {
        runCatching { repo.updateProfile(currentUid, mapOf("photoUrl" to "")) }
            .onSuccess { _action.value = ActionState.Success("Photo supprimée") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    // ── Admin ─────────────────────────────────────────────────────────────────
    fun grantENIBadge(uid: String, granted: Boolean) = viewModelScope.launch {
        if (!isAdmin(currentUid)) return@launch
        runCatching { repo.updateProfile(uid, mapOf("hasBadgeENI" to granted)) }
            .onSuccess {
                _action.value = ActionState.Success(
                    if (granted) "Badge ENI accordé ✓" else "Badge ENI retiré"
                )
            }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun banUser(uid: String, banned: Boolean) = viewModelScope.launch {
        if (!isAdmin(currentUid)) return@launch
        if (uid == currentUid) {
            _action.value = ActionState.Error("Vous ne pouvez pas vous bannir")
            return@launch
        }
        runCatching { repo.updateProfile(uid, mapOf("isBanned" to banned)) }
            .onSuccess {
                _action.value = ActionState.Success(
                    if (banned) "Utilisateur banni" else "Utilisateur réactivé ✓"
                )
            }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun setCustomBadge(uid: String, name: String, colorHex: String) = viewModelScope.launch {
        if (!isAdmin(currentUid)) return@launch
        runCatching {
            repo.updateProfile(uid, mapOf(
                "customBadgeName"  to name.trim().take(15),
                "customBadgeColor" to colorHex
            ))
        }.onSuccess { _action.value = ActionState.Success("Badge attribué ✓") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun setMemberSearch(q: String) { _memberSearch.value = q }
    fun clearAction()              { _action.value = ActionState.Idle }

    class Factory(
        private val app: Application,
        private val uid: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            `ProfileViewModel.kt`(app, uid) as T
    }
}