package com.vortex.downloader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vortex.downloader.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vortex_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultVideoQualityHeight: Int = 1080,
    val defaultAudioBitrateKbps: Int = 320,
    val videoSubfolder: String = "Movies/VORTEX",
    val audioSubfolder: String = "Music/VORTEX",
    val maxSimultaneousDownloads: Int = 2,
    val wifiOnly: Boolean = false,
    val notificationsEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_VIDEO_HEIGHT = intPreferencesKey("default_video_height")
        val DEFAULT_AUDIO_BITRATE = intPreferencesKey("default_audio_bitrate")
        val VIDEO_SUBFOLDER = stringPreferencesKey("video_subfolder")
        val AUDIO_SUBFOLDER = stringPreferencesKey("audio_subfolder")
        val MAX_SIMULTANEOUS = intPreferencesKey("max_simultaneous")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.DARK,
            defaultVideoQualityHeight = prefs[Keys.DEFAULT_VIDEO_HEIGHT] ?: 1080,
            defaultAudioBitrateKbps = prefs[Keys.DEFAULT_AUDIO_BITRATE] ?: 320,
            videoSubfolder = prefs[Keys.VIDEO_SUBFOLDER] ?: "Movies/VORTEX",
            audioSubfolder = prefs[Keys.AUDIO_SUBFOLDER] ?: "Music/VORTEX",
            maxSimultaneousDownloads = (prefs[Keys.MAX_SIMULTANEOUS] ?: 2).coerceIn(1, 4),
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultVideoQuality(height: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_VIDEO_HEIGHT] = height }
    }

    suspend fun setDefaultAudioBitrate(kbps: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_AUDIO_BITRATE] = kbps }
    }

    suspend fun setMaxSimultaneousDownloads(limit: Int) {
        context.dataStore.edit { it[Keys.MAX_SIMULTANEOUS] = limit.coerceIn(1, 4) }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }
}
