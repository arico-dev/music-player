package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

data class ArtistWithCounts(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int
)

@Dao
interface ArtistDao {

    @Query(
        """
        SELECT a.id, a.name,
            (SELECT COUNT(DISTINCT s.albumId) FROM songs s WHERE s.artistId = a.id) AS albumCount,
            (SELECT COUNT(*) FROM songs s WHERE s.artistId = a.id) AS trackCount
        FROM artists a
        ORDER BY a.name COLLATE NOCASE ASC
        """
    )
    fun observeAllWithCounts(): Flow<List<ArtistWithCounts>>

    @Query(
        """
        SELECT a.id, a.name,
            (SELECT COUNT(DISTINCT s.albumId) FROM songs s WHERE s.artistId = a.id) AS albumCount,
            (SELECT COUNT(*) FROM songs s WHERE s.artistId = a.id) AS trackCount
        FROM artists a
        WHERE a.id = :id
        """
    )
    fun observeByIdWithCounts(id: Long): Flow<ArtistWithCounts?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Query("DELETE FROM artists")
    suspend fun clear()
}