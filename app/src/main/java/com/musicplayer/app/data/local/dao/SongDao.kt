package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY COALESCE(trackNumber, 9999) ASC, title COLLATE NOCASE ASC")
    fun observeByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY album COLLATE NOCASE ASC, COALESCE(trackNumber, 9999) ASC")
    fun observeByArtist(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)
}
