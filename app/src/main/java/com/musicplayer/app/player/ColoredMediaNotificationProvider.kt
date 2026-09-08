package com.musicplayer.app.player

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.musicplayer.app.core.util.AlbumArtColorExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Provider personalizado de la notificación MediaStyle: replica el comportamiento de
 * [androidx.media3.session.DefaultMediaNotificationProvider] (controles prev/play/next, carátula
 * como large icon, VISIBILITY_PUBLIC para lockscreen, acciones conectadas a la sesión vía
 * ActionFactory) pero añadiendo el color dominante de la carátula como color de acento (y
 * colorizado en Android 12+), igual que hacen el mini-player y el player de la app.
 *
 * El color se aplica de dos formas: de forma síncrona si ya está en la caché de Palette, o de
 * forma asíncrona (decode + Palette en IO) repintando la notificación vía callback con el mismo
 * builder, replicando el patrón del provider por defecto.
 */
@UnstableApi
class ColoredMediaNotificationProvider(
    private val context: android.content.Context,
    private val channelId: String,
    private val colorExtractor: AlbumArtColorExtractor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : MediaNotification.Provider {

    private val mainExecutor: java.util.concurrent.Executor =
        androidx.core.content.ContextCompat.getMainExecutor(context)

    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val player = mediaSession.player
        val metadata = player.mediaMetadata
        val notificationId = DEFAULT_NOTIFICATION_ID

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(metadata.title)
            .setContentText(metadata.artist)
            .setContentIntent(mediaSession.sessionActivity)
            .setDeleteIntent(actionFactory.createNotificationDismissalIntent(mediaSession))
            .setOnlyAlertOnce(true)
            .setSmallIcon(SMALL_ICON_RES_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setGroup(null)
            .setWhen(0L)

        val compactViewIndices = addNotificationActions(mediaSession, builder, actionFactory)
        builder.setStyle(
            MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(*compactViewIndices)
        )

        // Carga asíncrona: large icon (carátula) + color dominante (Palette), y repinta vía
        // callback cuando están listos, replicando el patrón del DefaultMediaNotificationProvider.
        val bitmapFuture = mediaSession.bitmapLoader.loadBitmapFromMetadata(metadata)
        if (bitmapFuture != null) {
            val artworkUri = metadata.artworkUri
            if (bitmapFuture.isDone) {
                try {
                    applyBitmapAndColor(builder, Futures.getDone(bitmapFuture), artworkUri)
                } catch (e: Exception) {
                    // se ignora: sin portada se muestra la notificación sin color
                }
            } else {
                Futures.addCallback(
                    bitmapFuture,
                    object : FutureCallback<Bitmap> {
                        override fun onSuccess(result: Bitmap) {
                            applyBitmapAndColor(builder, result, artworkUri)
                            callback.onNotificationChanged(
                                MediaNotification(notificationId, builder.build())
                            )
                        }

                        override fun onFailure(t: Throwable) {
                            applyColorAsync(builder, artworkUri, notificationId, callback)
                        }
                    },
                    mainExecutor
                )
            }
        }

        // Color de forma síncrona si ya está cacheado por el mini-player/player.
        metadata.artworkUri?.let { uri ->
            colorExtractor.peekFromUri(uri)?.let { color ->
                builder.setColor(color)
            }
        }

        return MediaNotification(notificationId, builder.build())
    }

    private fun applyBitmapAndColor(
        builder: NotificationCompat.Builder,
        bitmap: Bitmap,
        artworkUri: android.net.Uri?,
    ) {
        builder.setLargeIcon(bitmap)
        if (artworkUri != null) {
            colorExtractor.peekFromUri(artworkUri)?.let { color ->
                applyColor(builder, color)
            } ?: run {
                scope.launch {
                    val color = colorExtractor.extractFromUri(artworkUri)
                    if (color != null) applyColor(builder, color)
                }
            }
        }
    }

    private fun applyColorAsync(
        builder: NotificationCompat.Builder,
        artworkUri: android.net.Uri?,
        notificationId: Int,
        callback: MediaNotification.Provider.Callback,
    ) {
        if (artworkUri == null) return
        scope.launch {
            val color = colorExtractor.extractFromUri(artworkUri)
            if (color != null) {
                applyColor(builder, color)
                callback.onNotificationChanged(
                    MediaNotification(notificationId, builder.build())
                )
            }
        }
    }

    private fun addNotificationActions(
        mediaSession: MediaSession,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory,
    ): IntArray {
        val player = mediaSession.player
        val commands = player.availableCommands
        val showPause = player.isPlaying
        val compactIndices = mutableListOf<Int>()
        var index = 0

        if (commands.containsAny(
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            )
        ) {
            builder.addAction(
                actionFactory.createMediaAction(
                    mediaSession,
                    IconCompat.createWithResource(context, R.drawable.media3_icon_previous),
                    context.getString(R.string.media3_controls_seek_to_previous_description),
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                )
            )
            compactIndices.add(index)
            index++
        }

        if (commands.contains(Player.COMMAND_PLAY_PAUSE)) {
            builder.addAction(
                actionFactory.createMediaAction(
                    mediaSession,
                    IconCompat.createWithResource(
                        context,
                        if (showPause) R.drawable.media3_icon_pause
                        else R.drawable.media3_icon_play
                    ),
                    context.getString(
                        if (showPause) R.string.media3_controls_pause_description
                        else R.string.media3_controls_play_description
                    ),
                    Player.COMMAND_PLAY_PAUSE
                )
            )
            compactIndices.add(index)
            index++
        }

        if (commands.containsAny(
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
            )
        ) {
            builder.addAction(
                actionFactory.createMediaAction(
                    mediaSession,
                    IconCompat.createWithResource(context, R.drawable.media3_icon_next),
                    context.getString(R.string.media3_controls_seek_to_next_description),
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                )
            )
            compactIndices.add(index)
        }

        return compactIndices.toIntArray()
    }

    private fun applyColor(builder: NotificationCompat.Builder, color: Int) {
        builder.setColor(color)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setColorized(true)
        }
    }

    override fun handleCustomCommand(
        mediaSession: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = false

    private companion object {
        const val DEFAULT_NOTIFICATION_ID = 1001
        const val SMALL_ICON_RES_ID = com.musicplayer.app.R.drawable.ic_widget_music_note
    }
}