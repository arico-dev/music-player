package com.musicplayer.app.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.recents.RecentStore
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
    private val songDao: SongDao
) : ViewModel() {

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
}