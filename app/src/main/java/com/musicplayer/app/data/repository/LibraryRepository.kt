package com.musicplayer.app.data.repository

import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.AlbumEntity
import com.musicplayer.app.data.local.entity.ArtistEntity
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.mediastore.MediaStoreScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: com.musicplayer.app.data.local.dao.ArtistDao
) {

    val songs: Flow<List<Song>> = songDao.observeAll().map { list ->
        list.map { it.toSong() }
    }

    val albums: Flow<List<Album>> = albumDao.observeAll().map { list ->
        list.map { it.toAlbum() }
    }

    suspend fun refresh() {
        val songs = mediaStoreScanner.scanSongs()
        val albums = mediaStoreScanner.scanAlbums()

        // Collect unique artists
        val artists = songs
            .groupBy { it.artist }
            .map { (name, group) ->
                ArtistEntity(id = group.first().artist.hashCode().toLong(), name = name)
            }

        songDao.upsertAll(songs.map { it.toEntity() })
        albumDao.upsertAll(albums.map { it.toAlbumEntity() })
        artistDao.upsertAll(artists)

        // Remove songs that no longer exist on device
        val existingIds = songs.map { it.id }
        if (existingIds.isNotEmpty()) {
            songDao.deleteNotIn(existingIds)
        }
    }

    suspend fun isEmpty(): Boolean = songDao.count() == 0

    private fun Song.toEntity() = SongEntity(
        id = id,
        title = title,
        artist = artist,
        artistId = artist.hashCode().toLong(),
        album = album,
        albumId = albumId ?: 0L,
        durationMs = durationMs,
        path = path,
        trackNumber = trackNumber,
        dateAdded = System.currentTimeMillis()
    )

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

    private fun AlbumEntity.toAlbum() = Album(
        id = id,
        title = title,
        artist = artist,
        albumArtUri = android.net.Uri.parse("content://media/external/audio/albumart/$id"),
        year = year
    )

    private fun Album.toAlbumEntity() = AlbumEntity(
        id = id,
        title = title,
        artist = artist,
        artistId = artist.hashCode().toLong(),
        year = year
    )
}
