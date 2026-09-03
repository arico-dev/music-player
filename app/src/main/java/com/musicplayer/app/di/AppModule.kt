package com.musicplayer.app.di

import android.content.Context
import androidx.room.Room
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.database.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(context, MusicDatabase::class.java, "music_player.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSongDao(db: MusicDatabase): SongDao = db.songDao()

    @Provides
    fun provideAlbumDao(db: MusicDatabase): AlbumDao = db.albumDao()

    @Provides
    fun provideArtistDao(db: MusicDatabase): ArtistDao = db.artistDao()
}
