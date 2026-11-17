package com.eqnitymusic.damn.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.LayoutFullPlayerBinding
import com.eqnitymusic.damn.util.formatDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: LayoutFullPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayerViewModel by viewModels()
    private var progressJob: Job? = null
    private var lastTrackId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutFullPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        render()
        startProgressUpdates()
    }

    private fun setupListeners() {
        binding.btnFullPlayerPlayPause.setOnClickListener { viewModel.playPause(); render() }
        binding.btnFullPlayerNext.setOnClickListener { viewModel.next(); render() }
        binding.btnFullPlayerPrevious.setOnClickListener { viewModel.previous(); render() }

        binding.btnFullPlayerLoop.setOnClickListener {
            val next = when (viewModel.getRepeatMode()) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            viewModel.setRepeatMode(next)
            render()
        }

        binding.btnFullPlayerShuffle.setOnClickListener {
            viewModel.setShuffleEnabled(!viewModel.isShuffleEnabled())
            render()
        }

        binding.sliderFullPlayerProgress.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.seekTo(value.toLong())
            }
        }

        binding.btnFullPlayerQueue.setOnClickListener {
            val tracks = viewModel.getPlaylist()
            val message = if (tracks.isEmpty()) {
                getString(R.string.queue_empty)
            } else {
                "${tracks.size} songs in queue"
            }
            binding.btnFullPlayerQueue.text = message
        }
    }

    private fun startProgressUpdates() {
        progressJob = lifecycleScope.launch {
            while (isActive) {
                render()
                delay(500)
            }
        }
    }

    private fun render() {
        val track: MusicTrack? = viewModel.getCurrentTrack()
        if (track != null && track.id != lastTrackId) {
            lastTrackId = track.id
            viewModel.onTrackPlayed(track)
            updateTrackInfo(track)
        }

        val duration = viewModel.getDuration()
        val position = viewModel.getPosition().coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)

        binding.sliderFullPlayerProgress.valueTo =
            if (duration > 0) duration.toFloat().coerceAtLeast(1f) else 100f
        if (!binding.sliderFullPlayerProgress.isPressed) {
            binding.sliderFullPlayerProgress.value = position.toFloat()
        }
        binding.tvFullPlayerCurrentTime.text = formatDuration(position)
        binding.tvFullPlayerTotalTime.text = formatDuration(duration)

        updatePlayPauseButton(viewModel.isPlaying())
        updateLoopButton(viewModel.getRepeatMode())
        updateShuffleButton(viewModel.isShuffleEnabled())

        val tracks = viewModel.getPlaylist()
        binding.btnFullPlayerQueue.visibility = if (tracks.size > 1) View.VISIBLE else View.GONE
        if (tracks.size > 1) {
            binding.btnFullPlayerQueue.text = "${tracks.size} songs in queue"
        }
    }

    private fun updateTrackInfo(track: MusicTrack) {
        binding.tvFullPlayerTitle.text = track.getDisplayTitle()
        binding.tvFullPlayerArtist.text = track.getDisplayArtist()

        Glide.with(this)
            .load(track.artworkPath)
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .apply(RequestOptions().transform(RoundedCorners(16)))
            .into(binding.ivFullPlayerArt)

        Glide.with(this)
            .load(track.artworkPath)
            .placeholder(R.color.player_background)
            .error(R.color.player_background)
            .into(binding.ivFullPlayerBg)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.btnFullPlayerPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateLoopButton(mode: Int) {
        val (icon, colorRes) = when (mode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_loop_one to R.color.accent_primary
            Player.REPEAT_MODE_ALL -> R.drawable.ic_loop to R.color.accent_primary
            else -> R.drawable.ic_loop to R.color.text_secondary
        }
        binding.btnFullPlayerLoop.setImageResource(icon)
        binding.btnFullPlayerLoop.setColorFilter(requireContext().getColor(colorRes))
    }

    private fun updateShuffleButton(enabled: Boolean) {
        val colorRes = if (enabled) R.color.accent_primary else R.color.text_secondary
        binding.btnFullPlayerShuffle.setColorFilter(requireContext().getColor(colorRes))
    }

    override fun onDestroyView() {
        progressJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}