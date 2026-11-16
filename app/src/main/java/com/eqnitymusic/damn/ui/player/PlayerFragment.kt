package com.eqnitymusic.damn.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.databinding.LayoutFullPlayerBinding
import com.eqnitymusic.damn.service.MediaPlaybackService
import com.eqnitymusic.damn.util.formatDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: LayoutFullPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayerViewModel by viewModels()
    private var progressUpdateJob: kotlinx.coroutines.Job? = null

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
        setupObservers()
        setupClickListeners()
        startProgressUpdates()
    }

    private fun setupObservers() {
        viewModel.currentTrack.observe(viewLifecycleOwner) { track ->
            updateTrackInfo(track)
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            updatePlayPauseButton(isPlaying)
        }

        viewModel.loopMode.observe(viewLifecycleOwner) { mode ->
            updateLoopButton(mode)
        }

        viewModel.shuffleMode.observe(viewLifecycleOwner) { mode ->
            updateShuffleButton(mode)
        }

        viewModel.playlist.observe(viewLifecycleOwner) { playlist ->
            binding.btnFullPlayerQueue.visibility = if (playlist.isNotEmpty()) View.VISIBLE else View.GONE
            binding.btnFullPlayerQueue.text = "${playlist.size} songs in queue"
        }
    }

    private fun setupClickListeners() {
        binding.btnFullPlayerPlayPause.setOnClickListener { viewModel.playPause() }
        binding.btnFullPlayerNext.setOnClickListener { viewModel.playNext() }
        binding.btnFullPlayerPrevious.setOnClickListener { viewModel.playPrevious() }
        binding.btnFullPlayerLoop.setOnClickListener { cycleLoopMode() }
        binding.btnFullPlayerShuffle.setOnClickListener { cycleShuffleMode() }
        binding.btnFullPlayerQueue.setOnClickListener { showQueue() }

        binding.sliderFullPlayerProgress.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                viewModel.seekTo((value * viewModel.duration.value / 100).toLong())
            }
        }

        // Drag to dismiss
        binding.root.setOnTouchListener { _, event ->
            // Handle swipe down to dismiss
            false
        }
    }

    private fun startProgressUpdates() {
        progressUpdateJob = lifecycleScope.launch {
            while (true) {
                delay(500)
                viewModel.playbackService?.let { service ->
                    viewModel.onProgressChanged(
                        service.getProgress(),
                        service.getCurrentPosition(),
                        service.getDuration()
                    )
                }
            }
        }
    }

    private fun updateTrackInfo(track: com.eqnitymusic.damn.data.model.MusicTrack?) {
        track?.let {
            binding.tvFullPlayerTitle.text = it.getDisplayTitle()
            binding.tvFullPlayerArtist.text = it.getDisplayArtist()

            // Load album art
            Glide.with(this)
                .load(it.artworkPath)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .apply(RequestOptions().transform(RoundedCorners(16)))
                .into(binding.ivFullPlayerArt)

            // Also set as background with blur
            Glide.with(this)
                .load(it.artworkPath)
                .placeholder(R.color.player_background)
                .error(R.color.player_background)
                .into(binding.ivFullPlayerBg)

            binding.tvFullPlayerTotalTime.text = formatDuration(it.duration)
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.btnFullPlayerPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateLoopButton(mode: Int) {
        val (icon, tint) = when (mode) {
            MediaPlaybackService.REPEAT_MODE_OFF -> R.drawable.ic_loop to R.color.text_secondary
            MediaPlaybackService.REPEAT_MODE_ONE -> R.drawable.ic_loop_one to R.color.accent_primary
            MediaPlaybackService.REPEAT_MODE_ALL -> R.drawable.ic_loop to R.color.accent_primary
            else -> R.drawable.ic_loop to R.color.text_secondary
        }
        binding.btnFullPlayerLoop.setImageResource(icon)
        binding.btnFullPlayerLoop.setColorFilter(requireContext().getColor(tint))
    }

    private fun updateShuffleButton(mode: Int) {
        val tint = if (mode == MediaPlaybackService.SHUFFLE_MODE_OFF) {
            R.color.text_secondary
        } else {
            R.color.accent_primary
        }
        binding.btnFullPlayerShuffle.setColorFilter(requireContext().getColor(tint))
    }

    private fun cycleLoopMode() {
        val current = viewModel.loopMode.value
        val next = when (current) {
            MediaPlaybackService.REPEAT_MODE_OFF -> MediaPlaybackService.REPEAT_MODE_ALL
            MediaPlaybackService.REPEAT_MODE_ALL -> MediaPlaybackService.REPEAT_MODE_ONE
            MediaPlaybackService.REPEAT_MODE_ONE -> MediaPlaybackService.REPEAT_MODE_OFF
            else -> MediaPlaybackService.REPEAT_MODE_OFF
        }
        viewModel.setLoopMode(next)
    }

    private fun cycleShuffleMode() {
        val current = viewModel.shuffleMode.value
        val next = if (current == MediaPlaybackService.SHUFFLE_MODE_OFF) {
            MediaPlaybackService.SHUFFLE_MODE_ALL
        } else {
            MediaPlaybackService.SHUFFLE_MODE_OFF
        }
        viewModel.setShuffleMode(next)
    }

    private fun showQueue() {
        // Navigate to queue fragment or show bottom sheet
    }

    override fun onDestroyView() {
        progressUpdateJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}