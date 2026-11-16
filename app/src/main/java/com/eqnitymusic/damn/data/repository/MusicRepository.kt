package com.eqnitymusic.damn.data.repository

import com.eqnitymusic.damn.data.db.AppDatabase
import com.eqnitymusic.damn.data.model.MusicTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val dao = database.musicTrackDao()

    fun getAllTracks(): Flow<List<MusicTrack>> = dao.getAll()

    fun searchTracks(query: String): Flow<List<MusicTrack>> = dao.search("%$query%")

    fun getTrackById(id: Long): Flow<MusicTrack?> = dao.getById(id)

    suspend fun getTrackByIdSync(id: Long): MusicTrack? = dao.getByIdSync(id)

    fun getTrackByFilePath(path: String): Flow<MusicTrack?> = dao.getByFilePath(path)

    fun getTrackBySourceUrl(url: String): Flow<MusicTrack?> = dao.getBySourceUrl(url)

    suspend fun insertTrack(track: MusicTrack): Long = dao.insert(track)

    suspend fun insertTracks(tracks: List<MusicTrack>): List<Long> = dao.insertAll(tracks)

    suspend fun updateTrack(track: MusicTrack): Int = dao.update(track)

    suspend fun deleteTrack(track: MusicTrack): Int = dao.delete(track)

    suspend fun deleteTrackById(id: Long): Int = dao.deleteById(id)

    suspend fun deleteAllTracks(): Int = dao.deleteAll()

    suspend fun incrementPlayCount(id: Long): Int = dao.incrementPlayCount(id, System.currentTimeMillis())

    suspend fun setFavorite(id: Long, favorite: Boolean): Int = dao.setFavorite(id, favorite)

    fun getMostPlayed(limit: Int = 50): Flow<List<MusicTrack>> = dao.getMostPlayed(limit)

    fun getRecentlyPlayed(limit: Int = 50): Flow<List<MusicTrack>> = dao.getRecentlyPlayed(limit)

    suspend fun getTrackCount(): Int = dao.getCount()
}