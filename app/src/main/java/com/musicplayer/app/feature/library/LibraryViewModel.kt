package com.musicplayer.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Artist
import com.musicplayer.app.core.model.Folder
import com.musicplayer.app.core.model.Genre
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.repository.LibraryRepository
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    val songs: StateFlow<List<Song>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val genres: StateFlow<List<Genre>> = repository.genres
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(song: Song) {
        val playlist = songs.value
        val index = playlist.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        if (playlist.isNotEmpty()) {
            playbackController.playSongs(playlist, index)
        }
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.refresh()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}