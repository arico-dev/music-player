package com.musicplayer.app.data.repository

import com.musicplayer.app.core.model.Playlist
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.PlaylistSongDao
import com.musicplayer.app.data.local.entity.PlaylistEntity
import com.musicplayer.app.data.local.entity.PlaylistSongEntity
import com.musicplayer.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistSongDao: PlaylistSongDao
) {

    val playlists: Flow<List<Playlist>> = playlistDao.observeAllWithCounts().map { list ->
        list.map { Playlist(id = it.id, name = it.name, trackCount = it.trackCount) }
    }

    fun playlist(id: Long): Flow<Playlist?> = playlistDao.observeById(id).map {
        it?.let { entity -> Playlist(id = entity.id, name = entity.name, trackCount = 0) }
    }

    fun songsIn(playlistId: Long): Flow<List<Song>> =
        playlistSongDao.observeSongsByPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }

    suspend fun create(name: String): Long {
        val playlist = PlaylistEntity(name = name, createdAt = System.currentTimeMillis())
        return playlistDao.insert(playlist)
    }

    suspend fun rename(id: Long, name: String) {
        val current = playlistDao.getById(id) ?: return
        playlistDao.update(current.copy(name = name))
    }

    suspend fun delete(id: Long) {
        playlistSongDao.clearByPlaylist(id)
        playlistDao.getById(id)?.let { playlistDao.delete(it) }
    }

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        var position = playlistSongDao.maxPosition(playlistId)
        val relations = songIds.map { songId ->
            PlaylistSongEntity(playlistId = playlistId, songId = songId, position = ++position)
        }
        playlistSongDao.insertAll(relations)
    }

    suspend fun removeSong(playlistId: Long, songId: Long) {
        playlistSongDao.delete(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = 0))
    }

    suspend fun reorder(playlistId: Long, songIdsInOrder: List<Long>) {
        val relations = songIdsInOrder.mapIndexed { index, songId ->
            PlaylistSongEntity(playlistId = playlistId, songId = songId, position = index)
        }
        playlistSongDao.replaceAll(playlistId, relations)
    }

    private fun SongEntity.toSong() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        path = path,
        albumArtUri = albumId.takeIf { it > 0 }?.let {
            android.net.Uri.parse("content://media/external/audio/albumart/$it")
        },
        trackNumber = trackNumber,
        albumId = albumId
    )
}