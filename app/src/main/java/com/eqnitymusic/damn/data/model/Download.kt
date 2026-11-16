package com.eqnitymusic.damn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "downloads")
@Serializable
data class Download(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceUrl: String,
    val title: String = "",
    val artist: String = "",
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val filePath: String = "",
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val errorMessage: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateCompleted: Long = 0,
    val trackId: Long = 0
) : Comparable<Download> {

    override fun compareTo(other: Download): Int {
        return other.dateCreated.compareTo(this.dateCreated)
    }
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}