package com.musicplayer.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.recents.RecentStore
import com.musicplayer.app.data.repository.LibraryRepository
import com.musicplayer.app.data.settings.SettingsState
import com.musicplayer.app.data.settings.SettingsStore
import com.musicplayer.app.data.settings.ThemeMode
import com.musicplayer.app.data.usage.UsageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val settings: StateFlow<SettingsState> = SettingsStore.flow(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { SettingsStore.setThemeMode(context, mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { SettingsStore.setDynamicColor(context, enabled) }
    }

    fun refreshLibrary() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                libraryRepository.refresh(force = true)
                _refreshMessage.value = "ok"
            } catch (_: Exception) {
                _refreshMessage.value = "error"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearRecents() {
        viewModelScope.launch { RecentStore.clear(context) }
    }

    fun clearUsage() {
        viewModelScope.launch { UsageStore.clear(context) }
    }

    fun consumeRefreshMessage() {
        _refreshMessage.value = null
    }
}
