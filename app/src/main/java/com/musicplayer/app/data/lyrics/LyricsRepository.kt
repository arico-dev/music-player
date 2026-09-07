package com.musicplayer.app.data.lyrics

import android.util.Log
import android.util.LruCache
import com.musicplayer.app.core.model.LrcLine
import com.musicplayer.app.core.util.LrcParser
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LyricsResult {
    data object NotFound : LyricsResult
    data object Instrumental : LyricsResult
    data class Synced(val lines: List<LrcLine>) : LyricsResult
    data class Plain(val text: String) : LyricsResult
}

@Singleton
class LyricsRepository @Inject constructor(
    private val api: LrcLibApi
) {

    private val cache = LruCache<String, LyricsResult>(12)

    private val clean: (String) -> String = { raw ->
    raw.replace(Regex("""[^\p{L}\p{N} ]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

suspend fun fetchSong(artist: String, title: String, album: String?): LyricsResult = runCatching {
        val cleanArtist = clean(artist).takeIf { it.isNotEmpty() }
        val cleanTitle = clean(title)
        val cleanAlbum = clean(album ?: "").takeIf { it.isNotEmpty() }

        cacheKey(cleanArtist, cleanTitle, cleanAlbum)?.let { key ->
            cache.get(key)?.let {
                Log.d(TAG, "letras desde caché ($key)")
                return it
            }
        }

        val exact = api.getLyrics(
            artistName = cleanArtist,
            trackName = cleanTitle,
            albumName = cleanAlbum
        ).takeIf { it.isSuccessful }?.body()

        val record = if (exact != null && !exact.syncedLyrics.isNullOrBlank()) {
            exact
        } else {
            api.search(cleanTitle, page = 1).bestMatch(album, artist, exact)
        }
        record
    }.fold(
        onSuccess = { record ->
            val result = record.toResult()
            if (result != LyricsResult.NotFound) {
                cacheKey(clean(artist), clean(title), clean(album ?: "").takeIf { it.isNotEmpty() })
                    ?.let { key -> cache.put(key, result) }
            }
            result
        },
        onFailure = { LyricsResult.NotFound }
    )

    private fun cacheKey(artist: String?, title: String?, album: String?): String? {
        if (title.isNullOrBlank()) return null
        return listOf(artist.orEmpty(), title, album.orEmpty())
            .joinToString("|").lowercase()
    }

    private fun List<LrcLibLyrics>.bestMatch(
        album: String?,
        artist: String?,
        fallback: LrcLibLyrics?
    ): LrcLibLyrics? {
        if (isEmpty()) return fallback
        val albumKey = album?.let { clean(it).lowercase() }
        val artistKey = artist?.let { clean(it).lowercase() }?.takeIf { it.isNotEmpty() }
        val sameArtist: (LrcLibLyrics) -> Boolean = { rec ->
            artistKey == null || clean(rec.artistName ?: "").lowercase().contains(artistKey)
        }
        val synced: (LrcLibLyrics) -> Boolean = { !it.syncedLyrics.isNullOrBlank() }

        albumKey?.let { key ->
            firstOrNull { clean(it.albumName ?: "").lowercase() == key && sameArtist(it) && synced(it) }?.let { return it }
            firstOrNull { clean(it.albumName ?: "").lowercase() == key && synced(it) }?.let { return it }
            firstOrNull { clean(it.albumName ?: "").lowercase() == key && sameArtist(it) }?.let { return it }
            firstOrNull { clean(it.albumName ?: "").lowercase() == key }?.let { return it }
        }
        firstOrNull { sameArtist(it) && synced(it) }?.let { return it }
        return fallback
    }

    private fun LrcLibLyrics?.toResult(): LyricsResult = when {
        this == null -> LyricsResult.NotFound
        instrumental -> LyricsResult.Instrumental
        !syncedLyrics.isNullOrBlank() -> LyricsResult.Synced(LrcParser.parse(syncedLyrics))
        !plainLyrics.isNullOrBlank() -> LyricsResult.Plain(plainLyrics)
        else -> LyricsResult.NotFound
    }

    private companion object {
        const val TAG = "LyricsCache"
    }
}
