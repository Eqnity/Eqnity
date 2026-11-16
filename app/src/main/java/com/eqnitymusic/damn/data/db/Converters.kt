package com.eqnitymusic.damn.data.db

import androidx.room.TypeConverter
import com.eqnitymusic.damn.data.model.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toDownloadStatus(status: String?): DownloadStatus? {
        return status?.let { DownloadStatus.valueOf(it) }
    }
}