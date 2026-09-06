package com.rnandresy.lol.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rnandresy.lol.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore("askip_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME            = stringPreferencesKey("theme")
        val NOTIFY_MESSAGES  = booleanPreferencesKey("notify_messages")
        val NOTIFY_MENTIONS  = booleanPreferencesKey("notify_mentions")
        val NOTIFY_POSTS     = booleanPreferencesKey("notify_posts")
        val AUTOPLAY_VIDEOS  = booleanPreferencesKey("autoplay_videos")
        val FORCE_DATA_SAVER = booleanPreferencesKey("force_data_saver")
        val TOTAL_BYTES      = longPreferencesKey("total_bytes")
        val POST_DRAFT       = stringPreferencesKey("post_draft")
        val LAST_SEEN_FEED   = longPreferencesKey("last_seen_feed")
        val ONBOARDING_DONE  = booleanPreferencesKey("onboarding_done")
        val QUEST_PROGRESS   = stringPreferencesKey("quest_progress")
        val LAST_DAILY_BONUS = stringPreferencesKey("last_daily_bonus")
        val FEED_TAB         = stringPreferencesKey("feed_tab")
        val FEED_SECTION     = stringPreferencesKey("feed_section")
        val HAPTICS          = booleanPreferencesKey("haptics")
        val REDUCE_MOTION    = booleanPreferencesKey("reduce_motion")
    }

    // ── Thème ─────────────────────────────────────────────────────────────────
    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        runCatching { AppTheme.valueOf(prefs[Keys.THEME] ?: "") }
            .getOrDefault(AppTheme.SAKURA)
    }

    /** Alias historique — plusieurs écrans lisent `appTheme`. */
    val appTheme: Flow<AppTheme> get() = theme

    suspend fun setTheme(t: AppTheme) =
        context.dataStore.edit { it[Keys.THEME] = t.name }

    // ── Notifications ─────────────────────────────────────────────────────────
    val notifyMessages: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFY_MESSAGES] ?: true }
    val notifyMentions: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFY_MENTIONS] ?: true }
    val notifyPosts: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFY_POSTS] ?: true }

    suspend fun setNotifyMessages(v: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFY_MESSAGES] = v }
    suspend fun setNotifyMentions(v: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFY_MENTIONS] = v }
    suspend fun setNotifyPosts(v: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFY_POSTS] = v }

    // ── Économie de données ───────────────────────────────────────────────────
    val autoPlayVideos: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTOPLAY_VIDEOS] ?: false }
    val forceDataSaver: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.FORCE_DATA_SAVER] ?: false }

    suspend fun setAutoPlayVideos(v: Boolean) =
        context.dataStore.edit { it[Keys.AUTOPLAY_VIDEOS] = v }
    suspend fun setForceDataSaver(v: Boolean) =
        context.dataStore.edit { it[Keys.FORCE_DATA_SAVER] = v }

    // ── Consommation ──────────────────────────────────────────────────────────
    val totalBytes: Flow<Long> =
        context.dataStore.data.map { it[Keys.TOTAL_BYTES] ?: 0L }

    suspend fun addBytes(bytes: Long) = context.dataStore.edit {
        it[Keys.TOTAL_BYTES] = (it[Keys.TOTAL_BYTES] ?: 0L) + bytes
    }

    suspend fun resetBytes() = context.dataStore.edit { it[Keys.TOTAL_BYTES] = 0L }

    // ── Brouillon de post ─────────────────────────────────────────────────────
    val postDraft: Flow<String> =
        context.dataStore.data.map { it[Keys.POST_DRAFT] ?: "" }

    suspend fun setPostDraft(text: String) =
        context.dataStore.edit { it[Keys.POST_DRAFT] = text }
    suspend fun clearPostDraft() =
        context.dataStore.edit { it.remove(Keys.POST_DRAFT) }

    // ── Divers ────────────────────────────────────────────────────────────────
    val lastSeenFeed: Flow<Long> =
        context.dataStore.data.map { it[Keys.LAST_SEEN_FEED] ?: 0L }
    suspend fun setLastSeenFeed(ts: Long) =
        context.dataStore.edit { it[Keys.LAST_SEEN_FEED] = ts }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    suspend fun setOnboardingDone() =
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }

    // ── Confort ───────────────────────────────────────────────────────────────
    val haptics: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAPTICS] ?: true }
    suspend fun setHaptics(v: Boolean) =
        context.dataStore.edit { it[Keys.HAPTICS] = v }

    /** Respecte les utilisateurs sensibles au mouvement — coupe les animations longues. */
    val reduceMotion: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.REDUCE_MOTION] ?: false }
    suspend fun setReduceMotion(v: Boolean) =
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = v }

    // ── Onglet du fil ─────────────────────────────────────────────────────────
    val feedTab: Flow<FeedTab> = context.dataStore.data.map { prefs ->
        runCatching { FeedTab.valueOf(prefs[Keys.FEED_TAB] ?: "") }
            .getOrDefault(FeedTab.HOT)
    }
    suspend fun setFeedTab(tab: FeedTab) =
        context.dataStore.edit { it[Keys.FEED_TAB] = tab.name }

    /** Actualité principale, vocale ou Page de Vérité. */
    val feedSection: Flow<FeedSection> = context.dataStore.data.map { prefs ->
        runCatching { FeedSection.valueOf(prefs[Keys.FEED_SECTION] ?: "") }
            .getOrDefault(FeedSection.MAIN)
    }
    suspend fun setFeedSection(section: FeedSection) =
        context.dataStore.edit { it[Keys.FEED_SECTION] = section.name }

    // ── Missions du jour ──────────────────────────────────────────────────────
    // Stockées en local : ce sont des compteurs de confort, pas des données
    // partagées. Seule l'XP gagnée part sur le profil.
    val questProgress: Flow<QuestProgress> = context.dataStore.data.map {
        Quests.normalize(QuestProgress.parse(it[Keys.QUEST_PROGRESS] ?: ""))
    }

    suspend fun setQuestProgress(progress: QuestProgress) =
        context.dataStore.edit { it[Keys.QUEST_PROGRESS] = progress.serialize() }

    /** Jour du dernier bonus de connexion — évite de le donner deux fois. */
    val lastDailyBonus: Flow<String> =
        context.dataStore.data.map { it[Keys.LAST_DAILY_BONUS] ?: "" }
    suspend fun setLastDailyBonus(date: String) =
        context.dataStore.edit { it[Keys.LAST_DAILY_BONUS] = date }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}