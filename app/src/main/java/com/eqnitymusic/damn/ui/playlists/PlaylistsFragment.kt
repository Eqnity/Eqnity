package com.eqnitymusic.damn.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.databinding.FragmentPlaylistsBinding
import com.eqnitymusic.damn.ui.adapter.PlaylistAdapter
import com.eqnitymusic.damn.ui.main.MainActivity
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val playlistAdapter = PlaylistAdapter(
        onPlaylistClick = { playlist -> openDetail(playlist) },
        onPlaylistLongClick = { playlist, view -> showMenu(playlist, view) },
        onPlayClick = { playlist -> playPlaylist(playlist) },
        onRename = { playlist -> showRenameDialog(playlist) },
        onDelete = { playlist -> confirmDelete(playlist) },
        onAddSongs = { playlist -> (requireActivity() as MainActivity).openAddSongs(playlist.id) }
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
        binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylists.adapter = playlistAdapter

        binding.fabCreatePlaylist.setOnClickListener { showCreateDialog() }
        binding.btnEmptyStateCreate.setOnClickListener { showCreateDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { playlists ->
                    playlistAdapter.submitList(playlists)
                    binding.playlistEmptyState.visibility =
                        if (playlists.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvPlaylists.visibility =
                        if (playlists.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showMenu(playlist: Playlist, view: View) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.playlist_item_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_play -> playPlaylist(playlist)
                    R.id.menu_add_songs ->
                        (requireActivity() as MainActivity).openAddSongs(playlist.id)
                    R.id.menu_rename -> showRenameDialog(playlist)
                    R.id.menu_delete -> confirmDelete(playlist)
                }
                true
            }
            show()
        }
    }

    private fun openDetail(playlist: Playlist) {
        (requireActivity() as MainActivity).openPlaylistDetail(playlist.id)
    }

    private fun playPlaylist(playlist: Playlist) {
        lifecycleScope.launch {
            val tracks = viewModel.getPlaylistMusicTracks(playlist.id)
            if (tracks.isEmpty()) {
                binding.root.showSnackbar(getString(R.string.playlist_songs_empty))
                return@launch
            }
            (requireActivity() as MainActivity).playTracks(tracks, 0)
        }
    }

    private fun showCreateDialog() {
        showNameDialog(getString(R.string.playlist_create_title), "") { name ->
            lifecycleScope.launch {
                viewModel.createPlaylist(name)
                binding.root.showSnackbar(getString(R.string.success_saved))
            }
        }
    }

    private fun showRenameDialog(playlist: Playlist) {
        showNameDialog(getString(R.string.playlist_rename), playlist.name) { name ->
            lifecycleScope.launch {
                viewModel.renamePlaylist(playlist, name)
                binding.root.showSnackbar(getString(R.string.success_saved))
            }
        }
    }

    private fun showNameDialog(title: String, initial: String, onConfirm: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_playlist, null)
        val til = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPlaylistName)
        val input = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPlaylistName)
        dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle).text = title
        input.setText(initial)
        input.setSelection(initial.length)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogCreate)
            .setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    til.error = getString(R.string.download_invalid_url)
                } else {
                    dialog.dismiss()
                    onConfirm(name)
                }
            }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogCancel)
            .setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmDelete(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.playlist_delete_confirm, playlist.getDisplayName()))
            .setMessage(R.string.playlist_delete_message)
            .setPositiveButton(R.string.playlist_delete) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deletePlaylist(playlist)
                    binding.root.showSnackbar(getString(R.string.success_deleted))
                }
            }
            .setNegativeButton(R.string.download_dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}