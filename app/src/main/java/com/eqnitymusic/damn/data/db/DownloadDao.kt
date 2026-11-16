package com.eqnitymusic.damn.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.eqnitymusic.damn.data.model.Download
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<Download>): List<Long>

    @Update
    suspend fun update(download: Download): Int

    @Delete
    suspend fun delete(download: Download): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND dateCreated < :beforeDate")
    suspend fun deleteOldCompleted(beforeDate: Long): Int

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getById(id: Long): Flow<Download?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getByIdSync(id: Long): Download?

    @Query("SELECT * FROM downloads ORDER BY dateCreated DESC")
    fun getAll(): Flow<List<Download>>

    @Query("SELECT * FROM downloads ORDER BY dateCreated DESC")
    suspend fun getAllSync(): List<Download>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY dateCreated DESC")
    fun getByStatus(status: String): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE sourceUrl = :url")
    fun getBySourceUrl(url: String): Flow<Download?>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADING'")
    suspend fun getActiveDownloadCount(): Int
}