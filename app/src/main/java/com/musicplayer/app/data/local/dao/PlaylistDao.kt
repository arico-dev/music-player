package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.musicplayer.app.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val trackCount: Int
)

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, COUNT(ps.songId) AS trackCount
        FROM playlists p
        LEFT JOIN playlist_songs ps ON ps.playlistId = p.id
        GROUP BY p.id, p.name
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observeAllWithCounts(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)
}