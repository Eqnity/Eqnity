package com.eqnitymusic.damn.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.FragmentAddSongsToPlaylistBinding
import com.eqnitymusic.damn.ui.adapter.MusicAdapter
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddSongsToPlaylistFragment : Fragment() {

    private var _binding: FragmentAddSongsToPlaylistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val args: AddSongsToPlaylistFragmentArgs by navArgs()
    private val adapter = MusicAdapter(
        onTrackClick = { track -> toggleTrackSelection(track) },
        onTrackLongClick = { _, _ -> },
        onPlayClick = { track -> toggleTrackSelection(track) },
        onAddToPlaylist = { _ -> },
        onDelete = { _ -> },
        onShare = { _ -> },
        onInfo = { _ -> }
    )
    private val selectedTracks = mutableSetOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSongsToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.rvAvailableSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapter
        }
    }

    private fun observeData() {
        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            // Filter out tracks already in playlist
            lifecycleScope.launch {
                val playlistTracks = viewModel.getPlaylistMusicTracks(args.playlistId).firstOrNull() ?: emptyList()
                val existingIds = playlistTracks.map { it.id }.toSet()
                val availableTracks = tracks.filter { it.id !in existingIds }
                adapter.submitList(availableTracks)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAddSelected.setOnClickListener { addSelectedTracks() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
    }

    private fun toggleTrackSelection(track: MusicTrack) {
        if (selectedTracks.contains(track.id)) {
            selectedTracks.remove(track.id)
        } else {
            selectedTracks.add(track.id)
        }
        updateSelectionUI()
    }

    private fun updateSelectionUI() {
        binding.tvSelectedCount.text = "${selectedTracks.size} selected"
        binding.btnAddSelected.isEnabled = selectedTracks.isNotEmpty()
        adapter.notifyDataSetChanged()
    }

    private fun addSelectedTracks() {
        if (selectedTracks.isEmpty()) {
            showSnackbar(binding.root, "Select at least one song")
            return
        }

        lifecycleScope.launch {
            viewModel.addTracksToPlaylist(args.playlistId, selectedTracks.toList())
            showSnackbar(binding.root, "${selectedTracks.size} song(s) added to playlist")
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}