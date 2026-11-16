package com.eqnitymusic.damn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.data.model.PlaylistTrack

@Database(
    entities = [
        MusicTrack::class,
        Playlist::class,
        PlaylistTrack::class,
        Download::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicTrackDao(): MusicTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eqnity_music.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}