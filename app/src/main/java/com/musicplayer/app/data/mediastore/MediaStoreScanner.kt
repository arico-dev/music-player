package com.musicplayer.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.musicplayer.app.core.model.Album
import com.musicplayer.app.core.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val contentResolver = context.contentResolver
    private val tagReader = TagReader()

    private val prefs = context.getSharedPreferences("library_scan", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_FINGERPRINT = "media_fingerprint"
    }

    /** Huella ligera de la biblioteca (id y fecha de modificación por canción). */
    fun fingerprint(): String {
        val md = MessageDigest.getInstance("MD5")
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATE_MODIFIED),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val modifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                if (idCol >= 0) md.update(cursor.getLong(idCol).toString().toByteArray())
                md.update(':'.code.toByte())
                if (modifiedCol >= 0) md.update(cursor.getLong(modifiedCol).toString().toByteArray())
                md.update(';'.code.toByte())
            }
        }
        return md.digest().toHex()
    }

    /** Devuelve true si la biblioteca de MediaStore no cambió desde el último escaneo. */
    fun shouldSkipRescan(): Boolean {
        val stored = prefs.getString(KEY_FINGERPRINT, null) ?: return false
        return stored == fingerprint()
    }

    /** Marca el fingerprint actual como escaneado. */
    fun markScanned() {
        prefs.edit().putString(KEY_FINGERPRINT, fingerprint()).apply()
    }

    fun scanSongs(): List<SongScan> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = mutableListOf<SongScan>()
        contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val path = cursor.getString(dataCol) ?: ""
                val tags = tagReader.read(path)
                val mediaArtist = cursor.getString(artistCol) ?: "Unknown Artist"
                val mediaAlbum = cursor.getString(albumCol) ?: "Unknown Album"
                val mediaTrack = cursor.getInt(trackCol)
                songs += SongScan(
                    song = Song(
                        id = id,
                        title = tags.title?.takeIf { it.isNotBlank() }
                            ?: cursor.getString(titleCol) ?: "Unknown",
                        artist = tags.primaryArtist ?: mediaArtist.cleanArtist(),
                        album = tags.album?.takeIf { it.isNotBlank() } ?: mediaAlbum,
                        durationMs = cursor.getLong(durationCol),
                        path = path,
                        albumArtUri = albumArtUri(albumId),
                        trackNumber = tags.trackNumber ?: mediaTrack.takeIf { it > 0 },
                        albumId = albumId.takeIf { it > 0 }
                    ),
                    albumArtist = tags.albumArtists.primary()
                )
            }
        }
        return songs
    }

    fun scanAlbums(): List<Album> {
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        val albums = mutableListOf<Album>()
        contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Albums.ALBUM} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                albums += Album(
                    id = id,
                    title = cursor.getString(albumCol) ?: "Unknown Album",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    albumArtUri = albumArtUri(id),
                    year = cursor.getInt(yearCol).takeIf { it > 0 }
                )
            }
        }
        return albums
    }

    fun scanGenres(): List<GenreScan> {
        val namesById = mutableMapOf<Long, String>()
        contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null,
            null,
            "${MediaStore.Audio.Genres.NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)?.trim() ?: "Unknown Genre"
                namesById[id] = name
            }
        }

        val songsByGenreName = sortedMapOf<String, MutableList<Long>>(String.CASE_INSENSITIVE_ORDER)
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.GENRE),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val genreCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            while (cursor.moveToNext()) {
                val name = cursor.getString(genreCol)?.trim() ?: continue
                songsByGenreName.getOrPut(name) { mutableListOf() } += cursor.getLong(idCol)
            }
        }

        return namesById.map { (id, name) ->
            GenreScan(id = id, name = name, songIds = songsByGenreName[name].orEmpty())
        }
    }

    private fun albumArtUri(albumId: Long): Uri? =
        if (albumId > 0) {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
        } else {
            null
        }
}

data class GenreScan(
    val id: Long,
    val name: String,
    val songIds: List<Long>
)

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

data class SongScan(
    val song: Song,
    val albumArtist: String?
)

private val SCANNER_FEAT_REGEX = Regex("\\s+(ft\\.?|feat\\.?|featuring)\\b", RegexOption.IGNORE_CASE)

private fun String.cleanArtist(): String {
    var result = SCANNER_FEAT_REGEX.split(this).first().trim()
    for (sep in charArrayOf(';', ',', '/', '&', '+')) {
        val idx = result.indexOf(sep)
        if (idx > 0) result = result.substring(0, idx)
    }
    val lower = result.lowercase()
    for (kw in listOf(" and ", " with ", " vs ", " vs. ", " x ")) {
        val idx = lower.indexOf(kw)
        if (idx > 0) {
            result = result.substring(0, idx).trim()
            break
        }
    }
    return result.trim().takeIf { it.isNotBlank() } ?: this.trim()
}
