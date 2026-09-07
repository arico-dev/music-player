package com.musicplayer.app.data.recents

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/** DataStore dedicado al historial de "Escuchados recientemente". */
internal val Context.recentsDataStore by preferencesDataStore(name = "recents")

/**
 * Historial de reproducción (ids de canción, más reciente primero, sin duplicados).
 * Se guardan solo ids (van al frente de la lista) y la UI resuelve título/artista/portada
 * contra la base de datos, descartando los que ya no existan en la biblioteca.
 */
object RecentStore {

    private const val MAX = 20
    private val KEY_IDS = stringPreferencesKey("song_ids")

    fun flow(context: Context): Flow<List<Long>> =
        context.recentsDataStore.data.map { prefs ->
            prefs[KEY_IDS]?.let(::decode).orEmpty()
        }

    suspend fun current(context: Context): List<Long> = flow(context).first()

    suspend fun addOrMoveTop(context: Context, songId: Long) {
        context.recentsDataStore.edit { prefs ->
            val current = prefs[KEY_IDS]?.let(::decode).orEmpty()
            val updated = (listOf(songId) + current.filterNot { it == songId }).take(MAX)
            prefs[KEY_IDS] = encode(updated)
        }
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