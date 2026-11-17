package com.eqnitymusic.damn.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.data.repository.DownloadRepository
import com.eqnitymusic.damn.data.repository.MusicRepository
import com.eqnitymusic.damn.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads

    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks: StateFlow<List<MusicTrack>> = _tracks

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _playlistTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val playlistTracks: StateFlow<List<MusicTrack>> = _playlistTracks

    init {
        downloadRepository.getAllDownloads()
            .onEach { _downloads.value = it }
            .launchIn(viewModelScope)

        musicRepository.getAllTracks()
            .onEach { _tracks.value = it }
            .launchIn(viewModelScope)

        playlistRepository.getAllPlaylists()
            .onEach { _playlists.value = it }
            .launchIn(viewModelScope)
    }

    suspend fun startDownload(sourceUrl: String): Long {
        val existing = downloadRepository.getDownloadBySourceUrl(sourceUrl).firstOrNull()
        if (existing != null) {
            downloadRepository.updateDownload(
                existing.copy(
                    status = DownloadStatus.PENDING,
                    progress = 0,
                    errorMessage = "",
                    filePath = "",
                    trackId = 0
                )
            )
            return existing.id
        }
        return downloadRepository.insertDownload(
            Download(
                sourceUrl = sourceUrl,
                status = DownloadStatus.PENDING
            )
        )
    }

    suspend fun cancelDownload(downloadId: Long) {
        downloadRepository.getDownloadByIdSync(downloadId)?.let {
            downloadRepository.updateDownload(it.copy(status = DownloadStatus.CANCELLED))
        }
    }

    suspend fun deleteTrack(track: MusicTrack) = musicRepository.deleteTrack(track)

    suspend fun incrementPlayCount(trackId: Long) = musicRepository.incrementPlayCount(trackId)

    suspend fun createPlaylist(name: String): Long =
        playlistRepository.insertPlaylist(Playlist(name = name))

    suspend fun renamePlaylist(playlist: Playlist, newName: String) =
        playlistRepository.updatePlaylist(
            playlist.copy(name = newName, dateModified = System.currentTimeMillis())
        )

    suspend fun deletePlaylist(playlist: Playlist) = playlistRepository.deletePlaylist(playlist)

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) =
        playlistRepository.addTrackToPlaylist(playlistId, trackId)

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) =
        playlistRepository.removeTrackFromPlaylist(playlistId, trackId)

    fun getPlaylistTracks(playlistId: Long) = playlistRepository.getPlaylistMusicTracks(playlistId)

    suspend fun getPlaylistById(playlistId: Long): Playlist? =
        playlistRepository.getPlaylistByIdSync(playlistId)

    suspend fun getPlaylistMusicTracks(playlistId: Long): List<MusicTrack> =
        playlistRepository.getPlaylistMusicTracksSync(playlistId)
}