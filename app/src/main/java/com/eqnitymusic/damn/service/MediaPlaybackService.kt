package com.eqnitymusic.damn.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.eqnitymusic.damn.data.model.MusicTrack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {

    @Inject
    lateinit var musicRepository: com.eqnitymusic.damn.data.repository.MusicRepository

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    var playlist: List<MusicTrack> = emptyList()
        private set
    var currentIndex: Int = -1
        private set
    var currentTrack: MusicTrack? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        initialize()
        pendingPlayback?.let { request ->
            pendingPlayback = null
            setPlaylist(request.tracks, request.startIndex)
        }
    }

    private fun initialize() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
            .build()

        exoPlayer?.addListener(playerListener)

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setId("EqnityMusicSession")
            .build()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                next()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem != null) {
                val index = playlist.indexOfFirst { it.id.toString() == mediaItem.mediaId }
                if (index != -1) {
                    currentIndex = index
                    currentTrack = playlist[index]
                }
            }
        }
    }

    private fun buildMediaItem(track: MusicTrack): MediaItem {
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.filePath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.getDisplayTitle())
                    .setArtist(track.getDisplayArtist())
                    .build()
            )
            .build()
    }

    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        playlist = tracks
        currentIndex = startIndex.coerceIn(tracks.indices)
        currentTrack = tracks[currentIndex]
        val mediaItems = tracks.map { buildMediaItem(it) }
        exoPlayer?.setMediaItems(mediaItems, currentIndex, 0L)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true
    }

    fun playPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun play() = exoPlayer?.play() ?: Unit

    fun pause() = exoPlayer?.pause() ?: Unit

    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    fun next() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex < playlist.size - 1) currentIndex + 1 else 0
        currentTrack = playlist[currentIndex]
        exoPlayer?.seekTo(currentIndex, 0L)
        exoPlayer?.play()
    }

    fun previous() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        currentTrack = playlist[currentIndex]
        exoPlayer?.seekTo(currentIndex, 0L)
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    fun getDuration(): Long {
        val duration = exoPlayer?.duration ?: 0L
        return if (duration == androidx.media3.common.C.TIME_UNSET) 0L else duration
    }

    fun getRepeatMode(): Int = exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF

    fun setRepeatMode(mode: Int) {
        exoPlayer?.repeatMode = mode
    }

    fun isShuffleEnabled(): Boolean = exoPlayer?.shuffleModeEnabled == true

    fun setShuffleEnabled(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
    }

    fun stopPlayback() {
        exoPlayer?.stop()
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        exoPlayer?.release()
        mediaSession = null
        exoPlayer = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private var instance: MediaPlaybackService? = null
        private var pendingPlayback: PendingPlayback? = null

        private data class PendingPlayback(
            val tracks: List<MusicTrack>,
            val startIndex: Int
        )

        fun getInstance(): MediaPlaybackService? = instance

        fun playTracks(context: Context, tracks: List<MusicTrack>, startIndex: Int = 0) {
            if (tracks.isEmpty()) return
            val intent = Intent(context, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
            val service = instance
            if (service != null) {
                service.setPlaylist(tracks, startIndex)
            } else {
                pendingPlayback = PendingPlayback(tracks, startIndex)
            }
        }
    }
}