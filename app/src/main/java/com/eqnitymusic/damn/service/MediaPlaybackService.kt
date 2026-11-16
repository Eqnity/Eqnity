package com.eqnitymusic.damn.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.PowerManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommandGroup
import androidx.media3.session.MediaController
import androidx.media3.common.MediaLibraryInfo
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.repository.MusicRepository
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures

@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {

    @Inject
    lateinit var musicRepository: MusicRepository

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentTrack: MusicTrack? = null
    private var playlist: List<MusicTrack> = emptyList()
    private var currentIndex = -1
    private var loopMode = Player.REPEAT_MODE_OFF
    private var shuffleMode = Player.SHUFFLE_MODE_OFF
    private var shuffledIndices: List<Int> = emptyList()

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.AUDIO_USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer?.addListener(playerListener)
        
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
        
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionCallback(sessionCallback)
            .setMediaButtonReceiver(null)
            .setId("EqnityMusicSession")
            .build()

        // Keep screen on during playback
        exoPlayer?.setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updateNotification()
        }

        override fun onPlayerError(error: PlaybackException) {
            // Handle error - skip to next
            playNext()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem != null) {
                val index = findTrackIndex(mediaItem.mediaId)
                if (index != -1) {
                    currentIndex = index
                    currentTrack = playlist[index]
                    updateNotification()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateNotification()
        }
    }

    private val sessionCallback = object : MediaSession.Callback() {
        override fun onConnect(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.acceptedResult
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            playlist = mediaItems.map { item ->
                val trackId = item.mediaId.toLongOrNull() ?: 0L
                musicRepository.getTrackByIdSync(trackId) ?: MusicTrack(
                    id = trackId,
                    title = item.mediaMetadata?.title?.toString() ?: "Unknown",
                    artist = item.mediaMetadata?.artist?.toString() ?: "Unknown",
                    filePath = item.localConfiguration?.uri?.toString() ?: "",
                    sourceUrl = "",
                    duration = (item.mediaMetadata?.duration ?: 0).toLong()
                )
            }
            currentIndex = 0
            currentTrack = playlist.firstOrNull()
            preparePlaylist()
            return Futures.immediateFuture(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPosition: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val result = onAddMediaItems(mediaSession, controllerInfo, mediaItems)
            return Futures.transform(result, MoreExecutors.directExecutor()) { items ->
                MediaSession.MediaItemsWithStartPosition(items, startIndex, startPosition)
            }
        }

        override fun onPlay(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.Result> {
            exoPlayer?.playWhenReady = true
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun onPause(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.Result> {
            exoPlayer?.pause()
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun onSeekTo(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            position: Long
        ): ListenableFuture<MediaSession.Result> {
            exoPlayer?.seekTo(position)
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun onSkipToNext(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.Result> {
            playNext()
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun onSkipToPrevious(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.Result> {
            playPrevious()
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun onSetRepeatMode(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            repeatMode: Int
        ): ListenableFuture<MediaSession.Result> {
            loopMode = repeatMode
            exoPlayer?.repeatMode = repeatMode
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun onSetShuffleMode(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            shuffleMode: Int
        ): ListenableFuture<MediaSession.Result> {
            this@MediaPlaybackService.shuffleMode = shuffleMode
            exoPlayer?.shuffleMode = shuffleMode
            if (shuffleMode != Player.SHUFFLE_MODE_OFF) {
                generateShuffledIndices()
            }
            return Futures.immediateFuture(MediaSession.Result.acceptedResult)
        }

        override fun getAvailableCommands(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): SessionCommandGroup {
            return SessionCommandGroup(
                SessionCommand.SESSION_COMMAND_PLAY,
                SessionCommand.SESSION_COMMAND_PAUSE,
                SessionCommand.SESSION_COMMAND_SEEK_TO,
                SessionCommand.SESSION_COMMAND_SKIP_TO_NEXT,
                SessionCommand.SESSION_COMMAND_SKIP_TO_PREVIOUS,
                SessionCommand.SESSION_COMMAND_SET_REPEAT_MODE,
                SessionCommand.SESSION_COMMAND_SET_SHUFFLE_MODE,
                SessionCommand.SESSION_COMMAND_GET_MEDIA_ITEMS
            )
        }
    }

    private fun preparePlaylist() {
        val mediaItems = playlist.map { track ->
            MediaItem.fromUri(track.filePath)
                .copy(
                    mediaId = track.id.toString(),
                    mediaMetadata = MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(Uri.parse(track.artworkPath))
                        .setDuration(track.duration)
                        .build()
                )
                .build()
        }

        exoPlayer?.setMediaItems(mediaItems)
        exoPlayer?.prepare()
        exoPlayer?.repeatMode = loopMode
        exoPlayer?.shuffleMode = shuffleMode
    }

    private fun findTrackIndex(mediaId: String): Int {
        return playlist.indexOfFirst { it.id.toString() == mediaId }
    }

    private fun playNext() {
        if (playlist.isEmpty()) return
        
        val nextIndex = when {
            shuffleMode != Player.SHUFFLE_MODE_OFF -> getNextShuffledIndex()
            loopMode == Player.REPEAT_MODE_ONE -> currentIndex
            loopMode == Player.REPEAT_MODE_ALL -> (currentIndex + 1) % playlist.size
            else -> {
                val next = currentIndex + 1
                if (next >= playlist.size) -1 else next
            }
        }
        
        if (nextIndex != -1 && nextIndex < playlist.size) {
            currentIndex = nextIndex
            exoPlayer?.seekToNext()
        }
    }

    private fun playPrevious() {
        if (playlist.isEmpty()) return
        
        val prevIndex = when {
            shuffleMode != Player.SHUFFLE_MODE_OFF -> getPreviousShuffledIndex()
            loopMode == Player.REPEAT_MODE_ONE -> currentIndex
            loopMode == Player.REPEAT_MODE_ALL -> (currentIndex - 1 + playlist.size) % playlist.size
            else -> {
                val prev = currentIndex - 1
                if (prev < 0) -1 else prev
            }
        }
        
        if (prevIndex != -1 && prevIndex < playlist.size) {
            currentIndex = prevIndex
            exoPlayer?.seekToPrevious()
        }
    }

    private fun generateShuffledIndices() {
        val indices = (0 until playlist.size).toMutableList()
        indices.shuffle()
        shuffledIndices = indices
    }

    private fun getNextShuffledIndex(): Int {
        val currentPos = shuffledIndices.indexOf(currentIndex)
        val nextPos = (currentPos + 1) % shuffledIndices.size
        return shuffledIndices[nextPos]
    }

    private fun getPreviousShuffledIndex(): Int {
        val currentPos = shuffledIndices.indexOf(currentIndex)
        val prevPos = (currentPos - 1 + shuffledIndices.size) % shuffledIndices.size
        return shuffledIndices[prevPos]
    }

    private fun updateNotification() {
        // Media3 handles notification automatically via MediaSession
    }

    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int = 0) {
        playlist = tracks
        currentIndex = startIndex
        currentTrack = if (tracks.isNotEmpty()) tracks[startIndex] else null
        preparePlaylist()
        exoPlayer?.seekTo(startIndex, 0)
        exoPlayer?.playWhenReady = true
    }

    fun getCurrentTrack(): MusicTrack? = currentTrack
    fun getPlaylist(): List<MusicTrack> = playlist
    fun getCurrentIndex(): Int = currentIndex
    fun isPlaying(): Boolean = exoPlayer?.playWhenReady == true && exoPlayer?.playbackState == Player.STATE_READY
    fun getProgress(): Float = exoPlayer?.currentPosition?.let { 
        (it.toFloat() / exoPlayer?.duration!!) * 100 
    } ?: 0f
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0
    fun getDuration(): Long = exoPlayer?.duration ?: 0
    fun getLoopMode(): Int = loopMode
    fun getShuffleMode(): Int = shuffleMode

    fun playPause() {
        exoPlayer?.playWhenReady = !(exoPlayer?.playWhenReady == true)
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    override fun onDestroy() {
        exoPlayer?.release()
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaController.ControllerInfo): MediaSession? {
        return mediaSession
    }

    companion object {
        fun createIntent(context: Context, tracks: List<MusicTrack>, startIndex: Int): Intent {
            val intent = Intent(context, MediaPlaybackService::class.java)
            intent.putParcelableArrayListExtra("tracks", ArrayList(tracks))
            intent.putExtra("start_index", startIndex)
            return intent
        }
    }
}