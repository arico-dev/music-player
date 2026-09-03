package com.musicplayer.app.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.musicplayer.app.core.model.Song
import com.musicplayer.app.core.util.AlbumArtColorExtractor
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val colorExtractor: AlbumArtColorExtractor
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playbackController.currentSong
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPlaying: StateFlow<Boolean> = playbackController.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val currentIndex: StateFlow<Int> = playbackController.currentIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _dominantColor = MutableStateFlow<Int?>(null)
    val dominantColor: StateFlow<Int?> = _dominantColor

    init {
        viewModelScope.launch {
            playbackController.currentSong.collect { song ->
                _dominantColor.value = colorExtractor.extractFromUri(song?.albumArtUri)
            }
        }
        viewModelScope.launch {
            while (true) {
                _queue.value = playbackController.currentMediaItems()
                _position.value = playbackController.currentPosition()
                _duration.value = playbackController.duration()
                _isShuffled.value = playbackController.isShuffled
                _repeatMode.value = playbackController.repeatMode
                delay(500)
            }
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun skipToNext() = playbackController.skipToNext()

    fun skipToPrevious() = playbackController.skipToPrevious()

    fun skipToIndex(index: Int) = playbackController.skipToIndex(index)

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun toggleShuffle() = playbackController.setShuffleEnabled(!playbackController.isShuffled)

    fun cycleRepeatMode() = playbackController.toggleRepeatMode()

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }
}
