package com.musicplayer.app.data.repository

import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Artist
import com.musicplayer.app.core.model.Folder
import com.musicplayer.app.core.model.Genre
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.GenreDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.dao.SongGenreDao
import com.musicplayer.app.data.local.entity.AlbumEntity
import com.musicplayer.app.data.local.entity.ArtistEntity
import com.musicplayer.app.data.local.entity.GenreEntity
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.local.entity.SongGenreEntity
import com.musicplayer.app.data.mediastore.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val songGenreDao: SongGenreDao
) {

    val songs: Flow<List<Song>> = songDao.observeAll().map { list ->
        list.map { it.toSong() }
    }

    val albums: Flow<List<Album>> = albumDao.observeAll().map { list ->
        list.map { it.toAlbum() }
    }

    val artists: Flow<List<Artist>> = artistDao.observeAllWithCounts().map { list ->
        list.map { Artist(id = it.id, name = it.name, albumCount = it.albumCount, trackCount = it.trackCount) }
    }

    val genres: Flow<List<Genre>> = genreDao.observeAllWithCounts().map { list ->
        list.map { Genre(id = it.id, name = it.name, trackCount = it.trackCount) }
    }

    val folders: Flow<List<Folder>> = songs.map { list ->
        list.groupBy { it.folderPath }
            .map { (path, songs) ->
                Folder(
                    path = path,
                    name = path.substringAfterLast('/').ifEmpty { path },
                    songCount = songs.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun album(id: Long): Flow<Album?> = albumDao.observeById(id).map { it?.toAlbum() }
    fun artist(id: Long): Flow<Artist?> = artistDao.observeByIdWithCounts(id).map {
        it?.let { artist -> Artist(id = artist.id, name = artist.name, albumCount = artist.albumCount, trackCount = artist.trackCount) }
    }
    fun genre(id: Long): Flow<Genre?> = genreDao.observeById(id).map {
        it?.let { genre -> Genre(id = genre.id, name = genre.name, trackCount = 0) }
    }

    fun songsByAlbum(albumId: Long): Flow<List<Song>> =
        songDao.observeByAlbum(albumId).map { list -> list.map { it.toSong() } }

    fun songsByArtist(artistId: Long): Flow<List<Song>> =
        songDao.observeByArtist(artistId).map { list -> list.map { it.toSong() } }

    fun songsByGenre(genreId: Long): Flow<List<Song>> =
        songGenreDao.observeSongsByGenre(genreId).map { list -> list.map { it.toSong() } }

    fun songsByFolder(path: String): Flow<List<Song>> =
        songs.map { list ->
            list.filter { it.folderPath == path }.sortedBy { it.title.lowercase() }
        }

    suspend fun refresh(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && mediaStoreScanner.shouldSkipRescan()) {
            return@withContext
        }

        val songScans = mediaStoreScanner.scanSongs()
        val songs = songScans.map { it.song }
        val albums = mediaStoreScanner.scanAlbums()
        val genreScans = mediaStoreScanner.scanGenres()

        val genreEntities = genreScans.map { GenreEntity(id = it.id, name = it.name) }

        val artists = songs
            .groupBy { it.artist }
            .map { (name, group) ->
                ArtistEntity(id = group.first().artist.hashCode().toLong(), name = name)
            }

        val albumArtistByAlbumId = songScans
            .mapNotNull { scan ->
                scan.song.albumId?.takeIf { it > 0 }?.let { id -> id to scan.albumArtist }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.firstOrNull { !it.isNullOrBlank() } }

        val albumsWithTags = albums.map { album ->
            albumArtistByAlbumId[album.id]
                ?.let { artist -> album.copy(artist = artist) }
                ?: album
        }

        songDao.upsertAll(songs.map { it.toEntity() })
        albumDao.upsertAll(albumsWithTags.map { it.toAlbumEntity() })
        artistDao.upsertAll(artists)
        genreDao.upsertAll(genreEntities)

        songGenreDao.clearAll()
        songGenreDao.upsertAll(
            genreScans.flatMap { genre ->
                genre.songIds.map { SongGenreEntity(songId = it, genreId = genre.id) }
            }
        )

        if (genreEntities.isNotEmpty()) {
            genreDao.deleteNotIn(genreEntities.map { it.id })
        }

        // Remove artists that no longer have songs on device
        if (songs.isNotEmpty()) {
            val artistIds = songs.map { it.artist.hashCode().toLong() }.distinct()
            artistDao.deleteNotIn(artistIds)
        }

        // Remove albums that no longer have songs on device
        val existingAlbumIds = songs.mapNotNull { it.albumId }
        if (existingAlbumIds.isNotEmpty()) {
            albumDao.deleteNotIn(existingAlbumIds)
        } else {
            albumDao.clear()
        }

        // Remove songs that no longer exist on device
        val existingIds = songs.map { it.id }
        if (existingIds.isNotEmpty()) {
            songDao.deleteNotIn(existingIds)
        }

        mediaStoreScanner.markScanned()
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