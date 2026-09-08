package com.musicplayer.app.data.artist

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class DeezerArtist(
    val id: Long = 0,
    val name: String = "",
    val picture: String? = null,
    val picture_small: String? = null,
    val picture_medium: String? = null,
    val picture_big: String? = null,
    val picture_xl: String? = null
)

@Serializable
data class DeezerArtistSearchResponse(
    val data: List<DeezerArtist> = emptyList(),
    val total: Int = 0
)

interface DeezerArtistApi {
    @GET("search/artist")
    suspend fun searchArtist(@Query("q") query: String): DeezerArtistSearchResponse
}
