package com.musicplayer.app.data.lyrics

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

    private val clean: (String) -> String = { raw ->
    raw.replace(Regex("""[^\p{L}\p{N} ]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

suspend fun fetchSong(artist: String, title: String, album: String?): LyricsResult = runCatching {
        val response = api.getLyrics(
            artistName = clean(artist).takeIf { it.isNotEmpty() },
            trackName = clean(title),
            albumName = clean(album ?: "").takeIf { it.isNotEmpty() }
        )
        val record = if (response.isSuccessful) response.body() else null
        record ?: api.search(clean(title), page = 1).pickBest(album)
    }.fold(
        onSuccess = { record -> record.toResult() },
        onFailure = { LyricsResult.NotFound }
    )

    private fun List<LrcLibLyrics>.pickBest(album: String?): LrcLibLyrics? {
        if (isEmpty()) return null
        val albumKey = album?.let { clean(it).lowercase() }
        albumKey?.let { key ->
            firstOrNull { clean(it.albumName ?: "").lowercase() == key }?.let { return it }
        }
        firstOrNull { it.albumName.isNullOrBlank() && !it.syncedLyrics.isNullOrBlank() }?.let { return it }
        firstOrNull { !it.syncedLyrics.isNullOrBlank() }?.let { return it }
        return first()
    }

    private fun LrcLibLyrics?.toResult(): LyricsResult = when {
        this == null -> LyricsResult.NotFound
        instrumental -> LyricsResult.Instrumental
        !syncedLyrics.isNullOrBlank() -> LyricsResult.Synced(LrcParser.parse(syncedLyrics))
        !plainLyrics.isNullOrBlank() -> LyricsResult.Plain(plainLyrics)
        else -> LyricsResult.NotFound
    }
}