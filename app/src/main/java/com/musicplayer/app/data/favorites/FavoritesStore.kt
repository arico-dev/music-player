package com.musicplayer.app.data.favorites

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/** DataStore dedicado a las canciones favoritas (ids de canción marcadas con corazón). */
internal val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

/**
 * Set de canciones favoritas por su id de MediaStore.
 * Se guardan solo ids y la UI resuelve título/artista/portada contra la base de datos,
 * descartando los que ya no existan en la biblioteca.
 */
object FavoritesStore {

    private val KEY_IDS = stringPreferencesKey("song_ids")

    fun flow(context: Context): Flow<List<Long>> =
        context.favoritesDataStore.data.map { prefs ->
            prefs[KEY_IDS]?.let(::decode).orEmpty()
        }

    suspend fun current(context: Context): List<Long> = flow(context).first()

    suspend fun toggle(context: Context, songId: Long) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[KEY_IDS]?.let(::decode).orEmpty()
            prefs[KEY_IDS] = encode(
                if (songId in current) current - songId else current + songId
            )
        }
    }

    suspend fun clear(context: Context) {
        context.favoritesDataStore.edit { prefs -> prefs.remove(KEY_IDS) }
    }

    private fun encode(ids: List<Long>): String {
        val arr = JSONArray()
        ids.forEach(arr::put)
        return arr.toString()
    }

    private fun decode(raw: String): List<Long> {
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val value = arr.optLong(i)
                if (value != 0L) add(value)
            }
        }
    }
}