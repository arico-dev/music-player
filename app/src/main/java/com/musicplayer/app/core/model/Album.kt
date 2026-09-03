package com.musicplayer.app.core.model

import android.net.Uri

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val year: Int?
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int
)

data class Genre(
    val id: Long,
    val name: String,
    val trackCount: Int
)
