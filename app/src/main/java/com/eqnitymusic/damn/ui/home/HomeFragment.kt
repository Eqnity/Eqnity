package com.eqnitymusic.damn.ui.home

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
import com.eqnitymusic.damn.ui.main.MainActivity
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.FragmentHomeBinding
import com.eqnitymusic.damn.ui.adapter.DownloadAdapter
import com.eqnitymusic.damn.ui.adapter.MusicAdapter
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val musicAdapter = MusicAdapter(
        onTrackClick = { track -> playTrack(track) },
        onTrackLongClick = { _, _ -> },
        onPlayClick = { track -> playTrack(track) },
        onAddToPlaylist = { track -> showAddToPlaylistDialog(track) },
        onDelete = { track -> confirmDeleteTrack(track) },
        onShare = { track -> shareTrack(track) },
        onInfo = { track -> showTrackInfo(track) }
    )

    private val downloadAdapter = DownloadAdapter(
        onCancel = { download ->
            lifecycleScope.launch { viewModel.cancelDownload(download.id) }
        },
        onRetry = { download -> enqueueDownload(download.sourceUrl, download.id) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvDownloadedMusic.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
        }
        binding.rvActiveDownloads.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = downloadAdapter
        }

        binding.fabDownload.setOnClickListener { showDownloadDialog() }
        binding.btnEmptyStateDownload.setOnClickListener { showDownloadDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tracks.collect { tracks ->
                        musicAdapter.submitList(tracks)
                        val empty = tracks.isEmpty()
                        binding.emptyStateContainer.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.rvDownloadedMusic.visibility = if (empty) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.downloads.collect { downloads ->
                        val active = downloads.filter {
                            it.status == DownloadStatus.PENDING || it.status == DownloadStatus.DOWNLOADING
                        }
                        downloadAdapter.submitList(active)
                        binding.downloadsSection.visibility =
                            if (active.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showDownloadDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDownloadUrl)
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogDownload)
            .setOnClickListener {
                val url = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDownloadUrl)
                    .text.toString().trim()
                if (isValidYoutubeUrl(url)) {
                    dialog.dismiss()
                    enqueueDownload(url, -1L)
                } else {
                    dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDownloadUrl)
                        .error = getString(R.string.download_invalid_url)
                }
            }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogCancel)
            .setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun isValidYoutubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun enqueueDownload(url: String, downloadId: Long) {
        lifecycleScope.launch {
            val id = if (downloadId != -1L) downloadId else viewModel.startDownload(url)
            androidx.work.WorkManager.getInstance(requireContext())
                .enqueueUniqueWork(
                    "download_$id",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    androidx.work.OneTimeWorkRequestBuilder<com.eqnitymusic.damn.worker.DownloadWorker>()
                        .setInputData(
                            androidx.work.Data.Builder()
                                .putLong("download_id", id)
                                .putString("source_url", url)
                                .build()
                        )
                        .build()
                )
            binding.root.showSnackbar(getString(R.string.download_started))
        }
    }

    private fun playTrack(track: MusicTrack) {
        val tracks = viewModel.tracks.value
        val index = tracks.indexOfFirst { it.id == track.id }
        lifecycleScope.launch {
            viewModel.incrementPlayCount(track.id)
        }
        (requireActivity() as MainActivity).playTracks(
            if (index >= 0) tracks else listOf(track),
            if (index >= 0) index else 0
        )
    }

    private fun confirmDeleteTrack(track: MusicTrack) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.menu_delete)
            .setMessage(getString(R.string.playlist_delete_confirm, track.getDisplayTitle()))
            .setPositiveButton(R.string.menu_delete) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteTrack(track)
                    binding.root.showSnackbar(getString(R.string.success_deleted))
                }
            }
            .setNegativeButton(R.string.download_dialog_cancel, null)
            .show()
    }

    private fun showAddToPlaylistDialog(track: MusicTrack) {
        lifecycleScope.launch {
            val playlists = viewModel.playlists.value
            if (playlists.isEmpty()) {
                binding.root.showSnackbar(getString(R.string.playlist_empty))
                return@launch
            }
            val names = playlists.map { it.getDisplayName() }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_to_playlist_title)
                .setItems(names) { _, which ->
                    val playlist = playlists[which]
                    lifecycleScope.launch {
                        viewModel.addTrackToPlaylist(playlist.id, track.id)
                        binding.root.showSnackbar(getString(R.string.success_saved))
                    }
                }
                .setNegativeButton(R.string.download_dialog_cancel, null)
                .show()
        }
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