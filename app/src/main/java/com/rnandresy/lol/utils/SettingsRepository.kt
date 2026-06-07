package com.rnandresy.lol.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rnandresy.lol.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore("askip_settings")

class SettingsRepository(private val context: Context) {

    private val KEY_NOTIFY_MESSAGES = booleanPreferencesKey("notify_messages")
    private val KEY_NOTIFY_POSTS    = booleanPreferencesKey("notify_posts")
    private val KEY_NOTIFY_MENTIONS = booleanPreferencesKey("notify_mentions")
    private val KEY_TOTAL_BYTES     = longPreferencesKey("total_bytes")
    private val KEY_THEME           = stringPreferencesKey("app_theme")

    val notifyMessages: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIFY_MESSAGES] ?: true }
    val notifyPosts: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIFY_POSTS] ?: false }
    val notifyMentions: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIFY_MENTIONS] ?: true }
    val totalBytes: Flow<Long> =
        context.dataStore.data.map { it[KEY_TOTAL_BYTES] ?: 0L }
    val appTheme: Flow<AppTheme> =
        context.dataStore.data.map {
            when (it[KEY_THEME]) {
                "NEON"       -> AppTheme.NEON
                "NOSTALGIC"  -> AppTheme.NOSTALGIC
                else         -> AppTheme.BLACK_WHITE
            }
        }

    suspend fun setNotifyMessages(v: Boolean) =
        context.dataStore.edit { it[KEY_NOTIFY_MESSAGES] = v }
    suspend fun setNotifyPosts(v: Boolean) =
        context.dataStore.edit { it[KEY_NOTIFY_POSTS] = v }
    suspend fun setNotifyMentions(v: Boolean) =
        context.dataStore.edit { it[KEY_NOTIFY_MENTIONS] = v }
    suspend fun addBytes(bytes: Long) =
        context.dataStore.edit { prefs ->
            prefs[KEY_TOTAL_BYTES] = (prefs[KEY_TOTAL_BYTES] ?: 0L) + bytes
        }
    suspend fun setTheme(theme: AppTheme) =
        context.dataStore.edit { it[KEY_THEME] = theme.name }
}