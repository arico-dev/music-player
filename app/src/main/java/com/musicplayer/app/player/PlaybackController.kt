package com.musicplayer.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicplayer.app.core.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    /** Cola actual de canciones en orden de presentación (sin shuffle). */
    private var songQueue: List<Song> = emptyList()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    val isShuffled: Boolean
        get() = player.shuffleModeEnabled

    val repeatMode: Int
        get() = player.repeatMode

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                _currentIndex.value = index
                _currentSong.value = songQueue.getOrNull(index)
            }
        })
    }

    /**
     * Carga una cola de canciones y empieza a reproducir desde [startIndex].
     * La cola persiste hasta que se llame con otra lista.
     */
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        require(songs.isNotEmpty())
        require(startIndex in songs.indices)

        songQueue = songs
        player.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        _currentIndex.value = startIndex
        _currentSong.value = songs[startIndex]
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipToNext() {
        player.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val position = player.currentPosition
        // Si llevamos más de 3s reproducidos, reiniciamos la actual; si no, vamos a la anterior
        if (position > 3_000) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun skipToIndex(index: Int) {
        if (index in songQueue.indices) {
            player.seekTo(index, 0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(repeatMode: Int) {
        player.repeatMode = repeatMode
    }

    fun toggleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun currentPosition(): Long = player.currentPosition

    fun duration(): Long = player.duration.takeIf { it > 0 } ?: 0L

    fun currentMediaItems(): List<Song> = songQueue

    fun release() {
        player.release()
    }

    private fun Song.toMediaItem() = MediaItem.Builder()
        .setUri(Uri.parse(path))
        .setMediaId(id.toString())
        .build()
}
