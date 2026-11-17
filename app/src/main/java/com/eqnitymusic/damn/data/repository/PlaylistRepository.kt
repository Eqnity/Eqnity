package com.eqnitymusic.damn.data.repository

import com.eqnitymusic.damn.data.db.AppDatabase
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.data.model.PlaylistTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val dao = database.playlistDao()

    fun getAllPlaylists(): Flow<List<Playlist>> = dao.getAll()

    fun searchPlaylists(query: String): Flow<List<Playlist>> = dao.search("%$query%")

    fun getPlaylistById(id: Long): Flow<Playlist?> = dao.getById(id)

    suspend fun getPlaylistByIdSync(id: Long): Playlist? = dao.getByIdSync(id)

    suspend fun insertPlaylist(playlist: Playlist): Long = dao.insert(playlist)

    suspend fun updatePlaylist(playlist: Playlist): Int = dao.update(playlist)

    suspend fun deletePlaylist(playlist: Playlist): Int = dao.delete(playlist)

    suspend fun deletePlaylistById(id: Long): Int = dao.deleteById(id)

    suspend fun getPlaylistCount(): Int = dao.getCount()

    // Playlist Tracks
    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrack>> = dao.getPlaylistTracks(playlistId)

    suspend fun getPlaylistTracksSync(playlistId: Long): List<PlaylistTrack> = dao.getPlaylistTracksSync(playlistId)

    fun getPlaylistMusicTracks(playlistId: Long): Flow<List<MusicTrack>> = dao.getPlaylistMusicTracks(playlistId)

    suspend fun getPlaylistMusicTracksSync(playlistId: Long): List<MusicTrack> = dao.getPlaylistMusicTracksSync(playlistId)

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Long {
        val position = dao.getNextPosition(playlistId)
        val playlistTrack = PlaylistTrack(playlistId, trackId, position)
        return dao.addTrackToPlaylist(playlistTrack)
    }

    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>): List<Long> {
        val startPosition = dao.getNextPosition(playlistId)
        val playlistTracks = trackIds.mapIndexed { index, trackId ->
            PlaylistTrack(playlistId, trackId, startPosition + index)
        }
        return dao.addTracksToPlaylist(playlistTracks)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Int = 
        dao.removeTrackFromPlaylist(playlistId, trackId)

    suspend fun clearPlaylist(playlistId: Long): Int = dao.clearPlaylist(playlistId)

    suspend fun updateTrackPosition(playlistId: Long, trackId: Long, newPosition: Int): Int = 
        dao.updateTrackPosition(playlistId, trackId, newPosition)

    suspend fun getPlaylistTrackCount(playlistId: Long): Int = dao.getPlaylistTrackCount(playlistId)

    suspend fun updatePlaylistStats(playlistId: Long, trackCount: Int, totalDuration: Long): Int = 
        dao.updatePlaylistStats(playlistId, trackCount, totalDuration, System.currentTimeMillis())

    suspend fun reorderPlaylistTracks(playlistId: Long, fromPosition: Int, toPosition: Int) {
        val tracks = dao.getPlaylistTracksSync(playlistId).toMutableList()
        if (fromPosition < 0 || fromPosition >= tracks.size || toPosition < 0 || toPosition >= tracks.size) return
        
        val movedTrack = tracks.removeAt(fromPosition)
        tracks.add(toPosition, movedTrack)
        
        // Update positions
        for (i in tracks.indices) {
            dao.updateTrackPosition(playlistId, tracks[i].trackId, i)
        }
    }
}