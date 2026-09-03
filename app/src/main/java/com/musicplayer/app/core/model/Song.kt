package com.musicplayer.app.core.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val albumArtUri: Uri?,
    val trackNumber: Int?,
    val albumId: Long?
) {
    val uri: Uri get() = Uri.parse(path)
}
