package com.musicplayer.app.data.usage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** DataStore dedicado a la telemetría de uso (tiempo escuchado + conteos). */
internal val Context.usageDataStore by preferencesDataStore(name = "usage")

private const val DATE_KEY = "yyyy-MM-dd"
private const val MAX_DAYS = 70

private fun usageKey(dt: LocalDateTime): String =
    dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"))

private fun dateKey(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern(DATE_KEY))

private fun dateKeyOf(bucketKey: String): String = bucketKey.substring(0, DATE_KEY.length)

/**
 * Snapshot del uso acumulado. Los buckets se indexan por hora (clave "yyyy-MM-dd-HH"),
 * lo que permite derivar "última hora", "hoy" y "este mes" sin guardar cada evento.
 */
data class UsageData(
    val totalPlayedMs: Long = 0L,
    /** Canción -> nº de reproducciones iniciadas. */
    val plays: Map<Long, Int> = emptyMap(),
    val buckets: Map<String, Long> = emptyMap()
) {
    val distinctSongs: Int get() = plays.size
    val totalPlays: Int get() = plays.values.sum()

    fun lastHourMs(now: LocalDateTime): Long = buckets[usageKey(now)] ?: 0L

    fun todayMs(now: LocalDateTime): Long =
        buckets.entries.filter { it.key.startsWith(dateKey(now.toLocalDate())) }.sumOf { it.value }

    fun monthMs(now: LocalDateTime): Long {
        val monthPrefix = now.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return buckets.entries.filter { it.key.startsWith(monthPrefix) }.sumOf { it.value }
    }
}

object UsageStore {

    private val KEY_DATA = stringPreferencesKey("usage")

    fun flow(context: Context): Flow<UsageData> =
        context.usageDataStore.data.map { prefs ->
            prefs[KEY_DATA]?.let(::decode) ?: UsageData()
        }

    suspend fun current(context: Context): UsageData = flow(context).first()

    /** Cuenta una reproducción iniciada de [songId]. */
    suspend fun recordPlay(context: Context, songId: Long) {
        context.usageDataStore.edit { prefs ->
            val current = prefs[KEY_DATA]?.let(::decode) ?: UsageData()
            val updated = current.copy(
                plays = current.plays.toMutableMap().apply {
                    put(songId, (get(songId) ?: 0) + 1)
                }
            )
            prefs[KEY_DATA] = encode(updated)
        }
    }

    /** Acumula [ms] escuchados en la hora actual, recortando buckets antiguos. */
    suspend fun recordListening(context: Context, ms: Long) {
        if (ms <= 0) return
        context.usageDataStore.edit { prefs ->
            val current = prefs[KEY_DATA]?.let(::decode) ?: UsageData()
            val now = LocalDateTime.now()
            val key = usageKey(now)
            val updated = current.copy(
                totalPlayedMs = current.totalPlayedMs + ms,
                buckets = (current.buckets.toMutableMap().apply {
                    put(key, (get(key) ?: 0L) + ms)
                }).filterKeys { dateKeyOf(it) >= dateKey(now.toLocalDate().minusDays(MAX_DAYS.toLong())) }
            )
            prefs[KEY_DATA] = encode(updated)
        }
    }

    private fun encode(data: UsageData): String {
        val root = JSONObject()
        root.put("total", data.totalPlayedMs)
        val plays = JSONObject()
        data.plays.forEach { (id, count) -> plays.put(id.toString(), count) }
        root.put("plays", plays)
        val buckets = JSONObject()
        data.buckets.forEach { (key, ms) -> buckets.put(key, ms) }
        root.put("buckets", buckets)
        return root.toString()
    }

    private fun decode(raw: String): UsageData {
        val root = JSONObject(raw)
        val playsRoot = root.optJSONObject("plays")
        val plays = buildMap {
            playsRoot?.let { p ->
                val keys = p.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k.toLong(), p.optInt(k))
                }
            }
        }
        val bucketsRoot = root.optJSONObject("buckets")
        val buckets = buildMap {
            bucketsRoot?.let { b ->
                val keys = b.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, b.optLong(k))
                }
            }
        }
        return UsageData(
            totalPlayedMs = root.optLong("total"),
            plays = plays,
            buckets = buckets
        )
    }
}