package com.eqnitymusic.damn.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.databinding.FragmentPlaylistDetailBinding
import com.eqnitymusic.damn.ui.adapter.MusicAdapter
import com.eqnitymusic.damn.ui.main.MainActivity
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PlaylistDetailFragment : Fragment() {

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: Long): PlaylistDetailFragment {
            return PlaylistDetailFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PLAYLIST_ID, playlistId) }
            }
        }
    }

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private val playlistId: Long by lazy {
        requireArguments().getLong(ARG_PLAYLIST_ID, -1L)
    }

    private var playlist: Playlist? = null

    private val musicAdapter = MusicAdapter(
        onTrackClick = { track -> playTrack(track) },
        onTrackLongClick = { _, _ -> },
        onPlayClick = { track -> playTrack(track) },
        onAddToPlaylist = { track -> showTrackInfo(track) },
        onDelete = { track -> confirmRemove(track) },
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
        binding.rvPlaylistSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylistSongs.adapter = musicAdapter
        binding.btnAddSongs.setOnClickListener {
            (requireActivity() as MainActivity).openAddSongs(playlistId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getPlaylistTracks(playlistId).collect { tracks ->
                        musicAdapter.submitList(tracks)
                        binding.emptyStateContainer.visibility =
                            if (tracks.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvPlaylistSongs.visibility =
                            if (tracks.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    playlist = viewModel.getPlaylistById(playlistId)
                    playlist?.let {
                        binding.tvPlaylistDetailTitle.text = it.getDisplayName()
                    }
                }
            }
        }
    }

    private fun playTrack(track: MusicTrack) {
        val tracks = musicAdapter.currentList
        val index = tracks.indexOfFirst { it.id == track.id }
        lifecycleScope.launch { viewModel.incrementPlayCount(track.id) }
        (requireActivity() as MainActivity).playTracks(
            tracks,
            if (index >= 0) index else 0
        )
    }

    private fun confirmRemove(track: MusicTrack) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.menu_remove_from_playlist)
            .setMessage(getString(R.string.playlist_delete_confirm, track.getDisplayTitle()))
            .setPositiveButton(R.string.menu_delete) { _, _ ->
                lifecycleScope.launch {
                    viewModel.removeTrackFromPlaylist(playlistId, track.id)
                    showSnackbar(binding.root, getString(R.string.success_deleted))
                }
            }
            .setNegativeButton(R.string.download_dialog_cancel, null)
            .show()
    }

    private fun shareTrack(track: MusicTrack) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            File(track.filePath)
        )
        val intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            type = "audio/*"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.menu_share)))
    }

    private fun showTrackInfo(track: MusicTrack) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.file_info_title)
            .setMessage(
                "${getString(R.string.file_info_name)}: ${track.getDisplayTitle()}\n" +
                    "${getString(R.string.file_info_artist)}: ${track.getDisplayArtist()}\n" +
                    "${getString(R.string.file_info_duration)}: ${track.getFormattedDuration()}\n" +
                    "${getString(R.string.file_info_size)}: ${track.getFormattedFileSize()}\n" +
                    "${getString(R.string.file_info_format)}: ${track.mimeType}\n" +
                    "${getString(R.string.file_info_date_added)}: ${dateFormat.format(Date(track.dateAdded))}\n" +
                    "${getString(R.string.file_info_source)}: ${track.sourceUrl}"
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}