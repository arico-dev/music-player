package com.musicplayer.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicplayer.app.MainActivity
import com.musicplayer.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Servicio en primer plano que aloja al reproductor real (ExoPlayer + MediaSession).
 *
 * Al correr como servicio en primer plano, el proceso permanece vivo con la app
 * cerrada; con ello las acciones del widget y el refresco de Glance siempre se
 * ejecutan aunque la app esté en segundo plano (icono pausa↔play y portada).
 *
 * Service y app viven en el mismo proceso, así que el reproductor se expone como
 * singleton ([playerInstance]) y [PlaybackController] lo usa directamente. El estado
 * observable de la UI se empuja a [PlaybackController], única fuente de verdad.
 */
@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var colorExtractor: com.musicplayer.app.core.util.AlbumArtColorExtractor

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        MediaPlaybackService.playerInstance = p

        val session = MediaSession.Builder(this, p)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackController.onPlayerIsPlayingChanged(isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playbackController.onPlayerMediaItemTransition(p.currentMediaItemIndex)
            }
        })

        mediaSession = session
        // IMPORTANTE: dar de alta la sesión en el servicio. Es lo que hace que el
        // MediaNotificationManager interno cree su controller de notificación y empiece a
        // vigilar la reproducción. Sin addSession(), shouldShowNotification() devuelve false
        // y NO se publica la notificación MediaStyle (por eso solo quedaba la neutral).
        addSession(session)

        // Provider personalizado: controles MediaStyle + color dominante de la carátula.
        setMediaNotificationProvider(
            ColoredMediaNotificationProvider(
                this,
                CHANNEL_ID,
                colorExtractor,
            )
        )

        // Notificación "neutral" para cubrir el arranque en foreground sin música aún
        // (startForegroundService() exige startForeground() a tiempo o crashea con
        // ForegroundServiceDidNotStartInTimeException). En cuanto empieza a reproducir, el
        // MediaNotificationManager la reemplaza por la MediaStyle con controles + carátula.
        val neutral = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_music_note)
            .setOngoing(false)
            .setContentTitle(getString(R.string.media_playback_notif_title))
            .setContentText(getString(R.string.media_playback_notif_text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
        startForeground(
            NOTIFICATION_ID,
            neutral,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        Log.d(TAG, "Media service creado; player listo")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.media_playback_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.media_playback_channel_description)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        ContextCompat.getSystemService(this, NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val keepPlaying = try {
            kotlinx.coroutines.runBlocking {
                com.musicplayer.app.data.settings.SettingsStore.current(this@MediaPlaybackService).keepPlayingInBackground
            }
        } catch (_: Exception) {
            false
        }
        val p = MediaPlaybackService.playerInstance
        if (!keepPlaying) {
            // Cerrar la app con "reproducir en segundo plano" desactivado: hay que detener la
            // reproducción de verdad (no solo pausar). Si solo pausamos, el MediaNotificationManager
            // de Media3 mantiene la notificación publicada porque su shouldShowNotification()
            // depende de que la cola (timeline) no esté vacía, y quedaría colgada sin sonido.
            try {
                p?.stop()
                p?.clearMediaItems()
            } catch (_: Exception) {
            }
            stopSelf()
            return
        }
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        playbackController.onServiceDestroyed()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        MediaPlaybackService.playerInstance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MediaPlaybackService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "media_playback"

        /** Referencia al reproductor en este proceso (mismo proceso que la app). */
        @Volatile
        var playerInstance: ExoPlayer? = null
    }
}