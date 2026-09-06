package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.musicplayer.app.data.local.entity.PlaylistSongEntity
import com.musicplayer.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongDao {

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
        """
    )
    fun observeSongsByPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COALESCE(MAX(position), 0) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<PlaylistSongEntity>)

    @Delete
    suspend fun delete(relation: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearByPlaylist(playlistId: Long)

    @Transaction
    suspend fun replaceAll(playlistId: Long, relations: List<PlaylistSongEntity>) {
        clearByPlaylist(playlistId)
        insertAll(relations)
    }
}