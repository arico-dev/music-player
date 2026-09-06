package com.musicplayer.app.data.mediastore

import android.media.MediaMetadataRetriever
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.core.model.SongMetadata
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lee los metadatos de un archivo de audio directamente de su tag
 * (título, artista, álbum, género, códec, bitrate, frecuencia…) mediante
 * [MediaMetadataRetriever]. Siempre fallback a los datos de la biblioteca.
 */
@Singleton
class MetadataReader @Inject constructor() {

    fun read(song: Song): SongMetadata {
        val retriever = MediaMetadataRetriever()
        val file = File(song.path)
        val fallback = SongMetadata(
            title = song.title,
            artist = song.artist,
            album = song.album,
            genre = null,
            trackNumber = song.trackNumber,
            durationMs = song.durationMs,
            mimeType = null,
            codec = null,
            bitrateBps = null,
            sampleRateHz = null,
            fileSizeBytes = file.takeIf { it.exists() }?.length(),
            path = song.path
        )
        return try {
            retriever.setDataSource(song.path)
            fallback.copy(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: song.title,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: song.artist,
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: song.album,
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.substringBefore('/')
                    ?.toIntOrNull() ?: song.trackNumber,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: song.durationMs,
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                codec = retriever.extractMetadata(METADATA_KEY_CODEC),
                bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull(),
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toLongOrNull()
            )
        } catch (_: Exception) {
            fallback
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        /** METADATA_KEY_CODEC está oculto en las SDK recientes; su id es estable (17). */
        private const val METADATA_KEY_CODEC = 17
    }
}