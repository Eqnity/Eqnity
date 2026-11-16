package com.eqnitymusic.damn.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.data.repository.MusicRepository
import com.eqnitymusic.damn.data.repository.PlaylistRepository
import com.eqnitymusic.damn.data.repository.DownloadRepository
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    // Downloads
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads

    // Music Tracks
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks: StateFlow<List<MusicTrack>> = _tracks

    // Playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Observe downloads
                downloadRepository.getAllDownloads().onEach { downloads ->
                    _downloads.value = downloads
                }.launchIn(viewModelScope)

                // Observe tracks
                musicRepository.getAllTracks().onEach { tracks ->
                    _tracks.value = tracks
                }.launchIn(viewModelScope)

                // Observe playlists
                playlistRepository.getAllPlaylists().onEach { playlists ->
                    _playlists.value = playlists
                }.launchIn(viewModelScope)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Download operations
    suspend fun startDownload(sourceUrl: String): Long {
        val existingDownload = downloadRepository.getDownloadBySourceUrl(sourceUrl)
            .firstOrNull()
        
        if (existingDownload != null) {
            // Update existing download
            val updated = existingDownload.copy(
                status = DownloadStatus.PENDING,
                progress = 0,
                errorMessage = ""
            )
            downloadRepository.updateDownload(updated)
            return existingDownload.id
        }

        // Create new download
        val download = Download(
            sourceUrl = sourceUrl,
            status = DownloadStatus.PENDING
        )
        return downloadRepository.insertDownload(download)
    }

    suspend fun cancelDownload(downloadId: Long) {
        downloadRepository.getDownloadByIdSync(downloadId)?.let { download ->
            val cancelled = download.copy(status = DownloadStatus.CANCELLED)
            downloadRepository.updateDownload(cancelled)
        }
    }

    // Music operations
    suspend fun deleteTrack(track: MusicTrack) {
        musicRepository.deleteTrack(track)
    }

    suspend fun incrementPlayCount(track: MusicTrack) {
        musicRepository.incrementPlayCount(track.id)
    }

    // Playlist operations
    suspend fun createPlaylist(name: String): Long {
        val playlist = Playlist(name = name)
        return playlistRepository.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistRepository.deletePlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistRepository.updatePlaylist(playlist)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        playlistRepository.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        playlistRepository.addTracksToPlaylist(playlistId, trackIds)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getPlaylistTracks(playlistId: Long) = playlistRepository.getPlaylistMusicTracks(playlistId)

    fun searchTracks(query: String) = musicRepository.searchTracks(query)

    fun searchPlaylists(query: String) = playlistRepository.searchPlaylists(query)
}