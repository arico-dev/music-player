package com.musicplayer.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.recents.RecentStore
import com.musicplayer.app.data.usage.UsageData
import com.musicplayer.app.data.usage.UsageStore
import com.musicplayer.app.feature.widget.SessionState
import com.musicplayer.app.feature.widget.SessionStateStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Última sesión guardada junto con la canción a la que apunta, para la tarjeta "Seguir escuchando". */
data class ResumeEntry(
    val session: SessionState,
    val song: Song
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao
) : ViewModel() {

    /** Snapshot de uso (tiempo total, por hora, conteos). */
    val usage: StateFlow<UsageData> = UsageStore.flow(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsageData())

    /** Álbumes más escuchados (top 8 por suma de reproducciones de sus canciones). */
    val topAlbums: StateFlow<List<Album>> = usage
        .flatMapLatest { data ->
            if (data.plays.isEmpty()) {
                flowOf(emptyList())
            } else {
                flow { emit(computeTopAlbums(data)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Álbumes más escuchados (top 8 por suma de reproducciones de sus canciones). */
    private suspend fun computeTopAlbums(data: UsageData): List<Album> {
        if (data.plays.isEmpty()) return emptyList()
        val songs = songDao.getByIds(data.plays.keys.toList())
        val scoreByAlbum = HashMap<Long, Int>()
        songs.forEach { song ->
            val albumId = song.albumId
            if (albumId > 0) {
                scoreByAlbum[albumId] = (scoreByAlbum[albumId] ?: 0) + (data.plays[song.id] ?: 0)
            }
        }
        val rankedIds = scoreByAlbum.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }
        if (rankedIds.isEmpty()) return emptyList()
        val byId = albumDao.getByIds(rankedIds).associateBy { it.id }
        return rankedIds.mapNotNull { byId[it]?.toAlbum() }
    }

    /** Historial de canción, resolviendo ids contra la biblioteca y descartando las borradas. */
    val recentSongs: StateFlow<List<Song>> = RecentStore.flow(context)
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else flow { emit(songDao.getByIds(ids)) }.map { entities ->
                val byId = entities.associateBy { it.id }
                ids.mapNotNull { byId[it] }.map { it.toSong() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _resumeEntry = MutableStateFlow<ResumeEntry?>(null)
    val resumeEntry: StateFlow<ResumeEntry?> = _resumeEntry

    init {
        refresh()
    }

    /** Recarga la sesión guardada (se llama al entrar en Inicio). */
    fun refresh() {
        viewModelScope.launch {
            val session = SessionStateStore.load(context) ?: run {
                _resumeEntry.value = null
                return@launch
            }
            val targetId = session.songIds.getOrNull(session.index)
            val song = songDao.getByIds(session.songIds).firstOrNull { it.id == targetId }?.toSong()
            _resumeEntry.value = song?.let { ResumeEntry(session, it) }
        }
    }

    private fun SongEntity.toSong() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        path = path,
        albumArtUri = albumId.takeIf { it > 0 }?.let {
            Uri.parse("content://media/external/audio/albumart/$it")
        },
        trackNumber = trackNumber,
        albumId = albumId,
    )

    private fun com.musicplayer.app.data.local.entity.AlbumEntity.toAlbum() = Album(
        id = id,
        title = title,
        artist = artist,
        albumArtUri = Uri.parse("content://media/external/audio/albumart/$id"),
        year = year,
    )
}