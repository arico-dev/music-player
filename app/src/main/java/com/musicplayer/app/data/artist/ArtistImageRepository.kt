package com.musicplayer.app.data.artist

import android.content.Context
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DeezerArtistApi
) {
    private val memCache = LruCache<Long, String>(30)

    suspend fun imageUrl(artistId: Long, artistName: String): String? {
        memCache.get(artistId)?.let { return it }
        ArtistImageStore.get(context, artistId)?.let {
            memCache.put(artistId, it)
            return it
        }
        val fetched = fetchFromNetwork(artistName) ?: return null
        ArtistImageStore.put(context, artistId, fetched)
        memCache.put(artistId, fetched)
        return fetched
    }

    private suspend fun fetchFromNetwork(artistName: String): String? {
        return try {
            val clean = artistName.trim()
            if (clean.isEmpty()) return null
            android.util.Log.d("ArtistImage", "fetching $clean")
            val response = api.searchArtist(clean)
            android.util.Log.d("ArtistImage", "found ${response.data.size} for $clean")
            val best = response.data.firstOrNull { it.name.equals(clean, ignoreCase = true) }
                ?: response.data.firstOrNull()
                ?: run {
                    android.util.Log.d("ArtistImage", "no match for $clean")
                    return null
                }
            val url = best.picture_medium?.takeIf { it.isNotBlank() }
                ?: best.picture_big
                ?: best.picture
            android.util.Log.d("ArtistImage", "url for $clean -> $url")
            url
        } catch (e: Exception) {
            android.util.Log.e("ArtistImage", "fetch failed $artistName", e)
            null
        }
    }
}
