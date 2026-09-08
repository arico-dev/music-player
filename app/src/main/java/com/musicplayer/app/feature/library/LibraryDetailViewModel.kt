package com.musicplayer.app.feature.library

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.repository.LibraryRepository
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DetailHeader(
    val name: String,
    val subtitle: String,
    val artworkUri: Uri?
)

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: LibraryRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val albumId: Long? = savedStateHandle["albumId"]
    private val artistId: Long? = savedStateHandle["artistId"]
    private val genreId: Long? = savedStateHandle["genreId"]
    private val folderPath: String? = savedStateHandle["folderPath"]

    val isArtist: Boolean = artistId != null

    val songs: StateFlow<List<Song>> = when {
        albumId != null -> repository.songsByAlbum(albumId)
        artistId != null -> repository.songsByArtist(artistId)
        genreId != null -> repository.songsByGenre(genreId)
        folderPath != null -> repository.songsByFolder(folderPath)
        else -> MutableStateFlow(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artistAlbums: StateFlow<List<com.musicplayer.app.core.model.Album>> = when {
        artistId != null -> repository.albumsByArtist(artistId)
        else -> MutableStateFlow(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val header: StateFlow<DetailHeader?> = when {
        albumId != null -> combine(repository.album(albumId), songs) { album, _ ->
            album?.let {
                DetailHeader(
                    name = it.title,
                    subtitle = listOfNotNull(it.artist, it.year?.toString()).joinToString(" · "),
                    artworkUri = it.albumArtUri
                )
            }
        }
        artistId != null -> combine(repository.artist(artistId), songs) { artist, _ ->
            artist?.let {
                DetailHeader(
                    name = it.name,
                    subtitle = "${it.albumCount} álbumes · ${it.trackCount} canciones",
                    artworkUri = null
                )
            }
        }
        genreId != null -> combine(repository.genre(genreId), songs) { genre, list ->
            genre?.let {
                DetailHeader(
                    name = it.name,
                    subtitle = "${list.size} canciones",
                    artworkUri = null
                )
            }
        }
        folderPath != null -> songs.map { list ->
            DetailHeader(
                name = folderPath.substringAfterLast('/').ifEmpty { folderPath },
                subtitle = "${list.size} canciones",
                artworkUri = null
            )
        }
        else -> MutableStateFlow(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun play(song: Song) {
        val index = songs.value.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        playFrom(index)
    }

    fun playFrom(index: Int = 0) {
        val playlist = songs.value
        if (playlist.isNotEmpty() && index in playlist.indices) {
            playbackController.playSongs(playlist, index)
        } else if (playlist.isNotEmpty()) {
            playbackController.playSongs(playlist, 0)
        }
    }
}