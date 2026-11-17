package com.eqnitymusic.damn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.eqnitymusic.damn.data.db.Converters
import java.io.Serializable

@Entity(tableName = "music_tracks")
data class MusicTrack(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "",
    val duration: Long = 0,
    val filePath: String,
    val fileSize: Long = 0,
    val sourceUrl: String = "",
    val artworkPath: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val mimeType: String = "audio/mpeg",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0
) : Serializable {

    fun getDisplayTitle(): String = if (title.isNotBlank()) title else "Unknown Title"
    
    fun getDisplayArtist(): String = if (artist.isNotBlank()) artist else "Unknown Artist"
    
    fun getFormattedDuration(): String {
        val minutes = duration / 60000
        val seconds = (duration % 60000) / 1000
        return String.format("%d:%02d", minutes, seconds)
    }
    
    fun getFormattedFileSize(): String {
        return when {
            fileSize >= 1024 * 1024 * 1024 -> String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024))
            fileSize >= 1024 * 1024 -> String.format("%.2f MB", fileSize / (1024.0 * 1024))
            fileSize >= 1024 -> String.format("%.2f KB", fileSize / 1024.0)
            else -> "$fileSize B"
        }
    }
}