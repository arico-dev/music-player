package com.musicplayer.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.core.util.AlbumArtColorExtractor
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Expone el [PlaybackController] singleton a los composables de la raíz (mini-player). */
@HiltViewModel
class RootViewModel @Inject constructor(
    val playbackController: PlaybackController,
    private val colorExtractor: AlbumArtColorExtractor
) : ViewModel() {

    private val _dominantColor = MutableStateFlow<Int?>(null)
    val dominantColor: StateFlow<Int?> = _dominantColor

    init {
        viewModelScope.launch {
            playbackController.currentSong.collect { song ->
                _dominantColor.value = colorExtractor.extractFromUri(song?.albumArtUri)
            }
        }
    }
}