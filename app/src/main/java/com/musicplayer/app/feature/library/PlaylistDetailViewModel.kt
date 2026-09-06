package com.musicplayer.app.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Playlist
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.repository.LibraryRepository
import com.musicplayer.app.data.repository.PlaylistRepository
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    libraryRepository: LibraryRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val playlistId: Long = savedStateHandle["playlistId"] ?: -1L

    val songs: StateFlow<List<Song>> = playlistRepository.songsIn(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSongs: StateFlow<List<Song>> = libraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val name: StateFlow<String> = playlistRepository.playlist(playlistId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun rename(name: String) = viewModelScope.launch {
        playlistRepository.rename(playlistId, name)
    }

    fun delete() = viewModelScope.launch {
        playlistRepository.delete(playlistId)
    }

    fun removeSong(songId: Long) = viewModelScope.launch {
        playlistRepository.removeSong(playlistId, songId)
    }

    fun reorder(updated: List<Song>) = viewModelScope.launch {
        playlistRepository.reorder(playlistId, updated.map { it.id })
    }

    fun move(song: Song, delta: Int) {
        val current = songs.value.toMutableList()
        val from = current.indexOfFirst { it.id == song.id }
        val to = from + delta
        if (from == -1 || to !in current.indices) return
        val moved = current.removeAt(from)
        current.add(to, moved)
        reorder(current)
    }

    fun addSongs(selected: List<Song>) = viewModelScope.launch {
        playlistRepository.addSongs(playlistId, selected.map { it.id })
    }

    fun play(song: Song) {
        val index = songs.value.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        playFrom(index)
    }

    fun playFrom(index: Int = 0) {
        val playlist = songs.value
        if (playlist.isNotEmpty()) {
            val start = index.coerceIn(playlist.indices)
            playbackController.playSongs(playlist, start)
        }
    }
}