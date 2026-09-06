package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [Index(value = ["albumId"]), Index(value = ["artistId"])]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val path: String,
    val trackNumber: Int?,
    val dateAdded: Long
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val year: Int?
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String
)

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Long,
    val name: String
)

@Entity(
    tableName = "song_genres",
    primaryKeys = ["songId", "genreId"],
    indices = [Index(value = ["genreId"])]
)
data class SongGenreEntity(
    val songId: Long,
    val genreId: Long
)
