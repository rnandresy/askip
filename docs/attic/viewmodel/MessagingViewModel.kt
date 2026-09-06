package com.rnandresy.lol.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnandresy.lol.model.*
import com.rnandresy.lol.repository.GroupRepository
import com.rnandresy.lol.repository.MessageRepository
import com.rnandresy.lol.repository.NotificationRepository
import com.rnandresy.lol.repository.ProfileRepository
import com.rnandresy.lol.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * Messagerie privée et groupes.
 * Les messages d'une conversation ne sont chargés
 * qu'à l'ouverture de celle-ci.
 */
class `MessagingViewModel.kt`(
    app: Application,
    private val currentUid: String,
    private val currentUsername: String,
    private val currentPhotoUrl: String
) : AndroidViewModel(app) {

    private val msgRepo     = MessageRepository()
    private val groupRepo   = GroupRepository()
    private val profileRepo = ProfileRepository()
    private val notifRepo   = NotificationRepository()
    private val settings    = SettingsRepository(app)

    // ── Listes ────────────────────────────────────────────────────────────────
    val conversations: StateFlow<List<Conversation>> = msgRepo
        .listenToConversations(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<Group>> = groupRepo
        .listenToGroups(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProfiles: StateFlow<List<UserProfile>> = profileRepo
        .listenToAllProfiles(limit = PAGE_SIZE_MEMBERS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Total non lus — pour le badge sur l'onglet. */
    val unreadTotal: StateFlow<Int> = combine(conversations, groups) { convs, grps ->
        convs.sumOf { it.unreadCountFor(currentUid) } +
                grps.sumOf  { it.unreadCountFor(currentUid) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Conversation ouverte ──────────────────────────────────────────────────
    private val _activeConvId = MutableStateFlow("")
    val activeConvId: StateFlow<String> = _activeConvId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    // ── Groupe ouvert ─────────────────────────────────────────────────────────
    private val _activeGroupId = MutableStateFlow("")
    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages.asStateFlow()

    // ── Réponse à un message (swipe-to-reply) ─────────────────────────────────
    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    private val _action = MutableStateFlow<ActionState>(ActionState.Idle)
    val action: StateFlow<ActionState> = _action.asStateFlow()

    // ── Ouverture d'une conversation ──────────────────────────────────────────
    fun openConversation(convId: String) {
        if (_activeConvId.value == convId) return
        _activeConvId.value    = convId
        _messages.value        = emptyList()
        _hasMoreMessages.value = true

        viewModelScope.launch {
            msgRepo.listenToMessages(convId, PAGE_SIZE_MESSAGES).collect { list ->
                _messages.value = list
                _hasMoreMessages.value = list.size >= PAGE_SIZE_MESSAGES
            }
        }
        viewModelScope.launch { msgRepo.markAsRead(convId, currentUid) }
    }

    fun loadOlderMessages() = viewModelScope.launch {
        val convId = _activeConvId.value.ifBlank { return@launch }
        val oldest = _messages.value.minByOrNull { it.timestamp } ?: return@launch

        runCatching { msgRepo.getOlderMessages(convId, oldest.timestamp, PAGE_SIZE_MESSAGES) }
            .onSuccess { older ->
                if (older.isEmpty()) _hasMoreMessages.value = false
                else {
                    val ids = _messages.value.map { it.id }.toSet()
                    _messages.value = (older.filter { it.id !in ids } + _messages.value)
                        .sortedBy { it.timestamp }
                }
            }
    }

    fun closeConversation() {
        _activeConvId.value = ""
        _messages.value     = emptyList()
        _replyingTo.value   = null
    }

    // ── Envoi de message ──────────────────────────────────────────────────────
    fun sendMessage(
        text: String,
        toUid: String,
        toUsername: String,
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        audioFile: File? = null,
        audioDuration: Int = 0,
        fileUri: Uri? = null
    ) = viewModelScope.launch {

        val clean = InputValidator.sanitize(text).take(MAX_MESSAGE_LENGTH)
        val hasMedia = imageUri != null || videoUri != null ||
                audioFile != null || fileUri != null
        if (clean.isBlank() && !hasMedia) return@launch

        RateLimiter.check("message", MIN_MS_BETWEEN_MESSAGES)?.let {
            _action.value = ActionState.Error(it); return@launch
        }

        _action.value = ActionState.Loading(0)
        val ctx = getApplication<Application>()

        runCatching {
            var imageUrl = ""; var videoUrl = ""
            var audioUrl = ""; var fileUrl  = ""; var fileName = ""

            imageUri?.let {
                val r = CloudinaryUploader.uploadImage(ctx, it) { p ->
                    _action.value = ActionState.Loading(p)
                }
                imageUrl = r.url; settings.addBytes(r.bytesUploaded.toLong())
            }
            videoUri?.let {
                val r = CloudinaryUploader.uploadVideo(ctx, it) { p ->
                    _action.value = ActionState.Loading(p)
                }
                videoUrl = r.url; settings.addBytes(r.bytesUploaded.toLong())
            }
            audioFile?.let {
                val r = CloudinaryUploader.uploadAudio(it) { p ->
                    _action.value = ActionState.Loading(p)
                }
                audioUrl = r.url; settings.addBytes(r.bytesUploaded.toLong())
            }
            fileUri?.let {
                val (r, name) = CloudinaryUploader.uploadFile(ctx, it) { p ->
                    _action.value = ActionState.Loading(p)
                }
                fileUrl = r.url; fileName = name
                settings.addBytes(r.bytesUploaded.toLong())
            }

            val reply = _replyingTo.value
            msgRepo.sendMessage(
                fromUid       = currentUid,
                fromUsername  = currentUsername,
                fromPhotoUrl  = currentPhotoUrl,
                toUid         = toUid,
                toUsername    = toUsername,
                text          = clean,
                imageUrl      = imageUrl,
                videoUrl      = videoUrl,
                audioUrl      = audioUrl,
                audioDuration = audioDuration,
                fileUrl       = fileUrl,
                fileName      = fileName,
                replyToId     = reply?.id ?: "",
                replyToText   = reply?.previewText() ?: "",
                replyToName   = reply?.fromUsername ?: ""
            )

            notifRepo.notifyMessage(
                targetUid = toUid,
                fromUid   = currentUid,
                fromName  = currentUsername,
                preview   = clean.ifBlank { "📎 Pièce jointe" }.take(60),
                convId    = msgRepo.conversationIdFor(currentUid, toUid)
            )
        }.onSuccess {
            _replyingTo.value = null
            _action.value     = ActionState.Idle
        }.onFailure {
            _action.value = ActionState.Error(it.toFrenchMessage())
        }
    }

    fun deleteMessage(convId: String, messageId: String) = viewModelScope.launch {
        runCatching { msgRepo.deleteMessage(convId, messageId) }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun deleteConversation(convId: String) = viewModelScope.launch {
        runCatching { msgRepo.deleteConversation(convId, currentUid) }
            .onSuccess { _action.value = ActionState.Success("Conversation supprimée") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun togglePinConversation(convId: String, pinned: Boolean) = viewModelScope.launch {
        runCatching { msgRepo.setPinned(convId, currentUid, pinned) }
    }

    // ── Groupes ───────────────────────────────────────────────────────────────
    fun openGroup(groupId: String) {
        if (_activeGroupId.value == groupId) return
        _activeGroupId.value = groupId
        _groupMessages.value = emptyList()

        viewModelScope.launch {
            groupRepo.listenToMessages(groupId, PAGE_SIZE_MESSAGES)
                .collect { _groupMessages.value = it }
        }
        viewModelScope.launch { groupRepo.markAsRead(groupId, currentUid) }
    }

    fun closeGroup() {
        _activeGroupId.value = ""
        _groupMessages.value = emptyList()
    }

    fun createGroup(name: String, memberUids: List<String>) = viewModelScope.launch {
        val clean = name.trim()
        if (clean.isBlank()) {
            _action.value = ActionState.Error("Le nom du groupe est requis"); return@launch
        }
        if (memberUids.isEmpty()) {
            _action.value = ActionState.Error("Ajoutez au moins un membre"); return@launch
        }
        _action.value = ActionState.Loading()
        runCatching {
            groupRepo.createGroup(
                name       = clean,
                creatorUid = currentUid,
                memberUids = (memberUids + currentUid).distinct()
            )
        }.onSuccess { _action.value = ActionState.Success("Groupe créé ✓") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun sendGroupMessage(
        groupId: String,
        text: String,
        imageUri: Uri? = null,
        audioFile: File? = null,
        audioDuration: Int = 0
    ) = viewModelScope.launch {
        val clean = InputValidator.sanitize(text).take(MAX_MESSAGE_LENGTH)
        if (clean.isBlank() && imageUri == null && audioFile == null) return@launch

        RateLimiter.check("group_msg", MIN_MS_BETWEEN_MESSAGES)?.let {
            _action.value = ActionState.Error(it); return@launch
        }

        val ctx = getApplication<Application>()
        _action.value = ActionState.Loading(0)

        runCatching {
            var imageUrl = ""; var audioUrl = ""
            imageUri?.let {
                val r = CloudinaryUploader.uploadImage(ctx, it) { p ->
                    _action.value = ActionState.Loading(p)
                }
                imageUrl = r.url; settings.addBytes(r.bytesUploaded.toLong())
            }
            audioFile?.let {
                val r = CloudinaryUploader.uploadAudio(it) { p ->
                    _action.value = ActionState.Loading(p)
                }
                audioUrl = r.url; settings.addBytes(r.bytesUploaded.toLong())
            }

            groupRepo.sendMessage(
                groupId       = groupId,
                fromUid       = currentUid,
                fromUsername  = currentUsername,
                fromPhotoUrl  = currentPhotoUrl,
                text          = clean,
                imageUrl      = imageUrl,
                audioUrl      = audioUrl,
                audioDuration = audioDuration
            )
        }.onSuccess { _action.value = ActionState.Idle }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun leaveGroup(groupId: String) = viewModelScope.launch {
        runCatching { groupRepo.removeMember(groupId, currentUid) }
            .onSuccess { _action.value = ActionState.Success("Groupe quitté") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun addGroupMembers(groupId: String, uids: List<String>) = viewModelScope.launch {
        runCatching { groupRepo.addMembers(groupId, uids) }
            .onSuccess { _action.value = ActionState.Success("Membre(s) ajouté(s) ✓") }
            .onFailure { _action.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun startReply(message: Message) { _replyingTo.value = message }
    fun cancelReply()                { _replyingTo.value = null }
    fun clearAction()                { _action.value = ActionState.Idle }

    class Factory(
        private val app: Application,
        private val uid: String,
        private val username: String,
        private val photoUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            `MessagingViewModel.kt`(app, uid, username, photoUrl) as T
    }
}