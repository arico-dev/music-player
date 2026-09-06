package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.local.entity.SongGenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongGenreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(relations: List<SongGenreEntity>)

    @Query("DELETE FROM song_genres")
    suspend fun clearAll()

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN song_genres sg ON sg.songId = s.id
        WHERE sg.genreId = :genreId
        ORDER BY s.title COLLATE NOCASE ASC
        """
    )
    fun observeSongsByGenre(genreId: Long): Flow<List<SongEntity>>
}