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
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.databinding.DialogAddToPlaylistBinding
import com.eqnitymusic.damn.ui.adapter.PlaylistOptionAdapter
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddToPlaylistFragment : Fragment() {

    private var _binding: DialogAddToPlaylistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val args: AddToPlaylistFragmentArgs by navArgs()
    private val adapter = PlaylistOptionAdapter { playlist, isSelected ->
        if (isSelected) {
            selectedPlaylists.add(playlist.id)
        } else {
            selectedPlaylists.remove(playlist.id)
        }
    }
    private val selectedPlaylists = mutableSetOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.rvPlaylistOptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapter
        }
    }

    private fun observeData() {
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitList(playlists)
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateNewPlaylist.setOnClickListener { showCreatePlaylistDialog() }
        binding.btnDialogAdd.setOnClickListener { addToSelectedPlaylists() }
        binding.btnDialogCancel.setOnClickListener { findNavController().navigateUp() }
    }

    private fun showCreatePlaylistDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_playlist, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.btnDialogCreate.setOnClickListener {
            val name = dialogView.etPlaylistName.text.toString().trim()
            if (name.isNotBlank()) {
                dialog.dismiss()
                createPlaylistAndAdd(name)
            } else {
                dialogView.tilPlaylistName.error = "Playlist name cannot be empty"
            }
        }

        dialogView.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun createPlaylistAndAdd(name: String) {
        lifecycleScope.launch {
            val playlistId = viewModel.createPlaylist(name)
            viewModel.addTrackToPlaylist(playlistId, args.trackId)
            selectedPlaylists.add(playlistId)
            adapter.notifyDataSetChanged()
            showSnackbar(binding.root, getString(R.string.success_saved))
        }
    }

    private fun addToSelectedPlaylists() {
        if (selectedPlaylists.isEmpty()) {
            showSnackbar(binding.root, "Select at least one playlist")
            return
        }

        lifecycleScope.launch {
            selectedPlaylists.forEach { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, args.trackId)
            }
            showSnackbar(binding.root, "Added to ${selectedPlaylists.size} playlist(s)")
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}