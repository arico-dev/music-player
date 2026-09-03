package com.musicplayer.app

import androidx.lifecycle.ViewModel
import com.musicplayer.app.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Expone el [PlaybackController] singleton a los composables de la raíz (mini-player). */
@HiltViewModel
class RootViewModel @Inject constructor(
    val playbackController: PlaybackController
) : ViewModel()
