package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.GenreEntity
import kotlinx.coroutines.flow.Flow

data class GenreWithCount(
    val id: Long,
    val name: String,
    val trackCount: Int
)

@Dao
interface GenreDao {

    @Query(
        """
        SELECT g.id, g.name, COUNT(sg.songId) AS trackCount
        FROM genres g
        LEFT JOIN song_genres sg ON sg.genreId = g.id
        GROUP BY g.id, g.name
        ORDER BY g.name COLLATE NOCASE ASC
        """
    )
    fun observeAllWithCounts(): Flow<List<GenreWithCount>>

    @Query("SELECT * FROM genres WHERE id = :id")
    fun observeById(id: Long): Flow<GenreEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(genres: List<GenreEntity>)

    @Query("DELETE FROM genres WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)
}