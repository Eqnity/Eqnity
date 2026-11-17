package com.eqnitymusic.damn.ui.player

import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.repository.MusicRepository
import com.eqnitymusic.damn.service.MediaPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun service(): MediaPlaybackService? = MediaPlaybackService.getInstance()

    fun getCurrentTrack(): MusicTrack? = service()?.currentTrack

    fun isPlaying(): Boolean = service()?.isPlaying() == true

    fun getPosition(): Long = service()?.getCurrentPosition() ?: 0L

    fun getDuration(): Long = service()?.getDuration() ?: 0L

    fun getPlaylist(): List<MusicTrack> = service()?.playlist ?: emptyList()

    fun playPause() = service()?.playPause() ?: Unit

    fun next() = service()?.next() ?: Unit

    fun previous() = service()?.previous() ?: Unit

    fun seekTo(positionMs: Long) = service()?.seekTo(positionMs) ?: Unit

    fun getRepeatMode(): Int = service()?.getRepeatMode() ?: Player.REPEAT_MODE_OFF

    fun setRepeatMode(mode: Int) {
        service()?.setRepeatMode(mode)
    }

    fun isShuffleEnabled(): Boolean = service()?.isShuffleEnabled() == true

    fun setShuffleEnabled(enabled: Boolean) {
        service()?.setShuffleEnabled(enabled)
    }

    fun onTrackPlayed(track: MusicTrack) {
        scope.launch {
            musicRepository.incrementPlayCount(track.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}