package com.musicplayer.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.AlbumEntity
import com.musicplayer.app.data.local.entity.ArtistEntity
import com.musicplayer.app.data.local.entity.GenreEntity
import com.musicplayer.app.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        GenreEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao

    companion object {
        fun build(context: Context): MusicDatabase =
            Room.databaseBuilder(context, MusicDatabase::class.java, "music_player.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
