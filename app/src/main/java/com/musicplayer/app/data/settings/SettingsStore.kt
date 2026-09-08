package com.musicplayer.app.data.settings

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false
)

internal val Context.settingsDataStore by preferencesDataStore(name = "settings")

object SettingsStore {

    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")

    fun flow(context: Context): Flow<SettingsState> =
        context.settingsDataStore.data.map { prefs ->
            val rawTheme = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
            val mode = runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.SYSTEM)
            val dynamic = prefs[KEY_DYNAMIC] ?: false
            SettingsState(themeMode = mode, dynamicColor = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        }

    suspend fun current(context: Context): SettingsState = flow(context).first()

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }

    suspend fun setDynamicColor(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DYNAMIC] = enabled
        }
    }
}
