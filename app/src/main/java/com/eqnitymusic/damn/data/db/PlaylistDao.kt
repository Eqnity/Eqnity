package com.eqnitymusic.damn.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Transaction
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.data.model.PlaylistTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<Playlist>): List<Long>

    @Update
    suspend fun update(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlist: Playlist): Int

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getById(id: Long): Flow<Playlist?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getByIdSync(id: Long): Playlist?

    @Query("SELECT * FROM playlists ORDER BY dateModified DESC")
    fun getAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists ORDER BY dateModified DESC")
    suspend fun getAllSync(): List<Playlist>

    @Query("SELECT * FROM playlists WHERE name LIKE :query ORDER BY dateModified DESC")
    fun search(query: String): Flow<List<Playlist>>

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getCount(): Int

    // Playlist Tracks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(playlistTrack: PlaylistTrack): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTracksToPlaylist(playlistTracks: List<PlaylistTrack>): List<Long>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long): Int

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrack>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistTracksSync(playlistId: Long): List<PlaylistTrack>

    @Transaction
    @Query("SELECT mt.* FROM music_tracks mt INNER JOIN playlist_tracks pt ON mt.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC")
    fun getPlaylistMusicTracks(playlistId: Long): Flow<List<MusicTrack>>

    @Transaction
    @Query("SELECT mt.* FROM music_tracks mt INNER JOIN playlist_tracks pt ON mt.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC")
    suspend fun getPlaylistMusicTracksSync(playlistId: Long): List<MusicTrack>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getPlaylistTrackCount(playlistId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getNextPosition(playlistId: Long): Int

    @Query("UPDATE playlist_tracks SET position = :newPosition WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackPosition(playlistId: Long, trackId: Long, newPosition: Int): Int

    @Query("UPDATE playlists SET songCount = :count, totalDuration = :duration, dateModified = :now WHERE id = :id")
    suspend fun updatePlaylistStats(id: Long, count: Int, duration: Long, now: Long): Int
}