package com.rnandresy.lol.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.rnandresy.lol.model.Achievement
import com.rnandresy.lol.model.AppNotification
import com.rnandresy.lol.model.Badge
import com.rnandresy.lol.model.Bet
import com.rnandresy.lol.model.ChainLink
import com.rnandresy.lol.model.Comment
import com.rnandresy.lol.model.Conversation
import com.rnandresy.lol.model.Group
import com.rnandresy.lol.model.GroupMessage
import com.rnandresy.lol.model.MentionReply
import com.rnandresy.lol.model.Message
import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.SealedPayload
import com.rnandresy.lol.model.Story
import com.rnandresy.lol.model.UserProfile
import com.rnandresy.lol.model.Verdict
import com.rnandresy.lol.repository.AuthRepository
import com.rnandresy.lol.repository.FeedRepository
import com.rnandresy.lol.repository.MessagingRepository
import com.rnandresy.lol.repository.NotificationRepository
import com.rnandresy.lol.repository.ProfileRepository
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.utils.ADMIN_BADGE_NAME
import com.rnandresy.lol.utils.BETS_PER_DAY
import com.rnandresy.lol.utils.BET_PENALTY
import com.rnandresy.lol.utils.CHAIN_MAX_LENGTH
import com.rnandresy.lol.utils.CHAIN_MAX_LINKS
import com.rnandresy.lol.utils.CLOUT_PER_CREDIBLE
import com.rnandresy.lol.utils.CLOUT_PER_FAKE
import com.rnandresy.lol.utils.CLOUT_PER_SCOOP
import com.rnandresy.lol.utils.CloudinaryUploader
import com.rnandresy.lol.utils.DataUsageTracker
import com.rnandresy.lol.utils.FeedSection
import com.rnandresy.lol.utils.FeedTab
import com.rnandresy.lol.utils.HOT_WINDOW_SIZE
import com.rnandresy.lol.utils.MAX_COMMENT_LENGTH
import com.rnandresy.lol.utils.MAX_POSTS_PER_HOUR
import com.rnandresy.lol.utils.MIN_MS_BETWEEN_COMMENTS
import com.rnandresy.lol.utils.MIN_MS_BETWEEN_POSTS
import com.rnandresy.lol.utils.NotificationHelper
import com.rnandresy.lol.utils.PAGE_SIZE_FEED
import com.rnandresy.lol.utils.POST_TYPE_CHAIN
import com.rnandresy.lol.utils.POST_TYPE_CONFESSION
import com.rnandresy.lol.utils.POST_TYPE_SEALED
import com.rnandresy.lol.utils.POST_TYPE_TRUTH
import com.rnandresy.lol.utils.POST_TYPE_VOICE
import com.rnandresy.lol.utils.SEAL_KEYS_TO_OPEN
import com.rnandresy.lol.utils.QuestDef
import com.rnandresy.lol.utils.QuestKind
import com.rnandresy.lol.utils.QuestProgress
import com.rnandresy.lol.utils.Quests
import com.rnandresy.lol.utils.RateLimiter
import com.rnandresy.lol.utils.RumorEngine
import com.rnandresy.lol.utils.SettingsRepository
import com.rnandresy.lol.utils.VoiceRecorder
import com.rnandresy.lol.utils.XP_BET
import com.rnandresy.lol.utils.XP_CHAIN_LINK
import com.rnandresy.lol.utils.XP_COMMENT
import com.rnandresy.lol.utils.XP_DAILY_LOGIN
import com.rnandresy.lol.utils.XP_POST
import com.rnandresy.lol.utils.XP_REACTION
import com.rnandresy.lol.utils.XP_STORY
import com.rnandresy.lol.utils.XP_VERDICT
import com.rnandresy.lol.utils.isAdmin
import com.rnandresy.lol.utils.levelForXp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * ViewModel principal de l'app.
 *
 * Il porte l'état partagé par tous les écrans (session, profils, fil, messages)
 * et s'appuie sur des repositories découpés par domaine. Les ViewModels
 * spécialisés (`FeedViewModel`, `MessagingViewModel`, …) attaquent les mêmes
 * repositories : les écrans peuvent migrer vers eux un par un, sans rien casser.
 */
class AskipViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val profileRepo = ProfileRepository()
    private val feedRepo = FeedRepository()
    private val msgRepo = MessagingRepository()
    private val notifRepo = NotificationRepository()

    private val notif = NotificationHelper(application)
    private val settings = SettingsRepository(application)
    val dataTracker = DataUsageTracker()

    // ── Auth ──────────────────────────────────────────────────────────────────

    val isLoggedIn = MutableStateFlow(authRepo.isLoggedIn)
    val currentUserId: String get() = authRepo.currentUid
    val currentEmail: String get() = authRepo.currentEmail

    // ── Profils ───────────────────────────────────────────────────────────────

    private val _myProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile

    private val _viewedProfile = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile

    private val _profilesMap = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val profilesMap: StateFlow<Map<String, UserProfile>> = _profilesMap

    val allProfiles: StateFlow<List<UserProfile>> = _profilesMap
        .map { it.values.filter { p -> p.userId != currentUserId }.toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Fil ───────────────────────────────────────────────────────────────────

    /** La fenêtre récente, écoutée en temps réel. */
    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    /**
     * Les rumeurs plus anciennes, chargées à la demande.
     *
     * L'écoute temps réel s'arrête à [HOT_WINDOW_SIZE] pour ne pas payer une
     * lecture par post à chaque ouverture. Mais rien ne doit devenir
     * inatteignable : au bout du fil, on continue de paginer ici.
     */
    private val _olderPosts = MutableStateFlow<List<Post>>(emptyList())

    private val _hasMorePosts = MutableStateFlow(true)
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    /** Fenêtre récente + pages plus anciennes, sans doublon. */
    private val allKnownPosts: StateFlow<List<Post>> =
        combine(_allPosts, _olderPosts) { recent, older ->
            val ids = recent.map { it.id }.toSet()
            recent + older.filterNot { it.id in ids }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Les posts avec pseudo et photo à jour. Un changement de pseudo se voit
     * partout instantanément, sans réécrire les documents.
     * Les posts anonymes reçoivent leur masque stable au lieu d'un « Quelqu'un »
     * interchangeable.
     */
    private val enrichedPosts: StateFlow<List<Post>> =
        combine(allKnownPosts, _profilesMap) { posts, profiles ->
            posts.map { post ->
                if (post.isAnonymous) {
                    post.copy(
                        username = RumorEngine.anonAliasShort(post.userId, post.id),
                        userPhotoUrl = ""
                    )
                } else {
                    val p = profiles[post.userId]
                    post.copy(
                        username = p?.username?.takeIf { it.isNotBlank() } ?: post.username,
                        userPhotoUrl = p?.photoUrl ?: post.userPhotoUrl
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Onglet courant du fil — mémorisé entre deux lancements. */
    val feedTab: StateFlow<FeedTab> = settings.feedTab
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeedTab.HOT)

    fun setFeedTab(tab: FeedTab) = viewModelScope.launch { settings.setFeedTab(tab) }

    /** Actualité principale, vocale ou Page de Vérité. */
    val feedSection: StateFlow<FeedSection> = settings.feedSection
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeedSection.MAIN)

    fun setFeedSection(section: FeedSection) =
        viewModelScope.launch { settings.setFeedSection(section) }

    /** Le fil tel qu'affiché : la section décide de la matière, l'onglet du tri. */
    val feedPosts: StateFlow<List<Post>> =
        combine(enrichedPosts, feedSection, feedTab) { posts, section, tab ->
            RumorEngine.applySection(posts, section, tab)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Le registre de la Page de Vérité — serments prêtés, serments démentis. */
    val truthLedger: StateFlow<RumorEngine.TruthLedger> = enrichedPosts
        .map { RumorEngine.truthLedger(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            RumorEngine.TruthLedger(0, 0, 0, 0)
        )

    val confessions: StateFlow<List<Post>> = enrichedPosts
        .map { list -> RumorEngine.sortFresh(list.filter { it.isConfession() }) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Ce dont le campus parle en ce moment. */
    val trendingTags: StateFlow<List<RumorEngine.TrendingTag>> = enrichedPosts
        .map { RumorEngine.trendingTags(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** L'ambiance du campus sur 24 h — recalculée à chaque réaction qui arrive. */
    val campusWeather: StateFlow<RumorEngine.CampusWeather> = enrichedPosts
        .map { RumorEngine.campusWeather(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            RumorEngine.CampusWeather(RumorEngine.Weather.CALM, 0, 0, 0f)
        )

    /** Rumeurs d'un salon de tag — alimenté par [openTag]. */
    private val _tagPosts = MutableStateFlow<List<Post>>(emptyList())
    val tagPosts: StateFlow<List<Post>> = _tagPosts

    /** La rumeur ouverte dans l'écran de détail. */
    private val _viewedPost = MutableStateFlow<Post?>(null)
    val viewedPost: StateFlow<Post?> = _viewedPost

    // ── Paris, capsules, chaînes, droit de réponse ────────────────────────────
    // Déclarés ici, au-dessus du bloc `init` : celui-ci démarre l'écoute des
    // paris, et un initialiseur de propriété situé plus bas écraserait le job
    // qu'il vient de créer.

    /** Mes paris, indexés par rumeur. Un seul écouteur pour toute l'app. */
    private val _myBets = MutableStateFlow<Map<String, Bet>>(emptyMap())
    val myBets: StateFlow<Map<String, Bet>> = _myBets

    /** Contenus de capsules déjà obtenus du serveur, par rumeur. */
    private val _sealedContents = MutableStateFlow<Map<String, SealedPayload>>(emptyMap())
    val sealedContents: StateFlow<Map<String, SealedPayload>> = _sealedContents

    private val _chainLinks = MutableStateFlow<List<ChainLink>>(emptyList())
    val chainLinks: StateFlow<List<ChainLink>> = _chainLinks

    private val _mentionReplies = MutableStateFlow<List<MentionReply>>(emptyList())
    val mentionReplies: StateFlow<List<MentionReply>> = _mentionReplies

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    /**
     * Vrai dès que la première réponse Firestore est arrivée.
     * Sans ce drapeau, un fil encore vide affiche « le campus est calme »
     * pendant le chargement — ce qui donne l'impression que l'app est cassée.
     */
    private val _feedLoaded = MutableStateFlow(false)
    val feedLoaded: StateFlow<Boolean> = _feedLoaded

    /** L'amorce du jour, identique pour tout le monde, renouvelée à minuit. */
    val dailyPrompt: String get() = RumorEngine.dailyPrompt()

    // ── Commentaires ──────────────────────────────────────────────────────────

    private val _rawComments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> =
        combine(_rawComments, _profilesMap) { list, profiles ->
            list.map { c ->
                if (c.isAnonymous) {
                    c.copy(username = RumorEngine.anonAliasShort(c.userId, c.postId))
                } else {
                    val live = profiles[c.userId]?.username
                    if (live != null && live != c.username) c.copy(username = live) else c
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Conversations ─────────────────────────────────────────────────────────

    private val _rawConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> =
        combine(_rawConversations, _profilesMap) { convs, profiles ->
            convs.map { conv ->
                val updated = conv.participantNames.toMutableMap()
                conv.participants.forEach { uid ->
                    profiles[uid]?.username?.let { updated[uid] = it }
                }
                if (updated != conv.participantNames) conv.copy(participantNames = updated)
                else conv
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _rawMessages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> =
        combine(_rawMessages, _profilesMap) { msgs, profiles ->
            msgs.map { msg ->
                val live = profiles[msg.senderId]?.username
                if (live != null && live != msg.senderUsername) msg.copy(senderUsername = live)
                else msg
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Groupes ───────────────────────────────────────────────────────────────

    private val _rawGroups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _rawGroups

    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages

    // ── Badges ────────────────────────────────────────────────────────────────

    private val _allBadges = MutableStateFlow<List<Badge>>(emptyList())
    val allBadges: StateFlow<List<Badge>> = _allBadges

    private val _myBadges = MutableStateFlow<List<Badge>>(emptyList())
    val myBadges: StateFlow<List<Badge>> = _myBadges

    // ── Succès ────────────────────────────────────────────────────────────────

    private val _myAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val myAchievements: StateFlow<List<Achievement>> = _myAchievements

    private val _viewedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val viewedAchievements: StateFlow<List<Achievement>> = _viewedAchievements

    // ── Notifications ─────────────────────────────────────────────────────────

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    val unreadNotifCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Messages non lus, toutes conversations confondues — pour la pastille. */
    val unreadMessagesCount: StateFlow<Int> = _rawConversations
        .map { convs -> convs.sumOf { (it.unreadCounts[currentUserId] ?: 0L).toInt() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Missions du jour ──────────────────────────────────────────────────────

    val questProgress: StateFlow<QuestProgress> = settings.questProgress
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuestProgress())

    val todayQuests: List<QuestDef> get() = Quests.today()

    /** XP en attente de récupération — sert la pastille sur l'onglet Profil. */
    val claimableXp: StateFlow<Long> = questProgress
        .map { Quests.claimableXp(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** Le Scoop du jour est-il encore disponible ? */
    val canScoopToday: StateFlow<Boolean> = _myProfile
        .map { RumorEngine.canScoop(it?.lastScoopDate ?: "") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Jetons de pari restants aujourd'hui. */
    val betTokensLeft: StateFlow<Int> = _myProfile
        .map { it?.betTokensLeft(RumorEngine.today(), BETS_PER_DAY) ?: BETS_PER_DAY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BETS_PER_DAY)

    // ── Classements ───────────────────────────────────────────────────────────

    private val _topClout = MutableStateFlow<List<UserProfile>>(emptyList())
    val topClout: StateFlow<List<UserProfile>> = _topClout

    private val _topXp = MutableStateFlow<List<UserProfile>>(emptyList())
    val topXp: StateFlow<List<UserProfile>> = _topXp

    private val _topStreak = MutableStateFlow<List<UserProfile>>(emptyList())
    val topStreak: StateFlow<List<UserProfile>> = _topStreak

    /** Le panthéon : les rumeurs qui ont marqué, tous temps confondus. */
    val legendPosts: StateFlow<List<Post>> = enrichedPosts
        .map { RumorEngine.sortLegends(it).take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Recherche ─────────────────────────────────────────────────────────────

    val searchQuery = MutableStateFlow("")

    data class SearchResults(
        val posts: List<Post> = emptyList(),
        val users: List<UserProfile> = emptyList(),
        val groups: List<Group> = emptyList()
    )

    val searchResults: StateFlow<SearchResults> = combine(
        searchQuery, enrichedPosts, _profilesMap, _rawGroups
    ) { query, posts, profiles, grps ->
        if (query.isBlank()) SearchResults()
        else {
            val q = query.trim().lowercase().removePrefix("#")
            SearchResults(
                posts = posts.filter {
                    it.content.lowercase().contains(q) ||
                        it.tags.any { tag -> tag.contains(q) } ||
                        (!it.isAnonymous && it.username.lowercase().contains(q))
                }.take(20),
                users = profiles.values.filter {
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

    // ── Préférences ───────────────────────────────────────────────────────────

    val notifyMessages = settings.notifyMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifyPosts = settings.notifyPosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val notifyMentions = settings.notifyMentions
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val totalBytesStored = settings.totalBytes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val appTheme = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SAKURA)
    val haptics = settings.haptics
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val reduceMotion = settings.reduceMotion
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── État d'interface ──────────────────────────────────────────────────────

    val error = MutableStateFlow<String?>(null)
    val info = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

    /** Niveau atteint à célébrer, null le reste du temps. */
    private val _levelUp = MutableStateFlow<Int?>(null)
    val levelUp: StateFlow<Int?> = _levelUp

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private var voiceRecorder: VoiceRecorder? = null
    private var recordingTimerJob: Job? = null

    /** Dernier niveau connu — sert à détecter la montée de niveau. */
    private var lastKnownLevel = -1

    // ── Jobs ──────────────────────────────────────────────────────────────────

    private var postsJob: Job? = null
    private var storiesJob: Job? = null
    private var convJob: Job? = null
    private var groupJob: Job? = null
    private var groupMsgJob: Job? = null
    private var msgJob: Job? = null
    private var commentJob: Job? = null
    private var badgesJob: Job? = null
    private var profileJob: Job? = null
    private var profilesJob: Job? = null
    private var notifJob: Job? = null
    private var betsJob: Job? = null
    private var chainJob: Job? = null
    private var repliesJob: Job? = null

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    init {
        FirebaseAuth.getInstance().addAuthStateListener { fa ->
            val logged = fa.currentUser != null
            isLoggedIn.value = logged
            if (logged) startAll() else stopAll()
        }
        if (authRepo.isLoggedIn) startAll()
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
        listenMyBets()
        claimDailyBonus()
        registerPushToken()
    }

    /**
     * Enregistre le jeton push sur le profil.
     * `onNewToken` du service ne se déclenche qu'à la rotation du jeton :
     * sans cet appel, un compte fraîchement connecté ne recevrait jamais
     * de notification.
     */
    private fun registerPushToken() = viewModelScope.launch {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank() && token != _myProfile.value?.fcmToken) {
                profileRepo.updateProfile(currentUserId, mapOf("fcmToken" to token))
            }
            // Les préférences de notification vivent en local (DataStore) :
            // le serveur qui envoie les push ne peut pas les lire. On les
            // recopie sur le profil, sinon les interrupteurs des réglages
            // n'auraient aucun effet une fois l'app fermée.
            syncNotifyPrefs()
        }
    }

    /** Recopie les trois interrupteurs de notification sur le profil. */
    private suspend fun syncNotifyPrefs() {
        runCatching {
            profileRepo.updateProfile(
                currentUserId,
                mapOf(
                    "notifyMessages" to settings.notifyMessages.first(),
                    "notifyMentions" to settings.notifyMentions.first(),
                    "notifyPosts" to settings.notifyPosts.first()
                )
            )
        }
    }

    private fun stopAll() {
        listOf(
            postsJob, storiesJob, convJob, groupJob, groupMsgJob, msgJob,
            commentJob, badgesJob, profileJob, profilesJob, notifJob,
            betsJob, chainJob, repliesJob
        ).forEach { it?.cancel() }
        _myBets.value = emptyMap()
        _sealedContents.value = emptyMap()
        _chainLinks.value = emptyList()
        _mentionReplies.value = emptyList()
        _allPosts.value = emptyList()
        _olderPosts.value = emptyList()
        _hasMorePosts.value = true
        _stories.value = emptyList()
        _rawConversations.value = emptyList()
        _rawMessages.value = emptyList()
        _rawGroups.value = emptyList()
        _groupMessages.value = emptyList()
        _myProfile.value = null
        _profilesMap.value = emptyMap()
        _allBadges.value = emptyList()
        _myBadges.value = emptyList()
        _notifications.value = emptyList()
        _tagPosts.value = emptyList()
        lastKnownLevel = -1
    }

    // ── Authentification ──────────────────────────────────────────────────────

    fun login(email: String, password: String) = viewModelScope.launch {
        loading.value = true
        runCatching { authRepo.login(email.trim(), password.trim()) }
            .onFailure { error.value = friendly(it.message) }
        loading.value = false
    }

    fun register(email: String, password: String, username: String) = viewModelScope.launch {
        loading.value = true
        runCatching {
            if (profileRepo.usernameExists(username.trim())) error("Ce pseudo est déjà pris")
            authRepo.register(email.trim(), password.trim(), username.trim())
        }.onFailure { error.value = friendly(it.message) }
        loading.value = false
    }

    fun logout() = viewModelScope.launch {
        val bytes = dataTracker.getSessionBytes()
        if (bytes > 0) settings.addBytes(bytes)
        RateLimiter.resetAll()
        runCatching { authRepo.logout() }
    }

    fun updateEmail(e: String, p: String, onDone: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            runCatching { authRepo.updateEmail(e, p) }
                .onSuccess { onDone(true, null) }
                .onFailure { onDone(false, friendly(it.message)) }
        }

    fun updatePassword(c: String, n: String, onDone: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            runCatching { authRepo.updatePassword(c, n) }
                .onSuccess { onDone(true, null) }
                .onFailure { onDone(false, friendly(it.message)) }
        }

    fun resetPassword(email: String, onDone: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            runCatching { authRepo.resetPassword(email.trim()) }
                .onSuccess { onDone(true, "Email de réinitialisation envoyé") }
                .onFailure { onDone(false, friendly(it.message)) }
        }

    private fun friendly(msg: String?): String {
        if (msg == null) return "Erreur inconnue"
        val m = msg.lowercase()
        return when {
            "password" in m && "weak" !in m -> "Mot de passe incorrect"
            "already" in m -> "Email déjà utilisé"
            "no user" in m -> "Aucun compte trouvé"
            "network" in m -> "Vérifiez votre connexion"
            "invalid" in m -> "Email ou mot de passe invalide"
            "weak" in m -> "Mot de passe trop faible (6 caractères minimum)"
            "too many" in m -> "Trop de tentatives, réessaie dans quelques minutes"
            else -> msg
        }
    }

    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        loading.value = true
        runCatching {
            val bytes = dataTracker.getSessionBytes()
            if (bytes > 0) settings.addBytes(bytes)
            // 1. Vérifier le mot de passe AVANT de toucher à quoi que ce soit.
            //    Sinon un mot de passe erroné laisserait la personne sans son
            //    contenu et avec son compte toujours actif.
            authRepo.reauthenticate(password)
            // 2. Le contenu, tant qu'on est encore authentifié : une fois le
            //    compte supprimé, les règles Firestore refusent toute écriture.
            profileRepo.deleteAllUserContent(currentUserId)
            // 3. Le compte Auth en dernier.
            authRepo.deleteAccount(password)
        }.onSuccess {
            stopAll()
            onSuccess()
        }.onFailure { e ->
            onError(
                when {
                    e.message?.contains("password", true) == true -> "Mot de passe incorrect"
                    e.message?.contains("recent", true) == true ->
                        "Reconnecte-toi d'abord et réessaie"
                    else -> e.message ?: "Erreur lors de la suppression"
                }
            )
        }
        loading.value = false
    }

    // ── Profils ───────────────────────────────────────────────────────────────

    private fun listenMyProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            profileRepo.listenToProfile(currentUserId).collect { profile ->
                _myProfile.value = profile
                profile?.let {
                    _myBadges.value = _allBadges.value.filter { b -> b.id in it.badgeIds }
                    _profilesMap.value = _profilesMap.value + (it.userId to it)
                    detectLevelUp(it)
                    checkAchievements(it)
                }
            }
        }
        viewModelScope.launch {
            _myAchievements.value = profileRepo.getAchievements(currentUserId)
        }
    }

    private fun listenAllProfiles() {
        profilesJob?.cancel()
        profilesJob = viewModelScope.launch {
            profileRepo.listenToAllProfiles().collect { profiles ->
                _profilesMap.value = profiles
                    .filter { it.userId.isNotBlank() && it.username.isNotBlank() }
                    .associateBy { it.userId }
                _myProfile.value?.let { me ->
                    _myBadges.value = _allBadges.value.filter { b -> b.id in me.badgeIds }
                }
            }
        }
    }

    fun loadProfile(uid: String) = viewModelScope.launch {
        _viewedProfile.value = _profilesMap.value[uid]
        profileRepo.getProfile(uid)?.let { _viewedProfile.value = it }
        _viewedAchievements.value = profileRepo.getAchievements(uid)
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
                profileRepo.updateProfile(currentUserId, data)
                if (newUsername.isNotBlank() && newUsername != oldUsername) {
                    isSyncing.value = true
                    launch(Dispatchers.IO) {
                        profileRepo.syncUsername(currentUserId, newUsername)
                        isSyncing.value = false
                    }
                }
                onDone?.invoke()
            }.onFailure {
                isSyncing.value = false
                error.value = it.message
            }
        }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        loading.value = true
        _uploadProgress.value = 0
        runCatching {
            val res = CloudinaryUploader.uploadImage(getApplication(), uri) {
                _uploadProgress.value = it
            }
            settings.addBytes(res.bytesUploaded.toLong())
            profileRepo.updateProfile(currentUserId, mapOf("photoUrl" to res.url))
            res.savedKb
        }.onSuccess { saved ->
            if (saved > 0) info.value = "Photo mise à jour — $saved Ko économisés"
        }.onFailure { error.value = it.message }
        loading.value = false
        _uploadProgress.value = 0
    }

    fun uploadCover(uri: Uri) = viewModelScope.launch {
        loading.value = true
        _uploadProgress.value = 0
        runCatching {
            val res = CloudinaryUploader.uploadImage(getApplication(), uri) {
                _uploadProgress.value = it
            }
            settings.addBytes(res.bytesUploaded.toLong())
            profileRepo.updateProfile(currentUserId, mapOf("coverUrl" to res.url))
        }.onFailure { error.value = it.message }
        loading.value = false
        _uploadProgress.value = 0
    }

    fun deleteProfilePhoto() = viewModelScope.launch {
        runCatching { profileRepo.updateProfile(currentUserId, mapOf("photoUrl" to "")) }
            .onFailure { error.value = it.message }
    }

    fun deleteCoverPhoto() = viewModelScope.launch {
        runCatching { profileRepo.updateProfile(currentUserId, mapOf("coverUrl" to "")) }
            .onFailure { error.value = it.message }
    }

    // ── Progression ───────────────────────────────────────────────────────────

    /** Ajoute de l'XP et fait avancer les missions du jour concernées. */
    private fun award(xp: Long, kind: QuestKind?) = viewModelScope.launch {
        if (xp > 0) profileRepo.addXp(currentUserId, xp)
        kind?.let { k ->
            val current = settings.questProgress.first()
            settings.setQuestProgress(Quests.record(current, k))
        }
    }

    private fun detectLevelUp(profile: UserProfile) {
        val level = levelForXp(profile.xp)
        // Au premier chargement on mémorise sans célébrer : sinon chaque
        // ouverture de l'app déclencherait une fausse montée de niveau.
        if (lastKnownLevel in 1 until level) _levelUp.value = level
        lastKnownLevel = level
    }

    fun clearLevelUp() {
        _levelUp.value = null
    }

    /** Bonus de première connexion du jour, une seule fois par jour. */
    private fun claimDailyBonus() = viewModelScope.launch {
        val today = RumorEngine.today()
        if (settings.lastDailyBonus.first() == today) return@launch
        val isNewDay = profileRepo.updateStreak(currentUserId)
        if (isNewDay) {
            profileRepo.addXp(currentUserId, XP_DAILY_LOGIN)
            settings.setLastDailyBonus(today)
        }
    }

    fun claimQuest(def: QuestDef) = viewModelScope.launch {
        val current = settings.questProgress.first()
        if (!current.canClaim(def)) return@launch
        settings.setQuestProgress(Quests.claim(current, def))
        profileRepo.addXp(currentUserId, def.xp)
        info.value = "Mission accomplie — +${def.xp} XP"
    }

    // ── Succès ────────────────────────────────────────────────────────────────

    private fun checkAchievements(profile: UserProfile) = viewModelScope.launch {
        val uid = currentUserId
        val fetched = profileRepo.getAchievements(uid)
        _myAchievements.value = fetched
        val unlocked = fetched.map { it.id }.toSet()

        suspend fun unlock(id: String) {
            if (id !in unlocked) {
                profileRepo.unlockAchievement(uid, id)
                _myAchievements.value = _myAchievements.value +
                    Achievement(id = id, unlockedAt = System.currentTimeMillis())
            }
        }

        if (profile.postsCount >= 1) unlock("first_post")
        if (profile.postsCount >= 10) unlock("ten_posts")
        if (profile.postsCount >= 25) unlock("twenty_five_p")
        if (profile.postsCount >= 50) unlock("fifty_posts")
        if (profile.commentsCount >= 1) unlock("first_comment")
        if (profile.commentsCount >= 20) unlock("commentator")
        if (profile.commentsCount >= 50) unlock("deep_comment")
        if (profile.confessionsCount >= 1) unlock("confessor")
        if (profile.confessionsCount >= 5) unlock("dark_confessor")
        if (profile.pollsCount >= 3) unlock("poll_creator")
        if (profile.pollsCount >= 10) unlock("poll_master")
        if (profile.storiesCount >= 1) unlock("first_story")
        if (profile.storiesCount >= 10) unlock("storyteller")
        if (profile.convsStarted >= 5) unlock("social")
        if (profile.convsStarted >= 20) unlock("social_plus")
        if (profile.streak >= 3) unlock("streak_3")
        if (profile.streak >= 7) unlock("streak_7")
        if (profile.streak >= 30) unlock("streak_30")
        if (profile.hasBadgeENI) unlock("eni_pride")
        if (profile.badgeIds.isNotEmpty()) unlock("badge_maker")
        if (profile.moodEmoji.isNotBlank()) unlock("mood_master")
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    private fun listenAllBadges() {
        badgesJob?.cancel()
        badgesJob = viewModelScope.launch {
            profileRepo.listenToAllBadges().collect { badges ->
                _allBadges.value = badges
                val myIds = _myProfile.value?.badgeIds ?: emptyList()
                _myBadges.value = badges.filter { it.id in myIds }
            }
        }
    }

    fun createOrWearBadge(
        displayName: String,
        colorHex: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            runCatching {
                val trimmed = displayName.trim()
                if (trimmed.isBlank()) return@launch onError("Nom vide")
                if (trimmed.lowercase() == ADMIN_BADGE_NAME) return@launch onError("Nom réservé")
                val existing = profileRepo.findBadgeByName(trimmed)
                if (existing != null) {
                    if (_myBadges.value.any { it.id == existing.id }) {
                        return@launch onError("Déjà porté !")
                    }
                    if (!userIsAdmin && _myBadges.value.isNotEmpty()) {
                        return@launch onError("Retire le badge actuel d'abord")
                    }
                    profileRepo.wearBadge(existing.id, currentUserId)
                } else {
                    if (!userIsAdmin && _myBadges.value.isNotEmpty()) {
                        return@launch onError("Retire le badge actuel d'abord")
                    }
                    profileRepo.createBadge(trimmed, colorHex, currentUserId)
                }
                profileRepo.getProfile(currentUserId)?.let { checkAchievements(it) }
                onSuccess()
            }.onFailure { onError(it.message ?: "Erreur") }
        }
    }

    fun wearExistingBadge(badgeId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            runCatching {
                val badge = _allBadges.value.find { it.id == badgeId }
                    ?: return@launch onError("Badge introuvable")
                if (badge.name == ADMIN_BADGE_NAME) return@launch onError("Badge réservé")
                if (_myBadges.value.any { it.id == badgeId }) return@launch onError("Déjà porté !")
                if (!userIsAdmin && _myBadges.value.isNotEmpty()) {
                    return@launch onError("Retire le badge actuel avant")
                }
                profileRepo.wearBadge(badgeId, currentUserId)
                onSuccess()
            }.onFailure { onError(it.message ?: "Erreur") }
        }
    }

    fun unwearBadge(badgeId: String) = viewModelScope.launch {
        runCatching { profileRepo.unwearBadge(badgeId, currentUserId) }
    }

    fun updateBadge(
        badgeId: String,
        displayName: String,
        colorHex: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            runCatching {
                val badge = _allBadges.value.find { it.id == badgeId }
                    ?: return@launch onError("Badge introuvable")
                if (!userIsAdmin && badge.createdBy != currentUserId) {
                    return@launch onError("Interdit")
                }
                val trimmed = displayName.trim()
                if (trimmed.isBlank()) return@launch onError("Nom vide")
                if (trimmed.lowercase() != badge.name &&
                    profileRepo.findBadgeByName(trimmed) != null
                ) {
                    return@launch onError("Ce nom existe déjà")
                }
                profileRepo.updateBadge(badgeId, trimmed, colorHex)
                onSuccess()
            }.onFailure { onError(it.message ?: "Erreur") }
        }
    }

    fun deleteBadge(badgeId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val profile = _myProfile.value ?: return
        val userIsAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            runCatching {
                val badge = _allBadges.value.find { it.id == badgeId }
                    ?: return@launch onError("Badge introuvable")
                if (!userIsAdmin && badge.createdBy != currentUserId) {
                    return@launch onError("Interdit")
                }
                profileRepo.deleteBadge(badgeId)
                onSuccess()
            }.onFailure { onError(it.message ?: "Erreur") }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun listenNotifications() {
        notifJob?.cancel()
        notifJob = viewModelScope.launch {
            notifRepo.listenToNotifications(currentUserId)
                .catch { /* index manquant ou réseau — on reste silencieux */ }
                .collect { notifs ->
                    _notifications.value = notifs.map { n ->
                        val live = _profilesMap.value[n.fromUserId]?.username
                        if (live != null && live != n.fromUsername) n.copy(fromUsername = live)
                        else n
                    }
                }
        }
    }

    /**
     * Ce que la dernière action sur les notifications a donné.
     *
     * `null` tant qu'il n'y a rien à dire. Un échec silencieux était pire que
     * pas d'action du tout : l'écran affichait « tout lu », puis l'écouteur
     * ramenait l'état réel sans un mot d'explication.
     */
    private val _notifError = MutableStateFlow<String?>(null)
    val notifError: StateFlow<String?> = _notifError

    fun clearNotifError() { _notifError.value = null }

    fun markNotificationRead(notifId: String) = viewModelScope.launch {
        val prev = _notifications.value
        _notifications.value = prev.map {
            if (it.id == notifId) it.copy(isRead = true) else it
        }
        runCatching { notifRepo.markRead(notifId) }.onFailure {
            _notifications.value = prev
            _notifError.value = "Impossible de marquer comme lu."
        }
    }

    fun markAllNotificationsRead() = viewModelScope.launch {
        val prev = _notifications.value
        if (prev.none { !it.isRead }) return@launch

        _notifications.value = prev.map { it.copy(isRead = true) }
        runCatching { notifRepo.markAllRead(currentUserId) }.onFailure {
            _notifications.value = prev
            _notifError.value = "Rien n'a été marqué comme lu — réessaie."
        }
    }

    fun deleteNotification(notifId: String) = viewModelScope.launch {
        val prev = _notifications.value
        _notifications.value = prev.filter { it.id != notifId }
        runCatching { notifRepo.delete(notifId) }.onFailure {
            _notifications.value = prev
            _notifError.value = "Impossible de supprimer cette notification."
        }
    }

    private suspend fun handleMentions(
        text: String,
        fromUserId: String,
        fromUsername: String,
        fromAdmin: Boolean,
        postId: String = ""
    ) {
        notifRepo.notifyMentions(
            content = text,
            fromUid = fromUserId,
            fromName = fromUsername,
            postId = postId,
            profiles = _profilesMap.value.values.toList(),
            senderIsAdmin = fromAdmin
        )
        // Notification système côté expéditeur uniquement pour @everyone :
        // les autres arrivent chez les destinataires via FCM.
        if (fromAdmin && Regex("@(everyone|tout_le_monde|tous)", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        ) {
            notif.showEveryoneMentionNotification(fromUsername, text)
        }
    }

    // ── Fil : lecture ─────────────────────────────────────────────────────────

    private fun listenPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            feedRepo.listenToRecentPosts().collect { posts ->
                _allPosts.value = posts
                _isRefreshing.value = false
                _feedLoaded.value = true
                // Un verdict qui bascule peut rendre un pari gagnant : on
                // vérifie à chaque rafraîchissement du fil, pas seulement à
                // l'ouverture de l'app.
                settleMyBets()
            }
        }
    }

    fun refreshFeed() {
        _isRefreshing.value = true
        _olderPosts.value = emptyList()
        _hasMorePosts.value = true
        listenPosts()
    }

    /**
     * Charge la page suivante de rumeurs, au-delà de la fenêtre temps réel.
     * Appelé quand on arrive au bout du fil : on remonte ainsi jusqu'à la
     * toute première rumeur, comme avant la pagination.
     */
    fun loadMorePosts() = viewModelScope.launch {
        if (_isLoadingMore.value || !_hasMorePosts.value) return@launch
        val oldest = allKnownPosts.value.minByOrNull { it.timestamp } ?: return@launch

        _isLoadingMore.value = true
        runCatching { feedRepo.getPostsPage(limit = PAGE_SIZE_FEED, after = oldest) }
            .onSuccess { page ->
                if (page.isEmpty()) {
                    _hasMorePosts.value = false
                } else {
                    val known = allKnownPosts.value.map { it.id }.toSet()
                    _olderPosts.value = _olderPosts.value + page.filterNot { it.id in known }
                    _hasMorePosts.value = page.size >= PAGE_SIZE_FEED
                }
            }
            .onFailure { _hasMorePosts.value = false }
        _isLoadingMore.value = false
    }

    /** Charge le salon d'un tag. */
    fun openTag(tag: String) = viewModelScope.launch {
        _tagPosts.value = emptyList()
        _tagPosts.value = feedRepo.getPostsByTag(tag)
        val current = settings.questProgress.first()
        settings.setQuestProgress(Quests.record(current, QuestKind.OPEN_TAG))
    }

    fun loadLeaderboards() = viewModelScope.launch {
        _topClout.value = profileRepo.topByClout()
        _topXp.value = profileRepo.topByXp()
        _topStreak.value = profileRepo.topByStreak()
    }

    // ── Fil : écriture ────────────────────────────────────────────────────────

    fun createPost(
        content: String,
        type: String = "normal",
        pollOpt1: String = "",
        pollOpt2: String = "",
        tags: List<String> = emptyList()
    ) {
        createPostWithMedia(
            content = content, type = type,
            pollOpt1 = pollOpt1, pollOpt2 = pollOpt2, tags = tags
        )
    }

    fun createPostWithMedia(
        content: String,
        type: String = "normal",
        pollOpt1: String = "",
        pollOpt2: String = "",
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        audioFile: File? = null,
        /** Durée de la note vocale. Sans elle, le lecteur affichait « 0:00 ». */
        audioSeconds: Int = 0,
        fileUri: Uri? = null,
        tags: List<String> = emptyList(),
        ephemeral: Boolean = false,
        /** Texte réellement scellé. Non vide ⇒ la rumeur devient une capsule. */
        sealedContent: String = "",
        /** Délai avant ouverture, en heures. */
        sealHours: Int = 0,
        /** true ⇒ la rumeur est un Téléphone arabe, ouvert aux maillons. */
        isChain: Boolean = false
    ) {
        val profile = _myProfile.value ?: return
        val isConf = type == POST_TYPE_CONFESSION
        val fromAdmin = isAdmin(currentUserId) || profile.isAdmin

        // La signature de l'app : toute rumeur s'ouvre par « Askip ».
        // Un sondage pose une question, il n'est pas une rumeur — il en est
        // dispensé, comme depuis le début.
        val trimmed = content.trim()
        val postContent = when {
            trimmed.isBlank() -> ""
            type == "poll" -> trimmed
            trimmed.startsWith("Askip ", ignoreCase = true) -> trimmed
            else -> "Askip $trimmed"
        }
        val allTags = RumorEngine.mergeTags(tags, postContent)
        val sealing = sealedContent.isNotBlank() && sealHours > 0
        val effectiveType = when {
            sealing -> POST_TYPE_SEALED
            isChain -> POST_TYPE_CHAIN
            else -> type
        }

        viewModelScope.launch {
            RateLimiter.check("post", MIN_MS_BETWEEN_POSTS)?.let {
                error.value = it
                return@launch
            }
            RateLimiter.checkHourly("post_hourly", MAX_POSTS_PER_HOUR)?.let {
                error.value = it
                return@launch
            }

            loading.value = true
            _uploadProgress.value = 0
            runCatching {
                var imageUrl = ""
                var videoUrl = ""
                var audioUrl = ""
                var audioDuration = 0
                var fileUrl = ""
                var fileName = ""
                var savedKb = 0

                imageUri?.let {
                    val res = CloudinaryUploader.uploadImage(getApplication(), it) { p ->
                        _uploadProgress.value = p / 4
                    }
                    imageUrl = res.url
                    savedKb = res.savedKb
                    settings.addBytes(res.bytesUploaded.toLong())
                }
                videoUri?.let {
                    val res = CloudinaryUploader.uploadVideo(getApplication(), it) { p ->
                        _uploadProgress.value = 25 + p / 4
                    }
                    videoUrl = res.url
                    settings.addBytes(res.bytesUploaded.toLong())
                }
                audioFile?.let { f ->
                    val res = CloudinaryUploader.uploadAudio(f) { p ->
                        _uploadProgress.value = 50 + p / 4
                    }
                    audioUrl = res.url
                    audioDuration = audioSeconds
                    settings.addBytes(res.bytesUploaded.toLong())
                    f.delete()
                }
                fileUri?.let {
                    val (res, name) = CloudinaryUploader.uploadFile(getApplication(), it) { p ->
                        _uploadProgress.value = 75 + p / 4
                    }
                    fileUrl = res.url
                    fileName = name
                    settings.addBytes(res.bytesUploaded.toLong())
                }

                val now = System.currentTimeMillis()
                val unsealAt = if (sealing) now + sealHours * 3_600_000L else 0L
                val postId = feedRepo.createPost(
                    mapOf(
                        "userId" to currentUserId,
                        "username" to if (isConf) "" else profile.username,
                        "userPhotoUrl" to if (isConf) "" else profile.photoUrl,
                        "content" to postContent,
                        "postType" to effectiveType,
                        "isAnonymous" to isConf,
                        // Une capsule ne montre rien avant l'heure : l'image
                        // part avec le contenu scellé, pas sur le post.
                        "imageUrl" to if (sealing) "" else imageUrl,
                        "videoUrl" to videoUrl,
                        "audioUrl" to audioUrl,
                        "audioDuration" to audioDuration,
                        "fileUrl" to fileUrl,
                        "fileName" to fileName,
                        "pollOption1" to pollOpt1,
                        "pollOption2" to pollOpt2,
                        "pollVotes1" to 0,
                        "pollVotes2" to 0,
                        "pollVoters" to emptyList<String>(),
                        "likedBy" to emptyList<String>(),
                        "fireBy" to emptyList<String>(),
                        "lolBy" to emptyList<String>(),
                        "shockBy" to emptyList<String>(),
                        "eyesBy" to emptyList<String>(),
                        "credibleBy" to emptyList<String>(),
                        "fakeBy" to emptyList<String>(),
                        "scoopBy" to emptyList<String>(),
                        "tags" to allTags,
                        "commentCount" to 0,
                        "isPinned" to false,
                        "isEdited" to false,
                        "expiresAt" to if (ephemeral) now + 86_400_000L else 0L,
                        // Capsule
                        "sealedUntil" to unsealAt,
                        "keysNeeded" to if (sealing) SEAL_KEYS_TO_OPEN else 0,
                        "keysBy" to emptyList<String>(),
                        // Chaîne — le maillon d'origine est le texte du post
                        "chainCount" to if (isChain) 1 else 0,
                        "chainAuthors" to if (isChain) listOf(currentUserId) else emptyList(),
                        "chainLastContent" to "",
                        "chainLastAuthor" to "",
                        "repliedBy" to emptyList<String>(),
                        "timestamp" to now
                    )
                )

                if (sealing) {
                    feedRepo.sealContent(
                        postId = postId,
                        authorId = currentUserId,
                        content = sealedContent.trim(),
                        imageUrl = imageUrl,
                        unsealAt = unsealAt,
                        keysNeeded = SEAL_KEYS_TO_OPEN
                    )
                }

                val field = when (effectiveType) {
                    POST_TYPE_CONFESSION -> "confessionsCount"
                    "poll" -> "pollsCount"
                    else -> "postsCount"
                }
                profileRepo.incrementCounter(currentUserId, field)
                profileRepo.updateStreak(currentUserId)
                award(XP_POST, if (isConf) QuestKind.CONFESS else QuestKind.POST)
                profileRepo.getProfile(currentUserId)?.let { checkAchievements(it) }

                if (!isConf) {
                    notifRepo.notifyNewPost(
                        fromUid = currentUserId,
                        fromName = profile.username,
                        postId = postId,
                        preview = postContent,
                        senderIsAdmin = fromAdmin
                    )
                    if (fromAdmin) {
                        notif.showAdminPostNotification(profile.username, postContent)
                    } else if (notifyPosts.value) {
                        notif.showPostNotification(profile.username, postContent)
                    }
                    handleMentions(postContent, currentUserId, profile.username, fromAdmin, postId)
                }
                savedKb
            }.onSuccess { saved ->
                info.value = if (saved > 0) "Publié — $saved Ko économisés" else "Publié ✓"
            }.onFailure {
                error.value = it.message
            }
            loading.value = false
            _uploadProgress.value = 0
        }
    }

    /**
     * Publie dans l'actualité vocale.
     *
     * Aucun texte n'est envoyé : ni contenu, ni légende. Une actualité vocale
     * qui accepterait une phrase de résumé cesserait d'être vocale — on lirait
     * le résumé et on n'écouterait jamais.
     */
    fun createVoicePost(
        file: File,
        seconds: Int,
        tags: List<String> = emptyList()
    ) = createPostWithMedia(
        content = "",
        type = POST_TYPE_VOICE,
        audioFile = file,
        audioSeconds = seconds,
        tags = tags
    )

    /**
     * Publie sur la Page de Vérité, sous serment.
     *
     * Le serment n'est pas une case à cocher décorative : c'est ce qui rend le
     * verdict du campus signifiant. Une rumeur démentie ici devient un parjure,
     * et la page en tient le compte.
     */
    fun createTruthPost(
        content: String,
        tags: List<String> = emptyList()
    ) = createPostWithMedia(
        content = content,
        type = POST_TYPE_TRUTH,
        tags = tags
    )

    fun editPost(post: Post, newContent: String) = viewModelScope.launch {
        if (post.userId != currentUserId) {
            error.value = "Tu ne peux modifier que tes rumeurs"
            return@launch
        }
        val clean = newContent.trim()
        runCatching {
            feedRepo.editPost(post.id, clean, RumorEngine.mergeTags(post.tags, clean))
        }.onSuccess { info.value = "Rumeur modifiée ✓" }
            .onFailure { error.value = it.message }
    }

    fun deletePost(postId: String) = viewModelScope.launch {
        val post = findPost(postId)
        if (post != null && post.userId != currentUserId && !isAdmin(currentUserId)) {
            error.value = "Action non autorisée"
            return@launch
        }
        // Retrait immédiat : le listener confirmera derrière.
        _allPosts.value = _allPosts.value.filterNot { it.id == postId }
        _olderPosts.value = _olderPosts.value.filterNot { it.id == postId }
        runCatching {
            feedRepo.deletePost(postId)
            post?.let {
                val field = when (it.postType) {
                    POST_TYPE_CONFESSION -> "confessionsCount"
                    "poll" -> "pollsCount"
                    else -> "postsCount"
                }
                profileRepo.incrementCounter(it.userId, field, -1L)
            }
        }.onFailure {
            error.value = it.message
            refreshFeed()
        }
    }

    fun togglePin(post: Post) = viewModelScope.launch {
        if (!isAdmin(currentUserId)) return@launch
        runCatching { feedRepo.setPinned(post.id, !post.isPinned) }
            .onFailure { error.value = it.message }
    }

    fun reportPost(post: Post, reason: String, details: String = "") = viewModelScope.launch {
        runCatching { feedRepo.reportPost(post.id, currentUserId, reason, details.trim()) }
            .onSuccess { info.value = "Signalement envoyé. Merci." }
            .onFailure { error.value = it.message }
    }

    // ── Réactions ─────────────────────────────────────────────────────────────

    // ── Mises à jour optimistes ───────────────────────────────────────────────
    // Une rumeur peut vivre dans la fenêtre temps réel **ou** dans les pages
    // plus anciennes : ces aides touchent les deux, sinon réagir à un vieux
    // post n'aurait aucun effet visible.

    private data class PostsSnapshot(val recent: List<Post>, val older: List<Post>)

    private fun snapshotPosts() = PostsSnapshot(_allPosts.value, _olderPosts.value)

    private fun restorePosts(snapshot: PostsSnapshot) {
        _allPosts.value = snapshot.recent
        _olderPosts.value = snapshot.older
    }

    private fun patchPost(postId: String, transform: (Post) -> Post) {
        _allPosts.value = _allPosts.value.map { if (it.id == postId) transform(it) else it }
        _olderPosts.value = _olderPosts.value.map { if (it.id == postId) transform(it) else it }
    }

    private fun findPost(postId: String): Post? = allKnownPosts.value.find { it.id == postId }

    /**
     * Bascule la réaction. L'affichage change tout de suite ; si le réseau
     * refuse, on revient en arrière — c'est ce qui rend le fil réactif même
     * en 3G capricieuse.
     */
    fun toggleReaction(post: Post, emoji: String) = viewModelScope.launch {
        val uid = currentUserId
        val previous = post.getUserReaction(uid)
        val next = if (previous == emoji) null else emoji

        val snapshot = snapshotPosts()
        patchPost(post.id) { it.withReaction(uid, next) }

        runCatching { feedRepo.setReaction(post.id, uid, next, previous) }
            .onSuccess { if (next != null) award(XP_REACTION, QuestKind.REACT) }
            .onFailure {
                restorePosts(snapshot)
                error.value = "Réaction non enregistrée"
            }
    }

    // ── Rumeur-mètre ──────────────────────────────────────────────────────────

    /**
     * Tranche sur une rumeur. Revoter la même chose retire le vote.
     * Le clout de l'auteur suit : croire rapporte, démonter coûte.
     */
    fun voteVerdict(post: Post, credible: Boolean) = viewModelScope.launch {
        val uid = currentUserId
        if (post.userId == uid) {
            error.value = "On ne vote pas sur sa propre rumeur"
            return@launch
        }

        val previous = post.myVerdictVote(uid)
        val next = if (previous == credible) null else credible

        val snapshot = snapshotPosts()
        patchPost(post.id) { it.withVerdictVote(uid, next) }

        runCatching { feedRepo.setVerdictVote(post.id, uid, next) }
            .onSuccess {
                // Le clout bouge du delta : on annule l'effet de l'ancien vote
                // avant d'appliquer le nouveau, sinon changer d'avis compterait double.
                val delta = cloutOf(next) - cloutOf(previous)
                if (delta != 0L) profileRepo.addClout(post.userId, delta)
                if (next != null) award(XP_VERDICT, QuestKind.VERDICT)
                syncVerdictOutcome(post.id)
            }
            .onFailure {
                restorePosts(snapshot)
                error.value = "Vote non enregistré"
            }
    }

    private fun cloutOf(vote: Boolean?): Long = when (vote) {
        true -> CLOUT_PER_CREDIBLE
        false -> CLOUT_PER_FAKE
        null -> 0L
    }

    /**
     * Quand une rumeur bascule en « confirmée » ou « démentie », on en garde
     * la trace sur le profil de l'auteur : c'est ce qui construit sa fiabilité.
     */
    private suspend fun syncVerdictOutcome(postId: String) {
        val before = findPost(postId) ?: return
        val fresh = feedRepo.getPost(postId) ?: return
        val verdict = fresh.verdict()
        if (verdict == before.verdict()) return
        when (verdict) {
            Verdict.CONFIRMED ->
                profileRepo.incrementCounter(fresh.userId, "confirmedRumors")
            Verdict.DEBUNKED ->
                profileRepo.incrementCounter(fresh.userId, "debunkedRumors")
            else -> Unit
        }
    }

    /** Offre le Scoop du jour. Un seul par personne et par jour. */
    fun giveScoop(post: Post) = viewModelScope.launch {
        val uid = currentUserId
        if (uid in post.scoopBy) return@launch
        if (post.userId == uid) {
            error.value = "On ne s'offre pas son propre Scoop"
            return@launch
        }
        if (!profileRepo.consumeScoop(uid)) {
            error.value = "Scoop déjà utilisé aujourd'hui — il revient à minuit"
            return@launch
        }

        val snapshot = snapshotPosts()
        patchPost(post.id) { it.withScoop(uid) }

        runCatching { feedRepo.giveScoop(post.id, uid) }
            .onSuccess {
                profileRepo.addClout(post.userId, CLOUT_PER_SCOOP)
                award(0L, QuestKind.SCOOP)
                info.value = "Scoop offert 💎"
            }
            .onFailure {
                restorePosts(snapshot)
                error.value = "Scoop non enregistré"
            }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Le Pari
    // ═════════════════════════════════════════════════════════════════════════

    private fun listenMyBets() {
        betsJob?.cancel()
        betsJob = viewModelScope.launch {
            feedRepo.listenToMyBets(currentUserId)
                .catch { /* index de groupe manquant — silencieux */ }
                .collect {
                    _myBets.value = it
                    settleMyBets()
                }
        }
    }

    /**
     * Mise des jetons sur un verdict à venir.
     *
     * La cote est figée maintenant, à partir de l'état du vote à cet instant :
     * c'est tout l'intérêt du pari précoce, et ça empêche de miser une fois le
     * verdict devenu évident.
     */
    fun placeBet(post: Post, onCredible: Boolean, stake: Int) = viewModelScope.launch {
        val uid = currentUserId
        if (post.userId == uid) {
            error.value = "On ne parie pas sur sa propre rumeur"
            return@launch
        }
        if (post.id in _myBets.value) {
            error.value = "Tu as déjà parié sur cette rumeur"
            return@launch
        }
        if (post.verdict() != Verdict.INVESTIGATING && post.verdict() != Verdict.CONTESTED) {
            error.value = "Le verdict est déjà tombé"
            return@launch
        }
        if (!profileRepo.consumeBetTokens(uid, stake)) {
            error.value = "Plus de jetons aujourd'hui — ils reviennent à minuit"
            return@launch
        }

        val votes = post.verdictVotes()
        val ratio = RumorEngine.sideRatio(post.credibleBy.size, post.fakeBy.size, onCredible)
        val bet = Bet(
            userId = uid,
            postId = post.id,
            onCredible = onCredible,
            stake = stake,
            ratioAtBet = ratio,
            votesAtBet = votes,
            odds = RumorEngine.betOdds(ratio, votes),
            timestamp = System.currentTimeMillis()
        )

        runCatching { feedRepo.placeBet(bet) }
            .onSuccess {
                _myBets.value = _myBets.value + (post.id to bet)
                award(XP_BET, QuestKind.BET)
                info.value = "Pari posé — cote ×${"%.1f".format(bet.odds)}"
            }
            .onFailure { error.value = "Pari non enregistré" }
    }

    /**
     * Règle mes paris dont la rumeur a tranché depuis.
     *
     * Chacun règle **uniquement le sien** : c'est ce qui permet de s'en passer
     * de Cloud Function tout en gardant des règles Firestore serrées.
     */
    private fun settleMyBets() = viewModelScope.launch {
        val pending = _myBets.value.values.filter { !it.settled }
        if (pending.isEmpty()) return@launch

        pending.forEach { bet ->
            // La rumeur peut être sortie de la fenêtre chargée depuis le pari :
            // sans ce repli, un pari sur une vieille rumeur ne se réglerait jamais.
            val post = findPost(bet.postId)
                ?: feedRepo.getPost(bet.postId)
                ?: return@forEach
            val verdict = post.verdict()
            val truth = when (verdict) {
                Verdict.CONFIRMED -> true
                Verdict.DEBUNKED -> false
                else -> return@forEach   // pas encore tranché
            }
            val won = bet.onCredible == truth
            val payout = if (won) bet.potentialGain() else -(BET_PENALTY * bet.stake)

            runCatching {
                feedRepo.settleBet(bet.postId, currentUserId, won, payout)
                profileRepo.recordBetResult(currentUserId, won, payout)
            }.onSuccess {
                _myBets.value = _myBets.value + (
                    bet.postId to bet.copy(payout = payout).also {
                        it.settled = true
                        it.won = won
                    }
                    )
                info.value = if (won) "Pari gagné : +$payout clout 🎉"
                else "Pari perdu : $payout clout"
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  La Capsule scellée
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Demande le contenu d'une capsule.
     * Avant l'heure, le serveur refuse et on ne stocke rien — l'app n'a
     * simplement jamais le texte entre les mains.
     */
    fun revealSealed(postId: String) = viewModelScope.launch {
        if (postId in _sealedContents.value) return@launch
        feedRepo.readSealed(postId)?.let {
            _sealedContents.value = _sealedContents.value + (postId to it)
        }
    }

    /** Donne sa clé. Assez de clés et la capsule s'ouvre pour tout le campus. */
    fun giveKey(post: Post) = viewModelScope.launch {
        val uid = currentUserId
        if (uid in post.keysBy) return@launch

        val snapshot = snapshotPosts()
        patchPost(post.id) { it.withKey(uid) }

        runCatching { feedRepo.giveKey(post.id, uid) }
            .onSuccess {
                award(0L, QuestKind.KEY)
                val left = (post.keysNeeded - post.keysBy.size - 1).coerceAtLeast(0)
                info.value = if (left == 0) "Capsule ouverte 🔓"
                else "Clé donnée — encore $left"
            }
            .onFailure {
                restorePosts(snapshot)
                error.value = "Clé non enregistrée"
            }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Le Téléphone arabe
    // ═════════════════════════════════════════════════════════════════════════

    fun listenChain(postId: String) {
        chainJob?.cancel()
        _chainLinks.value = emptyList()
        chainJob = viewModelScope.launch {
            feedRepo.listenToChain(postId).collect { _chainLinks.value = it }
        }
    }

    /** Ajoute une phrase à la chaîne. Une seule par personne, sept au total. */
    fun addChainLink(post: Post, content: String, anonymous: Boolean = false) =
        viewModelScope.launch {
            val profile = _myProfile.value ?: return@launch
            val uid = currentUserId
            if (!post.canAddLink(uid, CHAIN_MAX_LINKS)) {
                error.value = when {
                    uid in post.chainAuthors -> "Tu as déjà ajouté ton maillon"
                    post.chainIsFull(CHAIN_MAX_LINKS) -> "La chaîne est complète"
                    else -> "Impossible d'ajouter un maillon"
                }
                return@launch
            }
            val clean = content.trim().take(CHAIN_MAX_LENGTH)
            if (clean.isBlank()) return@launch

            RateLimiter.check("chain", MIN_MS_BETWEEN_COMMENTS)?.let {
                error.value = it
                return@launch
            }

            runCatching {
                feedRepo.addChainLink(
                    postId = post.id,
                    index = post.chainCount + 1,
                    userId = uid,
                    username = profile.username,
                    userPhotoUrl = profile.photoUrl,
                    content = clean,
                    isAnonymous = anonymous
                )
            }.onSuccess {
                award(XP_CHAIN_LINK, QuestKind.CHAIN)
                info.value = "Maillon ajouté 📞"
            }.onFailure { error.value = it.message }
        }

    // ═════════════════════════════════════════════════════════════════════════
    //  Le droit de réponse
    // ═════════════════════════════════════════════════════════════════════════

    fun listenReplies(postId: String) {
        repliesJob?.cancel()
        _mentionReplies.value = emptyList()
        repliesJob = viewModelScope.launch {
            feedRepo.listenToReplies(postId).collect { _mentionReplies.value = it }
        }
    }

    /** Cette rumeur me cite-t-elle nommément ? */
    fun mentionsMe(post: Post): Boolean {
        val me = _myProfile.value?.username ?: return false
        return post.userId != currentUserId && RumorEngine.mentions(post.content, me)
    }

    /**
     * Publie une réponse officielle sur une rumeur qui te cite.
     * Elle est épinglée en tête du post : la version de la personne visée ne
     * doit pas se perdre au milieu des commentaires.
     */
    fun publishRightOfReply(post: Post, content: String) = viewModelScope.launch {
        val profile = _myProfile.value ?: return@launch
        if (!mentionsMe(post)) {
            error.value = "Le droit de réponse ne vaut que si la rumeur te cite"
            return@launch
        }
        val clean = content.trim().take(MAX_COMMENT_LENGTH)
        if (clean.isBlank()) return@launch

        runCatching {
            feedRepo.setMentionReply(
                MentionReply(
                    userId = currentUserId,
                    username = profile.username,
                    userPhotoUrl = profile.photoUrl,
                    postId = post.id,
                    content = clean,
                    timestamp = System.currentTimeMillis()
                )
            )
        }.onSuccess { info.value = "Réponse publiée ⚖️" }
            .onFailure { error.value = it.message }
    }

    fun removeRightOfReply(postId: String) = viewModelScope.launch {
        runCatching { feedRepo.deleteMentionReply(postId, currentUserId) }
            .onFailure { error.value = it.message }
    }

    fun votePoll(postId: String, option: Int) = viewModelScope.launch {
        val uid = currentUserId
        val post = findPost(postId) ?: return@launch
        if (uid in post.pollVoters) return@launch

        val snapshot = snapshotPosts()
        patchPost(postId) { it.withVote(uid, option) }

        runCatching { feedRepo.votePoll(postId, uid, option) }
            .onFailure {
                restorePosts(snapshot)
                error.value = "Vote non enregistré"
            }
    }

    // ── Stories ───────────────────────────────────────────────────────────────

    private fun listenStories() {
        storiesJob?.cancel()
        storiesJob = viewModelScope.launch {
            feedRepo.listenToActiveStories().collect { raw ->
                _stories.value = raw.map { s ->
                    val live = _profilesMap.value[s.userId]?.username
                    if (live != null && live != s.username) s.copy(username = live) else s
                }
            }
        }
    }

    fun createStory(content: String, emoji: String, bgColor: String) {
        val profile = _myProfile.value ?: return
        viewModelScope.launch {
            RateLimiter.check("story", MIN_MS_BETWEEN_POSTS)?.let {
                error.value = it
                return@launch
            }
            runCatching {
                feedRepo.createStory(
                    userId = currentUserId,
                    username = profile.username,
                    userPhotoUrl = profile.photoUrl,
                    content = content,
                    emoji = emoji,
                    colorHex = bgColor
                )
                profileRepo.incrementCounter(currentUserId, "storiesCount")
                profileRepo.updateStreak(currentUserId)
                award(XP_STORY, QuestKind.STORY)
                profileRepo.getProfile(currentUserId)?.let { checkAchievements(it) }
            }.onFailure { error.value = it.message }
        }
    }

    fun deleteStory(storyId: String) = viewModelScope.launch {
        runCatching {
            val story = _stories.value.find { it.id == storyId }
            feedRepo.deleteStory(storyId)
            story?.let { profileRepo.incrementCounter(it.userId, "storiesCount", -1L) }
        }.onFailure { error.value = it.message }
    }

    // ── Commentaires ──────────────────────────────────────────────────────────

    /**
     * Ouvre une rumeur : charge son contenu puis écoute ses commentaires.
     * La rumeur peut être hors de la fenêtre chargée (lien depuis une
     * notification, un classement…), d'où le repli sur une lecture directe.
     */
    fun openPost(postId: String) = viewModelScope.launch {
        val post = findPost(postId) ?: feedRepo.getPost(postId)
        _viewedPost.value = post
        listenComments(postId)
        listenReplies(postId)
        if (post?.isChain() == true) listenChain(postId)
        if (post?.isSealed() == true) revealSealed(postId)
    }

    fun listenComments(postId: String) {
        commentJob?.cancel()
        _rawComments.value = emptyList()
        commentJob = viewModelScope.launch {
            feedRepo.listenToComments(postId).collect { _rawComments.value = it }
        }
    }

    /** [anonymous] permet de répondre masqué sous une confession. */
    fun addComment(postId: String, content: String, anonymous: Boolean = false) {
        val profile = _myProfile.value ?: return
        val fromAdmin = isAdmin(currentUserId) || profile.isAdmin
        viewModelScope.launch {
            RateLimiter.check("comment", MIN_MS_BETWEEN_COMMENTS)?.let {
                error.value = it
                return@launch
            }
            runCatching {
                feedRepo.addComment(
                    postId,
                    mapOf(
                        "postId" to postId,
                        "userId" to currentUserId,
                        "username" to if (anonymous) "" else profile.username,
                        "userPhotoUrl" to if (anonymous) "" else profile.photoUrl,
                        "content" to content,
                        "isAnonymous" to anonymous,
                        "likedBy" to emptyList<String>(),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                profileRepo.incrementCounter(currentUserId, "commentsCount")
                profileRepo.updateStreak(currentUserId)
                award(XP_COMMENT, QuestKind.COMMENT)
                profileRepo.getProfile(currentUserId)?.let { checkAchievements(it) }
                if (!anonymous) {
                    handleMentions(content, currentUserId, profile.username, fromAdmin, postId)
                }
            }.onFailure { error.value = it.message }
        }
    }

    /**
     * Répond par la voix.
     *
     * Sous une rumeur vocale, c'est la seule réponse possible : le champ texte
     * disparaît. Ailleurs, ça reste proposé — une voix dit des choses qu'un
     * message écrit ne dit pas.
     */
    fun addVoiceComment(
        postId: String,
        file: File,
        seconds: Int,
        anonymous: Boolean = false
    ) = viewModelScope.launch {
        val profile = _myProfile.value ?: run { file.delete(); return@launch }

        RateLimiter.check("comment", MIN_MS_BETWEEN_COMMENTS)?.let {
            error.value = it
            file.delete()
            return@launch
        }

        loading.value = true
        _uploadProgress.value = 0
        runCatching {
            val res = CloudinaryUploader.uploadAudio(file) { _uploadProgress.value = it }
            settings.addBytes(res.bytesUploaded.toLong())
            file.delete()

            feedRepo.addComment(
                postId,
                mapOf(
                    "postId" to postId,
                    "userId" to currentUserId,
                    "username" to if (anonymous) "" else profile.username,
                    "userPhotoUrl" to if (anonymous) "" else profile.photoUrl,
                    "content" to "",
                    "audioUrl" to res.url,
                    "audioDuration" to seconds,
                    "isAnonymous" to anonymous,
                    "likedBy" to emptyList<String>(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
            profileRepo.incrementCounter(currentUserId, "commentsCount")
            profileRepo.updateStreak(currentUserId)
            award(XP_COMMENT, QuestKind.COMMENT)
        }.onFailure {
            error.value = it.message
            file.delete()
        }
        loading.value = false
        _uploadProgress.value = 0
    }

    fun deleteComment(postId: String, commentId: String) = viewModelScope.launch {
        runCatching { feedRepo.deleteComment(postId, commentId) }
            .onFailure { error.value = it.message }
    }

    fun toggleCommentLike(postId: String, comment: Comment) = viewModelScope.launch {
        val uid = currentUserId
        val liked = !comment.isLikedBy(uid)
        _rawComments.value = _rawComments.value.map { c ->
            if (c.id != comment.id) c
            else c.copy(
                likedBy = if (liked) c.likedBy + uid else c.likedBy.filterNot { it == uid }
            )
        }
        runCatching { feedRepo.toggleCommentLike(postId, comment.id, uid, liked) }
    }

    // ── Conversations ─────────────────────────────────────────────────────────

    private fun listenConversations() {
        convJob?.cancel()
        convJob = viewModelScope.launch {
            msgRepo.listenToConversations(currentUserId).collect { _rawConversations.value = it }
        }
    }

    fun startConversation(otherId: String, otherUsername: String, onDone: (String) -> Unit) {
        val me = _myProfile.value ?: return
        viewModelScope.launch {
            runCatching {
                val convId = msgRepo.getOrCreateConversation(
                    currentUserId, me.username,
                    otherId, resolveUsername(otherId, otherUsername)
                )
                profileRepo.incrementCounter(currentUserId, "convsStarted")
                profileRepo.getProfile(currentUserId)?.let { checkAchievements(it) }
                onDone(convId)
            }.onFailure { error.value = it.message }
        }
    }

    fun listenMessages(convId: String) {
        msgJob?.cancel()
        msgJob = viewModelScope.launch {
            msgRepo.listenToMessages(convId).collect { newMsgs ->
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
                                    msg.isFile() -> "📎 ${msg.mediaName}"
                                    else -> msg.content
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
        val profile = _myProfile.value ?: return
        val conv = _rawConversations.value.find { it.id == convId } ?: return
        val receiverId = conv.participants.firstOrNull { it != currentUserId } ?: return
        val fromAdmin = isAdmin(currentUserId) || profile.isAdmin

        viewModelScope.launch {
            val hasMedia = imageUri != null || videoUri != null || fileUri != null
            if (hasMedia) {
                loading.value = true
                _uploadProgress.value = 0
            }
            runCatching {
                var mediaUrl = ""
                var mediaType = ""
                var mediaName = ""
                when {
                    imageUri != null -> {
                        val res = CloudinaryUploader.uploadImage(getApplication(), imageUri) {
                            _uploadProgress.value = it
                        }
                        mediaUrl = res.url
                        mediaType = "image"
                        settings.addBytes(res.bytesUploaded.toLong())
                    }
                    videoUri != null -> {
                        val res = CloudinaryUploader.uploadVideo(getApplication(), videoUri) {
                            _uploadProgress.value = it
                        }
                        mediaUrl = res.url
                        mediaType = "video"
                        settings.addBytes(res.bytesUploaded.toLong())
                    }
                    fileUri != null -> {
                        val (res, name) = CloudinaryUploader.uploadFile(getApplication(), fileUri) {
                            _uploadProgress.value = it
                        }
                        mediaUrl = res.url
                        mediaType = "file"
                        mediaName = name
                        settings.addBytes(res.bytesUploaded.toLong())
                    }
                }
                val preview = when (mediaType) {
                    "image" -> "📸 Photo"
                    "video" -> "🎥 Vidéo"
                    "file" -> "📎 $mediaName"
                    "audio" -> "🎤 Vocal"
                    else -> content.take(80)
                }
                msgRepo.sendMessage(
                    convId,
                    mapOf(
                        "conversationId" to convId,
                        "senderId" to currentUserId,
                        "senderUsername" to profile.username,
                        "content" to content,
                        "mediaUrl" to mediaUrl,
                        "mediaType" to mediaType,
                        "mediaName" to mediaName,
                        "mediaDuration" to 0,
                        "timestamp" to System.currentTimeMillis()
                    ),
                    receiverId
                )
                notifRepo.create(
                    mapOf(
                        "targetUserId" to receiverId,
                        "type" to "message",
                        "fromUserId" to currentUserId,
                        "fromUsername" to profile.username,
                        "fromIsAdmin" to fromAdmin,
                        "postId" to "",
                        "conversationId" to convId,
                        "content" to preview,
                        "isRead" to false,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }.onFailure { error.value = it.message }
            loading.value = false
            _uploadProgress.value = 0
        }
    }

    fun markRead(convId: String) = viewModelScope.launch {
        msgRepo.markRead(convId, currentUserId)
    }

    fun getUnread(conv: Conversation): Int =
        (conv.unreadCounts[currentUserId] ?: 0L).toInt()

    // ── Note vocale ───────────────────────────────────────────────────────────

    fun startVoiceRecording(context: android.content.Context) {
        if (_isRecording.value) return
        voiceRecorder = VoiceRecorder(context)
        runCatching { voiceRecorder?.start() }
            .onSuccess {
                _isRecording.value = true
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
        voiceRecorder = null
        _isRecording.value = false
        if (result == null || convId.isBlank()) return

        val (file, durationSec) = result
        val profile = _myProfile.value ?: run { file.delete(); return }
        val conv = _rawConversations.value.find { it.id == convId }
            ?: run { file.delete(); return }
        val receiverId = conv.participants.firstOrNull { it != currentUserId }
            ?: run { file.delete(); return }
        val fromAdmin = isAdmin(currentUserId) || profile.isAdmin

        viewModelScope.launch {
            loading.value = true
            _uploadProgress.value = 0
            runCatching {
                val res = CloudinaryUploader.uploadAudio(file) { _uploadProgress.value = it }
                settings.addBytes(res.bytesUploaded.toLong())
                file.delete()
                msgRepo.sendMessage(
                    convId,
                    mapOf(
                        "conversationId" to convId,
                        "senderId" to currentUserId,
                        "senderUsername" to profile.username,
                        "content" to "",
                        "mediaUrl" to res.url,
                        "mediaType" to "audio",
                        "mediaName" to "vocal_${durationSec}s.m4a",
                        "mediaDuration" to durationSec,
                        "timestamp" to System.currentTimeMillis()
                    ),
                    receiverId
                )
                notifRepo.create(
                    mapOf(
                        "targetUserId" to receiverId,
                        "type" to "message",
                        "fromUserId" to currentUserId,
                        "fromUsername" to profile.username,
                        "fromIsAdmin" to fromAdmin,
                        "postId" to "",
                        "conversationId" to convId,
                        "content" to "🎤 Message vocal (${durationSec}s)",
                        "isRead" to false,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }.onFailure {
                error.value = it.message
                file.delete()
            }
            loading.value = false
            _uploadProgress.value = 0
        }
    }

    fun stopRecordingForPost(): Pair<File, Int>? {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null
        _isRecording.value = false
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

    // ── Groupes ───────────────────────────────────────────────────────────────

    private fun listenGroups() {
        groupJob?.cancel()
        groupJob = viewModelScope.launch {
            msgRepo.listenToGroups(currentUserId).collect { _rawGroups.value = it }
        }
    }

    fun listenGroupMessages(groupId: String) {
        groupMsgJob?.cancel()
        groupMsgJob = viewModelScope.launch {
            msgRepo.listenToGroupMessages(groupId).collect { msgs ->
                _groupMessages.value = msgs.map { msg ->
                    val live = _profilesMap.value[msg.senderId]?.username
                    if (live != null && live != msg.senderUsername) {
                        msg.copy(senderUsername = live)
                    } else {
                        msg
                    }
                }
            }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        emoji: String,
        memberIds: List<String>,
        onDone: (String) -> Unit
    ) {
        val profile = _myProfile.value ?: return
        val memberNames = memberIds.associateWith { resolveUsername(it, it) }
        val memberPhotos = memberIds.associateWith { resolvePhotoUrl(it) }
        viewModelScope.launch {
            runCatching {
                msgRepo.createGroup(
                    name, description, emoji,
                    currentUserId, profile.username, profile.photoUrl,
                    memberIds, memberNames, memberPhotos
                )
            }.onSuccess { onDone(it) }
                .onFailure { error.value = it.message }
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
            val hasMedia = imageUri != null || videoUri != null || fileUri != null
            if (hasMedia) {
                loading.value = true
                _uploadProgress.value = 0
            }
            runCatching {
                var mediaUrl = ""
                var mediaType = ""
                var mediaName = ""
                when {
                    imageUri != null -> {
                        val res = CloudinaryUploader.uploadImage(getApplication(), imageUri) {
                            _uploadProgress.value = it
                        }
                        mediaUrl = res.url
                        mediaType = "image"
                        settings.addBytes(res.bytesUploaded.toLong())
                    }
                    videoUri != null -> {
                        val res = CloudinaryUploader.uploadVideo(getApplication(), videoUri) {
                            _uploadProgress.value = it
                        }
                        mediaUrl = res.url
                        mediaType = "video"
                        settings.addBytes(res.bytesUploaded.toLong())
                    }
                    fileUri != null -> {
                        val (res, name) = CloudinaryUploader.uploadFile(getApplication(), fileUri) {
                            _uploadProgress.value = it
                        }
                        mediaUrl = res.url
                        mediaType = "file"
                        mediaName = name
                        settings.addBytes(res.bytesUploaded.toLong())
                    }
                }
                msgRepo.sendGroupMessage(
                    groupId,
                    mapOf(
                        "groupId" to groupId,
                        "senderId" to currentUserId,
                        "senderUsername" to profile.username,
                        "content" to content,
                        "mediaUrl" to mediaUrl,
                        "mediaType" to mediaType,
                        "mediaName" to mediaName,
                        "mediaDuration" to 0,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }.onFailure { error.value = it.message }
            loading.value = false
            _uploadProgress.value = 0
        }
    }

    fun stopAndSendGroupVoice(groupId: String) {
        recordingTimerJob?.cancel()
        val result = voiceRecorder?.stop()
        voiceRecorder = null
        _isRecording.value = false
        if (result == null) return

        val (file, durationSec) = result
        val profile = _myProfile.value ?: run { file.delete(); return }

        viewModelScope.launch {
            loading.value = true
            _uploadProgress.value = 0
            runCatching {
                val res = CloudinaryUploader.uploadAudio(file) { _uploadProgress.value = it }
                settings.addBytes(res.bytesUploaded.toLong())
                file.delete()
                msgRepo.sendGroupMessage(
                    groupId,
                    mapOf(
                        "groupId" to groupId,
                        "senderId" to currentUserId,
                        "senderUsername" to profile.username,
                        "content" to "",
                        "mediaUrl" to res.url,
                        "mediaType" to "audio",
                        "mediaName" to "vocal_${durationSec}s.m4a",
                        "mediaDuration" to durationSec,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }.onFailure {
                error.value = it.message
                file.delete()
            }
            loading.value = false
            _uploadProgress.value = 0
        }
    }

    fun leaveGroup(groupId: String) = viewModelScope.launch {
        runCatching { msgRepo.removeGroupMember(groupId, currentUserId) }
            .onFailure { error.value = it.message }
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch {
        runCatching { msgRepo.deleteGroup(groupId) }
            .onFailure { error.value = it.message }
    }

    // ── Modération ────────────────────────────────────────────────────────────

    fun grantENIBadge(uid: String, granted: Boolean) = viewModelScope.launch {
        if (!isAdmin(currentUserId)) return@launch
        runCatching { profileRepo.updateProfile(uid, mapOf("hasBadgeENI" to granted)) }
            .onSuccess { info.value = if (granted) "Badge ENI accordé ✓" else "Badge ENI retiré" }
            .onFailure { error.value = it.message }
    }

    fun banUser(uid: String, banned: Boolean) = viewModelScope.launch {
        if (!isAdmin(currentUserId)) return@launch
        if (uid == currentUserId) {
            error.value = "Tu ne peux pas te bannir toi-même"
            return@launch
        }
        runCatching { profileRepo.updateProfile(uid, mapOf("isBanned" to banned)) }
            .onSuccess { info.value = if (banned) "Utilisateur banni" else "Utilisateur réactivé ✓" }
            .onFailure { error.value = it.message }
    }

    // ── Préférences ───────────────────────────────────────────────────────────

    // Chaque interrupteur est écrit en local **et** sur le profil : le local
    // pilote les notifications affichées par l'app, le profil pilote celles
    // que le serveur envoie quand l'app est fermée.
    fun setNotifyMessages(v: Boolean) = viewModelScope.launch {
        settings.setNotifyMessages(v)
        profileRepo.updateProfile(currentUserId, mapOf("notifyMessages" to v))
    }

    fun setNotifyPosts(v: Boolean) = viewModelScope.launch {
        settings.setNotifyPosts(v)
        profileRepo.updateProfile(currentUserId, mapOf("notifyPosts" to v))
    }

    fun setNotifyMentions(v: Boolean) = viewModelScope.launch {
        settings.setNotifyMentions(v)
        profileRepo.updateProfile(currentUserId, mapOf("notifyMentions" to v))
    }
    fun setTheme(t: AppTheme) = viewModelScope.launch { settings.setTheme(t) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settings.setHaptics(v) }
    fun setReduceMotion(v: Boolean) = viewModelScope.launch { settings.setReduceMotion(v) }

    fun clearError() {
        error.value = null
    }

    fun clearInfo() {
        info.value = null
    }
}
