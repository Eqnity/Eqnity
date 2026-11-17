package com.eqnitymusic.damn.worker

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.network.ApiService
import com.eqnitymusic.damn.data.repository.DownloadRepository
import com.eqnitymusic.damn.data.repository.MusicRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.io.InputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: DownloadRepository,
    private val musicRepository: MusicRepository,
    private val apiService: ApiService,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    private val downloadId = inputData.getLong("download_id", -1)
    private val sourceUrl = inputData.getString("source_url") ?: ""

    override suspend fun doWork(): Result {
        if (downloadId == -1L) {
            return Result.failure()
        }

        return try {
            // Update status to downloading
            downloadRepository.getDownloadByIdSync(downloadId)?.let { download ->
                val updatedDownload = download.copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0
                )
                downloadRepository.updateDownload(updatedDownload)
            }

            // Get download URL from ytmp3.gl
            val downloadUrl = getDownloadUrl(sourceUrl)
                ?: return@doWork Result.failure()

            // Download the file
            val filePath = downloadFile(downloadUrl)
                ?: return@doWork Result.failure()

            // Create music track entry
            val track = createMusicTrack(filePath, sourceUrl)
            
            // Update download as completed
            downloadRepository.getDownloadByIdSync(downloadId)?.let { download ->
                val completedDownload = download.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    filePath = filePath,
                    dateCompleted = System.currentTimeMillis(),
                    trackId = track.id
                )
                downloadRepository.updateDownload(completedDownload)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Download failed", e)
            downloadRepository.getDownloadByIdSync(downloadId)?.let { download ->
                val failedDownload = download.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "Unknown error"
                )
                downloadRepository.updateDownload(failedDownload)
            }
            Result.failure()
        }
    }

    private suspend fun getDownloadUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getYtmp3Info(url).execute()
            if (response.isSuccessful) {
                response.body()?.downloadUrl
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Failed to get download URL", e)
            null
        }
    }

    private suspend fun downloadFile(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            val body = response.body?.byteStream()
                ?: throw IOException("Response body is null")

            val fileName = generateFileName(url)
            val file = createMusicFile(fileName)
            
            val sink = file.sink().buffer()
            val inputStream = body as InputStream
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                sink.write(buffer, 0, bytesRead)
            }
            sink.close()
            inputStream.close()

            file.absolutePath
        } catch (e: Exception) {
            Log.e("DownloadWorker", "File download failed", e)
            null
        }
    }

    private fun generateFileName(url: String): String {
        val uri = Uri.parse(url)
        val path = uri.path ?: "download"
        val fileName = File(path).name
        return if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a")) {
            fileName
        } else {
            "${System.currentTimeMillis()}.mp3"
        }
    }

    private fun createMusicFile(fileName: String): File {
        val musicDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "EqnityMusic"
        )
        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }
        return File(musicDir, fileName)
    }

    private suspend fun createMusicTrack(filePath: String, sourceUrl: String): MusicTrack = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val title = file.nameWithoutExtension
        
        val track = MusicTrack(
            title = title,
            artist = "Unknown Artist",
            filePath = filePath,
            fileSize = file.length(),
            sourceUrl = sourceUrl,
            mimeType = "audio/mpeg"
        )
        
        val trackId = musicRepository.insertTrack(track)
        track.copy(id = trackId)
    }
}