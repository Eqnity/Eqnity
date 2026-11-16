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
import com.eqnitymusic.damn.databinding.FragmentPlaylistDetailBinding
import com.eqnitymusic.damn.ui.adapter.MusicAdapter
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val args: PlaylistDetailFragmentArgs by navArgs()
    private val musicAdapter = MusicAdapter(
        onTrackClick = { track -> playTrack(track) },
        onTrackLongClick = { track, view -> showTrackOptions(track, view) },
        onPlayClick = { track -> playTrack(track) },
        onAddToPlaylist = { track -> showAddToPlaylistDialog(track) },
        onDelete = { track -> confirmRemoveFromPlaylist(track) },
        onShare = { track -> shareTrack(track) },
        onInfo = { track -> showTrackInfo(track) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupToolbar()
    }

    private fun setupRecyclerView() {
        binding.rvPlaylistSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
        }
    }

    private fun observeData() {
        viewModel.getPlaylistTracks(args.playlistId).observe(viewLifecycleOwner) { tracks ->
            musicAdapter.submitList(tracks)
            binding.emptyStateContainer.visibility = if (tracks.isEmpty()) View.VISIBLE else View.GONE
            binding.rvPlaylistSongs.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_add_songs -> {
                    showAddSongsDialog()
                    true
                }
                R.id.menu_rename -> {
                    // Would need playlist object
                    true
                }
                R.id.menu_delete -> {
                    // Would need playlist object
                    true
                }
                else -> false
            }
        }
    }

    private fun playTrack(track: MusicTrack) {
        viewModel.incrementPlayCount(track)
        val activity = requireActivity() as MainActivity
        val allTracks = musicAdapter.currentList
        activity.playTracks(allTracks, allTracks.indexOf(track))
    }

    private fun showTrackOptions(track: MusicTrack, view: View) {
        // Handled by adapter
    }

    private fun showAddToPlaylistDialog(track: MusicTrack) {
        val navController = findNavController()
        navController.navigate(PlaylistDetailFragmentDirections.actionPlaylistDetailToAddToPlaylist(track.id))
    }

    private fun confirmRemoveFromPlaylist(track: MusicTrack) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.menu_remove_from_playlist)
            .setMessage(getString(R.string.playlist_delete_confirm, track.getDisplayTitle()))
            .setPositiveButton(R.string.playlist_delete) { _, _ ->
                viewModel.removeTrackFromPlaylist(args.playlistId, track.id)
                showSnackbar(binding.root, getString(R.string.success_deleted))
            }
            .setNegativeButton(R.string.download_dialog_cancel, null)
            .show()
    }

    private fun shareTrack(track: MusicTrack) {
        val intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                java.io.File(track.filePath)
            ))
            type = "audio/mpeg"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.menu_share)))
    }

    private fun showTrackInfo(track: MusicTrack) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.file_info_title)
            .setMessage(
                "${getString(R.string.file_info_name)}: ${track.getDisplayTitle()}\n" +
                "${getString(R.string.file_info_artist)}: ${track.getDisplayArtist()}\n" +
                "${getString(R.string.file_info_path)}: ${track.filePath}\n" +
                "${getString(R.string.file_info_size)}: ${track.getFormattedFileSize()}\n" +
                "${getString(R.string.file_info_duration)}: ${track.getFormattedDuration()}\n" +
                "${getString(R.string.file_info_format)}: ${track.mimeType}\n" +
                "${getString(R.string.file_info_date_added)}: ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(track.dateAdded))}\n" +
                "${getString(R.string.file_info_source)}: ${track.sourceUrl}"
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showAddSongsDialog() {
        findNavController().navigate(PlaylistDetailFragmentDirections.actionPlaylistDetailToAddSongs(args.playlistId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}