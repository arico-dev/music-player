package com.musicplayer.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Artist
import com.musicplayer.app.core.model.Folder
import com.musicplayer.app.core.model.Genre
import com.musicplayer.app.core.model.Playlist
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.core.model.SongMetadata
import com.musicplayer.app.data.mediastore.MetadataReader
import com.musicplayer.app.data.repository.LibraryRepository
import com.musicplayer.app.data.repository.PlaylistRepository
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController,
    private val metadataReader: MetadataReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

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

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _songInfo = MutableStateFlow<SongMetadata?>(null)
    val songInfo: StateFlow<SongMetadata?> = _songInfo

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
            val hasCachedData = withContext(Dispatchers.IO) { !repository.isEmpty() }
            _uiState.value = _uiState.value.copy(isLoading = !hasCachedData)
            withContext(Dispatchers.IO) { repository.refresh() }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { playlistRepository.create(trimmed) }
    }

    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch { playlistRepository.addSongs(playlistId, listOf(song.id)) }
    }

    fun createPlaylistWithSong(name: String, song: Song) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val playlistId = playlistRepository.create(trimmed)
            playlistRepository.addSongs(playlistId, listOf(song.id))
        }
    }

    fun requestSongInfo(song: Song) {
        viewModelScope.launch {
            _songInfo.value = withContext(Dispatchers.IO) { metadataReader.read(song) }
        }
    }

    fun dismissSongInfo() {
        _songInfo.value = null
    }
}