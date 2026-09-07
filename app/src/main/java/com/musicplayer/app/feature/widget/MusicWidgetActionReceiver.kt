package com.musicplayer.app.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe los taps de los botones del widget y los traduce a llamadas al reproductor.
 * ExoPlayer solo puede tocarse desde el hilo main, y `onReceive` ya corre en él;
 * el refresco del widget lo dispara el propio [PlaybackController] con su estado.
 */
@AndroidEntryPoint
class MusicWidgetActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playbackController: PlaybackController

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        try {
            val action = intent.getStringExtra(MusicWidget.EXTRA_ACTION)
            Log.d(TAG, "acción del widget: $action")
            when (action) {
                MusicWidget.ACTION_TOGGLE -> playbackController.togglePlayPause()
                MusicWidget.ACTION_NEXT -> playbackController.skipToNext()
                MusicWidget.ACTION_PREVIOUS -> playbackController.skipToPrevious()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error ejecutando acción del widget", t)
        } finally {
            pendingResult.finish()
        }
    }

    private companion object {
        const val TAG = "MusicWidgetAction"
    }
}