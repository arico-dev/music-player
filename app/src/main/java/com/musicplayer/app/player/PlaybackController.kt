package com.musicplayer.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Player
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.recents.RecentStore
import com.musicplayer.app.data.usage.UsageStore
import com.musicplayer.app.feature.widget.MusicWidget
import com.musicplayer.app.feature.widget.SessionState
import com.musicplayer.app.feature.widget.SessionStateStore
import com.musicplayer.app.feature.widget.WidgetState
import com.musicplayer.app.feature.widget.WidgetStateStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada de control de reproducción. El reproductor real (ExoPlayer) vive en
 * [MediaPlaybackService] (servicio en primer plano) y se expone como singleton del
 * proceso, así que este controlador lo usa directamente sin binding asíncrono.
 *
 * Al estar el proceso vivo por el servicio, el widget funciona en frío y Glance
 * refresca su estado siempre (icono pausa↔play y portada), incluso con la app en
 * segundo plano.
 *
 * Este objeto es la única fuente de verdad de la UI: expone los StateFlows que
 * observan los ViewModels; el servicio empuja aquí el estado real del reproductor.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {
    /** Hilo principal: ExoPlayer solo se puede tocar desde él. */
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Cola actual en orden de presentación (sin shuffle). */
    private var songQueue: List<Song> = emptyList()

    /** Acción pendiente a ejecutar en cuanto el servicio esté enlazado (caso frío). */
    private var pendingAction: (() -> Unit)? = null

    /** Restauración de sesión: la cola se re-aplica a un player recién creado (frío o service muerto). */
    private var restoreStarted = false
    private var restoreDone = false
    private var queueAppliedToPlayer = false
    private val restoreActions = ArrayDeque<() -> Unit>()

    /** Milisegundos reproducidos pendientes de volcar a [UsageStore]. */
    private var pendingListeningMs = 0L

    init {
        // Alimenta "Escuchados recientemente" y el conteo de reproducciones cuando una
        // canción empieza a sonar (currentSong cambia con isPlaying = true).
        widgetScope.launch {
            combine(_currentSong.drop(1), _isPlaying) { song, playing -> song to playing }
                .distinctUntilChanged()
                .collect { (song, playing) ->
                    if (playing && song != null) {
                        RecentStore.addOrMoveTop(context, song.id)
                        UsageStore.recordPlay(context, song.id)
                    }
                }
        }

        // Guarda la posición y el tiempo de escucha periódicamente mientras suena.
        widgetScope.launch {
            while (true) {
                delay(5_000)
                if (_isPlaying.value) {
                    pendingListeningMs += 5_000
                    mainHandler.post { persistSession(_currentIndex.value, readCurrentPosition()) }
                    if (pendingListeningMs >= 60_000) {
                        val toFlush = pendingListeningMs
                        pendingListeningMs = 0
                        UsageStore.recordListening(context, toFlush)
                    }
                }
            }
        }
    }

    val isShuffled: Boolean
        get() = player()?.shuffleModeEnabled ?: false

    val repeatMode: Int
        get() = player()?.repeatMode ?: Player.REPEAT_MODE_OFF

    /** Empuja el estado real del player hacia los StateFlows (llamado por el servicio). */
    fun onPlayerMediaItemTransition(index: Int) {
        _currentIndex.value = index
        _currentSong.value = songQueue.getOrNull(index)
        refreshWidget()
        persistSession(index, 0L)
    }

    fun onPlayerIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        Log.d(TAG, "isPlaying -> $isPlaying")
        refreshWidget()
        if (!isPlaying) {
            persistSession(_currentIndex.value, currentPosition())
            flushPendingListening()
        }
    }

    fun onServiceDestroyed() {
        pendingAction = null
        restoreStarted = false
        restoreDone = false
        queueAppliedToPlayer = false
        restoreActions.clear()
        persistSession(_currentIndex.value, readCurrentPosition())
        flushPendingListening()
    }

    /** Volca a [UsageStore] los milisegundos escuchados aún no persistidos. */
    private fun flushPendingListening() {
        val toFlush = pendingListeningMs
        if (toFlush <= 0) return
        pendingListeningMs = 0
        widgetScope.launch { UsageStore.recordListening(context, toFlush) }
    }

    /**
     * Asegura que el servicio de reproducción (y por tanto el ExoPlayer) exista.
     * Si el proceso acaba de nacer (widget en frío), el player se crea dentro de
     * unos milisegundos; [withPlayer] encola la acción pendiente hasta que exista.
     */
    private fun ensurePlayer() {
        val p = MediaPlaybackService.playerInstance
        if (p != null) return
        try {
            context.startForegroundService(
                Intent(context, MediaPlaybackService::class.java)
            )
            waitForPlayer()
        } catch (t: Throwable) {
            Log.e(TAG, "No se pudo iniciar el servicio de reproducción", t)
        }
    }

    /** Espera (de forma no bloqueante) a que el servicio cree el player. */
    private fun waitForPlayer() {
        mainHandler.post(object : Runnable {
            override fun run() {
                val p = MediaPlaybackService.playerInstance
                if (p != null) {
                    runPendingAction()
                } else {
                    mainHandler.postDelayed(this, 60)
                }
            }
        })
    }

    private fun runPendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        action()
    }

    private fun withPlayer(block: (Player) -> Unit) {
        ensurePlayer()
        val p = MediaPlaybackService.playerInstance
        if (p != null) {
            runWhenPlayerReady(p, block)
        } else {
            pendingAction = { withPlayer(block) }
            waitForPlayer()
        }
    }

    /** Encadena la acción detrás de la restauración de sesión cuando arranca en frío. */
    private fun runWhenPlayerReady(p: Player, block: (Player) -> Unit) {
        if (restoreDone) {
            runOnMain { block(p) }
            return
        }
        restoreActions.addLast { runOnMain { block(p) } }
        restoreQueueIfNeeded(p)
    }

    /** Ejecuta en el hilo main (ExoPlayer solo se toca desde él), sea cual sea el hilo llamante. */
    private fun runOnMain(run: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) run()
        else mainHandler.post(run)
    }

    /** Bloqueante: ejecuta en main y espera el resultado (uso para getters desde hilos de fondo). */
    private fun <T> runOnMainBlocking(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result: T? = null
        val gate = CountDownLatch(1)
        mainHandler.post { result = block(); gate.countDown() }
        gate.await(500, TimeUnit.MILLISECONDS)
        return result as T
    }

    private fun readCurrentPosition(): Long =
        runOnMainBlocking { MediaPlaybackService.playerInstance?.currentPosition ?: 0L }

    /** Restaura la última sesión (cola + índice + posición) una vez por arranque en frío. */
    private fun restoreQueueIfNeeded(p: Player) {
        if (restoreStarted) return
        restoreStarted = true
        widgetScope.launch {
            val session = SessionStateStore.load(context)
            val songsFromDb = session?.songIds?.let { ids ->
                val byId = songDao.getByIds(ids).associateBy { it.id }
                ids.mapNotNull { byId[it] }.map { it.toSong() }
            }.orEmpty()
            mainHandler.post {
                // Cola en memoria (proceso vivo pero service muerto) o recuperada de Room.
                val songs = songQueue.takeIf { it.isNotEmpty() } ?: songsFromDb
                if (!queueAppliedToPlayer && songs.isNotEmpty()) {
                    applySessionToPlayer(
                        p = p,
                        songs = songs,
                        index = session?.index ?: _currentIndex.value,
                        positionMs = session?.positionMs ?: 0L,
                    )
                }
                restoreDone = true
                while (restoreActions.isNotEmpty()) restoreActions.removeFirst().invoke()
            }
        }
    }

    private fun applySessionToPlayer(
        p: Player,
        songs: List<Song>,
        index: Int,
        positionMs: Long,
    ) {
        songQueue = songs
        val safeIndex = index.coerceIn(0, songs.lastIndex)
        p.setMediaItems(songs.map { it.toMediaItem() }, safeIndex, positionMs)
        _currentIndex.value = safeIndex
        _currentSong.value = songs[safeIndex]
        p.prepare()
        queueAppliedToPlayer = true
        Log.d(TAG, "sesión aplicada: ${songs.size} canciones index=$safeIndex pos=$positionMs")
        refreshWidget()
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        require(songs.isNotEmpty())
        require(startIndex in songs.indices)
        songQueue = songs
        persistSession(startIndex, 0L)
        withPlayer { p ->
            p.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
            _currentIndex.value = startIndex
            _currentSong.value = songs[startIndex]
            p.prepare()
            p.play()
            refreshWidget()
        }
    }

    /**
     * Reanuda la última sesión guardada (cola + índice + posición): sirve a la tarjeta
     * "Seguir escuchando" de Inicio. Si el player ya tiene esa sesión cargada solo se
     * busca la posición y se reproduce; si no, la re-aplica desde las canciones en memoria
     * o recuperadas de la base de datos.
     */
    fun resumeLastSession() {
        widgetScope.launch {
            val session = SessionStateStore.load(context) ?: return@launch
            val savedIds = session.songIds
            val restoredSongs = if (songQueue.map { it.id } == savedIds) {
                songQueue
            } else {
                songDao.getByIds(savedIds).map { it.toSong() }
            }
            if (restoredSongs.isEmpty()) return@launch
            val targetId = restoredSongs.getOrNull(session.index)?.id
            withPlayer { p ->
                val alreadyLoaded = p.mediaItemCount == restoredSongs.size &&
                    p.currentMediaItem?.mediaId == targetId?.toString()
                if (alreadyLoaded) {
                    p.seekTo(session.positionMs)
                } else {
                    applySessionToPlayer(p, restoredSongs, session.index, session.positionMs)
                }
                p.play()
            }
        }
    }

    fun togglePlayPause() {
        withPlayer { p -> if (p.isPlaying) p.pause() else p.play() }
    }

    fun skipToNext() {
        withPlayer { it.seekToNextMediaItem() }
    }

    fun skipToPrevious() {
        withPlayer { p ->
            val position = p.currentPosition
            // Si llevamos más de 3s reproducidos, reiniciamos la actual; si no, la anterior
            if (position > 3_000) {
                p.seekTo(0L)
            } else {
                p.seekToPreviousMediaItem()
            }
        }
    }

    fun skipToIndex(index: Int) {
        withPlayer { p -> if (index in songQueue.indices) p.seekTo(index, 0L) }
    }

    fun seekTo(positionMs: Long) {
        withPlayer { it.seekTo(positionMs) }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        withPlayer { it.shuffleModeEnabled = enabled }
    }

    fun setRepeatMode(repeatMode: Int) {
        withPlayer { it.repeatMode = repeatMode }
    }

    fun toggleRepeatMode() {
        withPlayer { p ->
            p.repeatMode = when (p.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun currentPosition(): Long = readCurrentPosition()

    fun duration(): Long =
        runOnMainBlocking { MediaPlaybackService.playerInstance?.duration?.takeIf { it > 0 } ?: 0L }

    fun currentMediaItems(): List<Song> = songQueue

    private fun player(): Player? = MediaPlaybackService.playerInstance

    /** Persiste la sesión (cola + índice + posición) para la restauración en frío. */
    private fun persistSession(index: Int, positionMs: Long) {
        if (songQueue.isEmpty()) return
        val ids = songQueue.map { it.id }
        widgetScope.launch {
            SessionStateStore.save(context, SessionState(ids, index, positionMs))
        }
    }

    /** Persiste el estado visible del widget y pide que se redibuje. */
    private fun refreshWidget() {
        val song = _currentSong.value
        widgetScope.launch {
            try {
                WidgetStateStore.save(
                    context,
                    WidgetState(
                        title = song?.title,
                        artist = song?.artist,
                        albumArtUri = song?.albumArtUri,
                        albumId = song?.albumId,
                        isPlaying = _isPlaying.value,
                    ),
                )
                val ids = GlanceAppWidgetManager(context).getGlanceIds(MusicWidget::class.java)
                if (ids.isNotEmpty()) {
                    withTimeoutOrNull(10_000) { MusicWidget.updateAll(context) }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error refrescando el widget", t)
            }
        }
    }

    private fun Song.toMediaItem() = androidx.media3.common.MediaItem.Builder()
        .setUri(Uri.parse(path))
        .setMediaId(id.toString())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri)
                .setDurationMs(durationMs)
                .build()
        )
        .build()

    private fun SongEntity.toSong() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        path = path,
        albumArtUri = albumId.takeIf { it > 0 }?.let {
            Uri.parse("content://media/external/audio/albumart/$it")
        },
        trackNumber = trackNumber,
        albumId = albumId,
    )

    private companion object {
        const val TAG = "PlaybackController"
    }
}