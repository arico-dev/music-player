package com.musicplayer.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class PaletteStyle { TONAL, EXPRESSIVE, FRUITSALAD, MONSOON }

const val DEFAULT_SEED_ARGB: Long = 0xFFC2185B

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColorArgb: Long = DEFAULT_SEED_ARGB,
    val paletteStyle: PaletteStyle = PaletteStyle.EXPRESSIVE,
    val keepPlayingInBackground: Boolean = false
)

internal val Context.settingsDataStore by preferencesDataStore(name = "settings")

object SettingsStore {

    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_SEED = longPreferencesKey("seed_color")
    private val KEY_PALETTE_STYLE = stringPreferencesKey("palette_style")
    private val KEY_KEEP_PLAYING = booleanPreferencesKey("keep_playing_background")

    fun flow(context: Context): Flow<SettingsState> =
        context.settingsDataStore.data.map { prefs ->
            val rawTheme = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
            val mode = runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.SYSTEM)
            val seed = prefs[KEY_SEED] ?: DEFAULT_SEED_ARGB
            val rawStyle = prefs[KEY_PALETTE_STYLE] ?: PaletteStyle.EXPRESSIVE.name
            val style = runCatching { PaletteStyle.valueOf(rawStyle) }.getOrDefault(PaletteStyle.EXPRESSIVE)
            val keepPlaying = prefs[KEY_KEEP_PLAYING] ?: false
            SettingsState(
                themeMode = mode,
                seedColorArgb = seed,
                paletteStyle = style,
                keepPlayingInBackground = keepPlaying
            )
        }

    suspend fun current(context: Context): SettingsState = flow(context).first()

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }

    suspend fun setSeedColor(context: Context, argb: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_SEED] = argb
        }
    }

    suspend fun setPaletteStyle(context: Context, style: PaletteStyle) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_PALETTE_STYLE] = style.name
        }
    }

    suspend fun setKeepPlayingInBackground(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_KEEP_PLAYING] = enabled
        }
    }
}
