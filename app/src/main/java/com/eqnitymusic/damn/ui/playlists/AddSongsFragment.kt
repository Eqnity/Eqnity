package com.eqnitymusic.damn.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.FragmentAddSongsBinding
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddSongsFragment : Fragment() {

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: Long): AddSongsFragment {
            return AddSongsFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PLAYLIST_ID, playlistId) }
            }
        }
    }

    private var _binding: FragmentAddSongsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private val playlistId: Long by lazy {
        requireArguments().getLong(ARG_PLAYLIST_ID, -1L)
    }

    private val selected = mutableSetOf<Long>()
    private val adapter = TrackPickAdapter(
        isSelected = { selected.contains(it) },
        onToggle = { trackId ->
            if (!selected.add(trackId)) selected.remove(trackId)
            binding.btnAddSelected.text =
                if (selected.isEmpty()) {
                    getString(R.string.playlist_add_songs)
                } else {
                    "Add ${selected.size} song(s)"
                }
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAllSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllSongs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tracks.collect { tracks ->
                        adapter.submitList(tracks)
                        binding.emptyStateContainer.visibility =
                            if (tracks.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAllSongs.visibility =
                            if (tracks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.getPlaylistTracks(playlistId).collect { existing ->
                        selected.clear()
                        selected.addAll(existing.map { it.id })
                        adapter.notifySelectionChanged()
                    }
                }
            }
        }

        binding.btnAddSelected.setOnClickListener {
            val toAdd = selected.toList()
            if (toAdd.isEmpty()) {
                parentFragmentManager.popBackStack()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val toActuallyAdd = toAdd.filter { trackId ->
                    viewModel.getPlaylistMusicTracks(playlistId).none { it.id == trackId }
                }
                if (toActuallyAdd.isEmpty()) {
                    showSnackbar(binding.root, getString(R.string.success_saved))
                } else {
                    viewModel.addTrackToPlaylist(playlistId, toActuallyAdd.first())
                    toActuallyAdd.drop(1).forEach { trackId ->
                        viewModel.addTrackToPlaylist(playlistId, trackId)
                    }
                    showSnackbar(binding.root, getString(R.string.success_saved))
                }
                parentFragmentManager.popBackStack()
            }
        }

        binding.btnCancelSelection.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}