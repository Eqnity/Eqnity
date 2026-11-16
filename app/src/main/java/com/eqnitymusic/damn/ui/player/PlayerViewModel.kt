package com.eqnitymusic.damn.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.repository.MusicRepository
import com.eqnitymusic.damn.service.MediaPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _loopMode = MutableStateFlow(MediaPlaybackService.REPEAT_MODE_OFF)
    val loopMode: StateFlow<Int> = _loopMode

    private val _shuffleMode = MutableStateFlow(MediaPlaybackService.SHUFFLE_MODE_OFF)
    val shuffleMode: StateFlow<Int> = _shuffleMode

    private val _playlist = MutableStateFlow<List<MusicTrack>>(emptyList())
    val playlist: StateFlow<List<MusicTrack>> = _playlist

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex

    // MediaPlaybackService reference (set by MainActivity)
    var playbackService: MediaPlaybackService? = null
        set(value) {
            field = value
            if (value != null) {
                syncWithService()
            }
        }

    private fun syncWithService() {
        playbackService?.let { service ->
            _currentTrack.value = service.getCurrentTrack()
            _isPlaying.value = service.isPlaying()
            _progress.value = service.getProgress()
            _currentPosition.value = service.getCurrentPosition()
            _duration.value = service.getDuration()
            _loopMode.value = service.getLoopMode()
            _shuffleMode.value = service.getShuffleMode()
            _playlist.value = service.getPlaylist()
            _currentIndex.value = service.getCurrentIndex()
        }
    }

    fun playPause() {
        playbackService?.playPause()
        _isPlaying.value = playbackService?.isPlaying() == true
    }

    fun playNext() {
        playbackService?.playNext()
    }

    fun playPrevious() {
        playbackService?.playPrevious()
    }

    fun seekTo(position: Long) {
        playbackService?.seekTo(position)
    }

    fun setLoopMode(mode: Int) {
        playbackService?.exoPlayer?.repeatMode = mode
        _loopMode.value = mode
    }

    fun setShuffleMode(mode: Int) {
        playbackService?.exoPlayer?.shuffleMode = mode
        _shuffleMode.value = mode
    }

    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int = 0) {
        _playlist.value = tracks
        playbackService?.setPlaylist(tracks, startIndex)
    }

    // Called from MediaPlaybackService callbacks
    fun onTrackChanged(track: MusicTrack?) {
        _currentTrack.value = track
        if (track != null) {
            musicRepository.incrementPlayCount(track.id)
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun onProgressChanged(progress: Float, currentPos: Long, dur: Long) {
        _progress.value = progress
        _currentPosition.value = currentPos
        _duration.value = dur
    }

    fun onPlaylistChanged(tracks: List<MusicTrack>, index: Int) {
        _playlist.value = tracks
        _currentIndex.value = index
    }
}