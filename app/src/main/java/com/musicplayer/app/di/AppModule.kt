package com.musicplayer.app.di

import android.content.Context
import androidx.room.Room
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.GenreDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.PlaylistSongDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.dao.SongGenreDao
import com.musicplayer.app.data.local.database.MusicDatabase
import com.musicplayer.app.data.lyrics.LrcLibApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
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

    @Provides
    fun provideGenreDao(db: MusicDatabase): GenreDao = db.genreDao()

    @Provides
    fun provideSongGenreDao(db: MusicDatabase): SongGenreDao = db.songGenreDao()

    @Provides
    fun providePlaylistDao(db: MusicDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun providePlaylistSongDao(db: MusicDatabase): PlaylistSongDao = db.playlistSongDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                )
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideLrcLibApi(retrofit: Retrofit): LrcLibApi = retrofit.create(LrcLibApi::class.java)

    @Provides
    @Singleton
    fun provideDeezerArtistApi(json: Json, client: OkHttpClient): com.musicplayer.app.data.artist.DeezerArtistApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(com.musicplayer.app.data.artist.DeezerArtistApi::class.java)
    }
}
