package com.musicplayer.app.data.artist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

internal val Context.artistImageDataStore by preferencesDataStore(name = "artist_images")

object ArtistImageStore {
    private val KEY_MAP = stringPreferencesKey("artist_image_map")

    fun flow(context: Context): Flow<Map<Long, String>> =
        context.artistImageDataStore.data.map { prefs ->
            prefs[KEY_MAP]?.let(::decode).orEmpty()
        }

    suspend fun get(context: Context, artistId: Long): String? =
        flow(context).first()[artistId]

    suspend fun put(context: Context, artistId: Long, url: String) {
        context.artistImageDataStore.edit { prefs ->
            val current = prefs[KEY_MAP]?.let(::decode).orEmpty().toMutableMap()
            current[artistId] = url
            prefs[KEY_MAP] = encode(current)
        }
    }

    private fun encode(map: Map<Long, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), v) }
        return obj.toString()
    }

    private fun decode(raw: String): Map<Long, String> {
        val obj = JSONObject(raw)
        val out = mutableMapOf<Long, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.optString(k)
            if (v.isNotBlank()) k.toLongOrNull()?.let { out[it] = v }
        }
        return out
    }
}
