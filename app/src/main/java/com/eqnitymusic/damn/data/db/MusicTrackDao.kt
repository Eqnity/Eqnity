package com.eqnitymusic.damn.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.eqnitymusic.damn.data.model.MusicTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: MusicTrack): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<MusicTrack>): List<Long>

    @Update
    suspend fun update(track: MusicTrack): Int

    @Delete
    suspend fun delete(track: MusicTrack): Int

    @Query("DELETE FROM music_tracks WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM music_tracks")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM music_tracks WHERE id = :id")
    fun getById(id: Long): Flow<MusicTrack?>

    @Query("SELECT * FROM music_tracks WHERE id = :id")
    suspend fun getByIdSync(id: Long): MusicTrack?

    @Query("SELECT * FROM music_tracks ORDER BY dateAdded DESC")
    fun getAll(): Flow<List<MusicTrack>>

    @Query("SELECT * FROM music_tracks ORDER BY dateAdded DESC")
    suspend fun getAllSync(): List<MusicTrack>

    @Query("SELECT * FROM music_tracks WHERE title LIKE :query OR artist LIKE :query ORDER BY dateAdded DESC")
    fun search(query: String): Flow<List<MusicTrack>>

    @Query("SELECT * FROM music_tracks WHERE filePath = :path")
    fun getByFilePath(path: String): Flow<MusicTrack?>

    @Query("SELECT * FROM music_tracks WHERE sourceUrl = :url")
    fun getBySourceUrl(url: String): Flow<MusicTrack?>

    @Query("SELECT * FROM music_tracks ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int): Flow<List<MusicTrack>>

    @Query("SELECT * FROM music_tracks ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int): Flow<List<MusicTrack>>

    @Query("SELECT COUNT(*) FROM music_tracks")
    suspend fun getCount(): Int

    @Query("UPDATE music_tracks SET playCount = playCount + 1, lastPlayed = :now WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, now: Long): Int

    @Query("UPDATE music_tracks SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean): Int
}