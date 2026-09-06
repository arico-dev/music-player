package com.musicplayer.app.data.lyrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class LrcLibLyrics(
    @SerialName("id") val id: Long? = null,
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("albumName") val albumName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    val instrumental: Boolean = false,
    @SerialName("plainLyrics") val plainLyrics: String? = null,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null
)

interface LrcLibApi {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String?,
        @Query("track_name") trackName: String,
        @Query("album_name") albumName: String? = null
    ): Response<LrcLibLyrics>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): List<LrcLibLyrics>
}