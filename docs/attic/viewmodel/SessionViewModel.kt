package com.rnandresy.lol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.repository.AuthRepository
import com.rnandresy.lol.repository.ProfileRepository
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Session utilisateur : authentification, profil courant,
 * préférences globales, état réseau.
 * Vit tant que l'app est ouverte — un seul instance partagée.
 */
class `SessionViewModel.kt`(app: Application) : AndroidViewModel(app) {

    private val authRepo    = AuthRepository()
    private val profileRepo = ProfileRepository()
    private val settings    = SettingsRepository(app)
    private val network     = NetworkMonitor(app)

    // ── Auth ──────────────────────────────────────────────────────────────────
    private val _isLoggedIn = MutableStateFlow(authRepo.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val currentUid: String get() = authRepo.currentUid
    val currentEmail: String get() = authRepo.currentEmail

    private val _authAction = MutableStateFlow<ActionState>(ActionState.Idle)
    val authAction: StateFlow<ActionState> = _authAction.asStateFlow()

    // ── Profil courant ────────────────────────────────────────────────────────
    private val _myProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile.asStateFlow()

    val isCurrentUserAdmin: Boolean get() = isAdmin(currentUid)

    // ── Réseau ────────────────────────────────────────────────────────────────
    val networkStatus: StateFlow<NetworkStatus> = network.status
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetworkStatus())

    /** Mode économie : désactive le préchargement des médias lourds. */
    val dataSaverActive: StateFlow<Boolean> = combine(
        networkStatus, settings.forceDataSaver
    ) { net, forced -> forced || net.shouldSaveData }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Préférences ───────────────────────────────────────────────────────────
    val appTheme: StateFlow<AppTheme> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.BLACK_WHITE)

    val notifyMessages: StateFlow<Boolean> = settings.notifyMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notifyMentions: StateFlow<Boolean> = settings.notifyMentions
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notifyPosts: StateFlow<Boolean> = settings.notifyPosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoPlayVideos: StateFlow<Boolean> = settings.autoPlayVideos
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Consommation data ─────────────────────────────────────────────────────
    val totalBytesUsed: StateFlow<Long> = settings.totalBytes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private var sessionBytes = 0L
    fun trackBytes(bytes: Int) = viewModelScope.launch {
        sessionBytes += bytes
        settings.addBytes(bytes.toLong())
    }
    fun sessionMb(): Float = sessionBytes / (1024f * 1024f)

    // ── Cycle de vie ──────────────────────────────────────────────────────────
    init {
        if (authRepo.isLoggedIn) observeMyProfile()
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val logged = auth.currentUser != null
            if (_isLoggedIn.value != logged) {
                _isLoggedIn.value = logged
                if (logged) observeMyProfile() else _myProfile.value = null
            }
        }
    }

    private fun observeMyProfile() = viewModelScope.launch {
        profileRepo.listenToProfile(currentUid).collect { _myProfile.value = it }
    }

    // ── Actions auth ──────────────────────────────────────────────────────────
    fun login(email: String, password: String) = viewModelScope.launch {
        InputValidator.email(email)?.let {
            _authAction.value = ActionState.Error(it); return@launch
        }
        InputValidator.password(password)?.let {
            _authAction.value = ActionState.Error(it); return@launch
        }
        if (!network.isOnlineNow()) {
            _authAction.value = ActionState.Error("Pas de connexion internet")
            return@launch
        }

        _authAction.value = ActionState.Loading()
        runCatching { authRepo.login(email.trim(), password) }
            .onSuccess {
                _isLoggedIn.value = true
                observeMyProfile()
                _authAction.value = ActionState.Success()
            }
            .onFailure { _authAction.value = ActionState.Error(it.toFrenchMessage()) }
    }

    fun register(email: String, password: String, username: String) = viewModelScope.launch {
        InputValidator.username(username)?.let {
            _authAction.value = ActionState.Error(it); return@launch
        }
        InputValidator.email(email)?.let {
            _authAction.value = ActionState.Error(it); return@launch
        }
        InputValidator.password(password)?.let {
            _authAction.value = ActionState.Error(it); return@launch
        }
        if (!network.isOnlineNow()) {
            _authAction.value = ActionState.Error("Pas de connexion internet")
            return@launch
        }

        _authAction.value = ActionState.Loading()
        runCatching {
            if (profileRepo.usernameExists(username.trim()))
                error("Ce pseudo est déjà pris")
            authRepo.register(email.trim(), password, username.trim())
        }.onSuccess {
            _isLoggedIn.value = true
            observeMyProfile()
            _authAction.value = ActionState.Success()
        }.onFailure {
            _authAction.value = ActionState.Error(it.toFrenchMessage())
        }
    }

    fun resetPassword(email: String, onDone: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            InputValidator.email(email)?.let { onDone(false, it); return@launch }
            runCatching { authRepo.resetPassword(email.trim()) }
                .onSuccess { onDone(true, "Email de réinitialisation envoyé") }
                .onFailure { onDone(false, it.toFrenchMessage()) }
        }

    fun updateEmail(newEmail: String, password: String, onDone: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            InputValidator.email(newEmail)?.let { onDone(false, it); return@launch }
            runCatching { authRepo.updateEmail(newEmail.trim(), password) }
                .onSuccess { onDone(true, "Vérifiez votre nouvelle adresse email") }
                .onFailure { onDone(false, it.toFrenchMessage()) }
        }

    fun updatePassword(current: String, newPwd: String, onDone: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            InputValidator.password(newPwd)?.let { onDone(false, it); return@launch }
            runCatching { authRepo.updatePassword(current, newPwd) }
                .onSuccess { onDone(true, "Mot de passe mis à jour") }
                .onFailure { onDone(false, it.toFrenchMessage()) }
        }

    fun logout() = viewModelScope.launch {
        authRepo.logout()
        RateLimiter.resetAll()
        _myProfile.value  = null
        _isLoggedIn.value = false
        _authAction.value = ActionState.Idle
    }

    fun deleteAccount(password: String, onDone: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            _authAction.value = ActionState.Loading()
            runCatching {
                profileRepo.deleteAllUserContent(currentUid)
                authRepo.deleteAccount(password)
            }.onSuccess {
                _myProfile.value  = null
                _isLoggedIn.value = false
                _authAction.value = ActionState.Idle
                onDone(true, "Compte supprimé")
            }.onFailure {
                _authAction.value = ActionState.Idle
                onDone(false, it.toFrenchMessage())
            }
        }

    // ── Préférences ───────────────────────────────────────────────────────────
    fun setTheme(t: AppTheme)          = viewModelScope.launch { settings.setTheme(t) }
    fun setNotifyMessages(v: Boolean)  = viewModelScope.launch { settings.setNotifyMessages(v) }
    fun setNotifyMentions(v: Boolean)  = viewModelScope.launch { settings.setNotifyMentions(v) }
    fun setNotifyPosts(v: Boolean)     = viewModelScope.launch { settings.setNotifyPosts(v) }
    fun setAutoPlayVideos(v: Boolean)  = viewModelScope.launch { settings.setAutoPlayVideos(v) }
    fun setForceDataSaver(v: Boolean)  = viewModelScope.launch { settings.setForceDataSaver(v) }

    fun clearAuthAction() { _authAction.value = ActionState.Idle }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            `SessionViewModel.kt`(app) as T
    }
}