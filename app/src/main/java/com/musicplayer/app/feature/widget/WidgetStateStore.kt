package com.musicplayer.app.feature.widget

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore compartido por el estado del widget y la sesión de reproducción. */
internal val Context.playerDataStore by preferencesDataStore(name = "music_widget")

/** Estado que ve el widget; se persiste para sobrevivir a la muerte del proceso. */
data class WidgetState(
    val title: String?,
    val artist: String?,
    val albumArtUri: Uri?,
    /** Id del álbum en MediaStore; permite reconstruir la carátula de forma robusta. */
    val albumId: Long?,
    val isPlaying: Boolean,
) {
    val hasMedia: Boolean get() = title != null
}

/** Persistencia (DataStore) del estado del reproductor para el widget. */
object WidgetStateStore {

    private val KEY_TITLE = stringPreferencesKey("title")
    private val KEY_ARTIST = stringPreferencesKey("artist")
    private val KEY_ALBUM_ART = stringPreferencesKey("album_art")
    private val KEY_ALBUM_ID = longPreferencesKey("album_id")
    private val KEY_IS_PLAYING = booleanPreferencesKey("is_playing")

    suspend fun load(context: Context): WidgetState = flow(context).first()

    /** Flujo del estado persistido; el widget lo observa para recomponerse. */
    fun flow(context: Context): Flow<WidgetState> =
        context.playerDataStore.data.map { prefs ->
            WidgetState(
                title = prefs[KEY_TITLE],
                artist = prefs[KEY_ARTIST],
                albumArtUri = prefs[KEY_ALBUM_ART]?.let(Uri::parse),
                albumId = prefs[KEY_ALBUM_ID],
                isPlaying = prefs[KEY_IS_PLAYING] ?: false,
            )
        }

    suspend fun save(context: Context, state: WidgetState) {
        context.playerDataStore.edit { prefs ->
            if (state.title != null) {
                prefs[KEY_TITLE] = state.title
                prefs[KEY_ARTIST] = state.artist.orEmpty()
                if (state.albumArtUri != null) {
                    prefs[KEY_ALBUM_ART] = state.albumArtUri.toString()
                }
                if (state.albumId != null) {
                    prefs[KEY_ALBUM_ID] = state.albumId
                }
            } else {
                prefs.remove(KEY_TITLE)
                prefs.remove(KEY_ARTIST)
                prefs.remove(KEY_ALBUM_ART)
                prefs.remove(KEY_ALBUM_ID)
            }
            prefs[KEY_IS_PLAYING] = state.isPlaying
        }
    }
}

/** Sesión de reproducción persistida; permite restaurar la cola en frío (proceso muerto). */
data class SessionState(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
)

/** Persistencia de la última sesión (cola de reproducción + posición). */
object SessionStateStore {

    private val KEY_QUEUE_IDS = stringPreferencesKey("queue_ids")
    private val KEY_QUEUE_INDEX = intPreferencesKey("queue_index")
    private val KEY_QUEUE_POSITION = longPreferencesKey("queue_position")

    suspend fun load(context: Context): SessionState? {
        val prefs = context.playerDataStore.data.first()
        val index = prefs[KEY_QUEUE_INDEX] ?: return null
        val ids = prefs[KEY_QUEUE_IDS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (ids.isEmpty()) return null
        return SessionState(ids, index, prefs[KEY_QUEUE_POSITION] ?: 0L)
    }

    suspend fun save(context: Context, session: SessionState) {
        context.playerDataStore.edit { prefs ->
            prefs[KEY_QUEUE_IDS] = session.songIds.joinToString(",")
            prefs[KEY_QUEUE_INDEX] = session.index
            prefs[KEY_QUEUE_POSITION] = session.positionMs
        }
    }
}