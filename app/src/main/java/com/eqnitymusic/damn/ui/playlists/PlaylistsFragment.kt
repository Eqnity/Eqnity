package com.eqnitymusic.damn.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.databinding.FragmentPlaylistsBinding
import com.eqnitymusic.damn.ui.adapter.PlaylistAdapter
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val playlistAdapter = PlaylistAdapter(
        onPlaylistClick = { playlist -> openPlaylist(playlist) },
        onPlaylistLongClick = { playlist, view -> showPlaylistOptions(playlist, view) },
        onPlayClick = { playlist -> playPlaylist(playlist) },
        onRename = { playlist -> showRenameDialog(playlist) },
        onDelete = { playlist -> confirmDeletePlaylist(playlist) },
        onAddSongs = { playlist -> showAddSongsDialog(playlist) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.rvPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playlistAdapter
        }
    }

    private fun observeData() {
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            playlistAdapter.submitList(playlists)
            binding.playlistEmptyState.visibility = if (playlists.isEmpty()) View.VISIBLE else View.GONE
            binding.rvPlaylists.visibility = if (playlists.isEmpty()) View.GONE else View.VISIBLE
            binding.fabCreatePlaylistMain.visibility = if (playlists.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.fabCreatePlaylist.setOnClickListener { showCreatePlaylistDialog() }
        binding.fabCreatePlaylistMain.setOnClickListener { showCreatePlaylistDialog() }
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
                createPlaylist(name)
            } else {
                dialogView.tilPlaylistName.error = "Playlist name cannot be empty"
            }
        }

        dialogView.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun createPlaylist(name: String) {
        lifecycleScope.launch {
            val playlistId = viewModel.createPlaylist(name)
            showSnackbar(binding.root, getString(R.string.success_saved))
        }
    }

    private fun openPlaylist(playlist: Playlist) {
        val navController = findNavController()
        navController.navigate(PlaylistsFragmentDirections.actionPlaylistsToPlaylistDetail(playlist.id))
    }

    private fun playPlaylist(playlist: Playlist) {
        lifecycleScope.launch {
            val tracks = viewModel.getPlaylistMusicTracks(playlist.id).firstOrNull() ?: emptyList()
            if (tracks.isNotEmpty()) {
                val activity = requireActivity() as MainActivity
                activity.playTracks(tracks, 0)
            }
        }
    }

    private fun showPlaylistOptions(playlist: Playlist, view: View) {
        // Handled by adapter's popup menu
    }

    private fun showRenameDialog(playlist: Playlist) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_playlist, null)
        dialogView.tvDialogTitle.text = getString(R.string.playlist_rename)
        dialogView.etPlaylistName.setText(playlist.name)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.btnDialogCreate.text = getString(R.string.playlist_rename)
        dialogView.btnDialogCreate.setOnClickListener {
            val name = dialogView.etPlaylistName.text.toString().trim()
            if (name.isNotBlank()) {
                dialog.dismiss()
                renamePlaylist(playlist, name)
            } else {
                dialogView.tilPlaylistName.error = "Playlist name cannot be empty"
            }
        }

        dialogView.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun renamePlaylist(playlist: Playlist, newName: String) {
        lifecycleScope.launch {
            val updated = playlist.copy(name = newName, dateModified = System.currentTimeMillis())
            viewModel.updatePlaylist(updated)
            showSnackbar(binding.root, getString(R.string.success_saved))
        }
    }

    private fun confirmDeletePlaylist(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_delete_confirm)
            .setMessage(getString(R.string.playlist_delete_message, playlist.name))
            .setPositiveButton(R.string.playlist_delete) { _, _ ->
                viewModel.deletePlaylist(playlist)
                showSnackbar(binding.root, getString(R.string.success_deleted))
            }
            .setNegativeButton(R.string.download_dialog_cancel, null)
            .show()
    }

    private fun showAddSongsDialog(playlist: Playlist) {
        val navController = findNavController()
        navController.navigate(PlaylistsFragmentDirections.actionPlaylistsToAddSongs(playlist.id))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}