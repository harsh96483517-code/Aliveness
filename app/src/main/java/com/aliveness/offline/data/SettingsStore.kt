package com.aliveness.offline.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aliveness_settings")

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val autoplay: Boolean = true,
    val autoNext: Boolean = true,
    val defaultSpeed: Float = 1f,
    val gestures: Boolean = true,
    /** 0 = low, 1 = balanced, 2 = high. Controls decoded thumbnail size. */
    val thumbQuality: Int = 1,
    val reelsCrop: Boolean = true,
    val resumePlayback: Boolean = true,
    val dockReels: Boolean = true,
    val liquidDock: Boolean = true,
    val dockHaptics: Boolean = true,
    val hiddenFolders: Set<String> = emptySet(),
    val lastScanAt: Long = 0L,
) {
    val thumbPx: Int get() = when (thumbQuality) { 0 -> 240; 2 -> 720; else -> 420 }
}

object Keys {
    val theme = intPreferencesKey("theme")
    val dynamic = booleanPreferencesKey("dynamic")
    val autoplay = booleanPreferencesKey("autoplay")
    val autoNext = booleanPreferencesKey("auto_next")
    val speed = floatPreferencesKey("speed")
    val gestures = booleanPreferencesKey("gestures")
    val thumbQuality = intPreferencesKey("thumb_quality")
    val reelsCrop = booleanPreferencesKey("reels_crop")
    val resume = booleanPreferencesKey("resume")
    val dockReels = booleanPreferencesKey("dock_reels")
    val liquidDock = booleanPreferencesKey("liquid_dock")
    val dockHaptics = booleanPreferencesKey("dock_haptics")
    val hidden = stringSetPreferencesKey("hidden_folders")
    val lastScan = longPreferencesKey("last_scan")
}

class SettingsStore(private val context: Context) {

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = ThemeMode.values().getOrElse(p[Keys.theme] ?: 0) { ThemeMode.System },
            dynamicColor = p[Keys.dynamic] ?: false,
            autoplay = p[Keys.autoplay] ?: true,
            autoNext = p[Keys.autoNext] ?: true,
            defaultSpeed = p[Keys.speed] ?: 1f,
            gestures = p[Keys.gestures] ?: true,
            thumbQuality = p[Keys.thumbQuality] ?: 1,
            reelsCrop = p[Keys.reelsCrop] ?: true,
            resumePlayback = p[Keys.resume] ?: true,
            dockReels = p[Keys.dockReels] ?: true,
            liquidDock = p[Keys.liquidDock] ?: true,
            dockHaptics = p[Keys.dockHaptics] ?: true,
            hiddenFolders = p[Keys.hidden] ?: emptySet(),
            lastScanAt = p[Keys.lastScan] ?: 0L,
        )
    }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setHidden(path: String, hidden: Boolean) {
        context.dataStore.edit { p ->
            val cur = p[Keys.hidden] ?: emptySet()
            p[Keys.hidden] = if (hidden) cur + path else cur - path
        }
    }
}
