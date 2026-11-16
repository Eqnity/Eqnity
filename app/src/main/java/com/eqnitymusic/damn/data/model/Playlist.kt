package com.eqnitymusic.damn.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.io.Serializable

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)],
    foreignKeys = []
)
@Serializable
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverArtPath: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val totalDuration: Long = 0,
    val isSystem: Boolean = false
) : Serializable {

    fun getDisplayName(): String = if (name.isNotBlank()) name else "Unnamed Playlist"
    
    fun getFormattedDuration(): String {
        val hours = totalDuration / 3600000
        val minutes = (totalDuration % 3600000) / 60000
        return if (hours > 0) {
            String.format("%dh %dm", hours, minutes)
        } else {
            String.format("%dm", minutes)
        }
    }
}