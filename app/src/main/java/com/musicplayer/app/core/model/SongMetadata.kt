package com.musicplayer.app.core.model

data class SongMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?,
    val trackNumber: Int?,
    val durationMs: Long,
    val mimeType: String?,
    val codec: String?,
    val bitrateBps: Long?,
    val sampleRateHz: Long?,
    val fileSizeBytes: Long?,
    val path: String
)