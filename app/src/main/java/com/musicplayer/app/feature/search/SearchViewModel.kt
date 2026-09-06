package com.musicplayer.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Artist
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.core.model.SongMetadata
import com.musicplayer.app.data.mediastore.MetadataReader
import com.musicplayer.app.data.repository.LibraryRepository
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SearchResults(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val playbackController: PlaybackController,
    private val metadataReader: MetadataReader
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val results: StateFlow<SearchResults> = _query
        .debounce(300)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isEmpty()) flowOf(SearchResults())
            else search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    private fun search(q: String) =
        combine(repository.songs, repository.albums, repository.artists) { songs, albums, artists ->
            val needle = q.lowercase()
            SearchResults(
                query = q,
                songs = songs
                    .filter {
                        it.title.lowercase().contains(needle) ||
                            it.artist.lowercase().contains(needle) ||
                            it.album.lowercase().contains(needle)
                    }
                    .sortedBy { it.title.lowercase() },
                albums = albums
                    .filter {
                        it.title.lowercase().contains(needle) ||
                            it.artist.lowercase().contains(needle)
                    }
                    .sortedBy { it.title.lowercase() },
                artists = artists
                    .filter { it.name.lowercase().contains(needle) }
                    .sortedBy { it.name.lowercase() }
            )
        }

    fun onQueryChange(q: String) {
        _query.value = q
    }

    fun play(song: Song) {
        val playlist = results.value.songs
        val index = playlist.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        if (playlist.isNotEmpty()) {
            playbackController.playSongs(playlist, index)
        }
    }

    private val _songInfo = MutableStateFlow<SongMetadata?>(null)
    val songInfo: StateFlow<SongMetadata?> = _songInfo

    fun requestSongInfo(song: Song) {
        viewModelScope.launch {
            _songInfo.value = withContext(Dispatchers.IO) { metadataReader.read(song) }
        }
    }

    fun dismissSongInfo() {
        _songInfo.value = null
    }
}