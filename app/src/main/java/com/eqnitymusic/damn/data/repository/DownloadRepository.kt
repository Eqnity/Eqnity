package com.eqnitymusic.damn.data.repository

import com.eqnitymusic.damn.data.db.AppDatabase
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val dao = database.downloadDao()

    fun getAllDownloads(): Flow<List<Download>> = dao.getAll()

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>> = dao.getByStatus(status.name)

    fun getDownloadById(id: Long): Flow<Download?> = dao.getById(id)

    suspend fun getDownloadByIdSync(id: Long): Download? = dao.getByIdSync(id)

    fun getDownloadBySourceUrl(url: String): Flow<Download?> = dao.getBySourceUrl(url)

    suspend fun insertDownload(download: Download): Long = dao.insert(download)

    suspend fun updateDownload(download: Download): Int = dao.update(download)

    suspend fun deleteDownload(download: Download): Int = dao.delete(download)

    suspend fun deleteDownloadById(id: Long): Int = dao.deleteById(id)

    suspend fun deleteOldCompleted(beforeDate: Long): Int = dao.deleteOldCompleted(beforeDate)

    suspend fun getActiveDownloadCount(): Int = dao.getActiveDownloadCount()
}