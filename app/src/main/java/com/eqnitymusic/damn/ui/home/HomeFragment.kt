package com.eqnitymusic.damn.ui.home

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
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.FragmentHomeBinding
import com.eqnitymusic.damn.ui.adapter.MusicAdapter
import com.eqnitymusic.damn.ui.adapter.DownloadAdapter
import com.eqnitymusic.damn.ui.main.MainViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val musicAdapter = MusicAdapter(
        onTrackClick = { track -> playTrack(track) },
        onTrackLongClick = { track, view -> showTrackOptions(track, view) },
        onPlayClick = { track -> playTrack(track) },
        onAddToPlaylist = { track -> showAddToPlaylistDialog(track) },
        onDelete = { track -> confirmDeleteTrack(track) },
        onShare = { track -> shareTrack(track) },
        onInfo = { track -> showTrackInfo(track) }
    )
    private val downloadAdapter = DownloadAdapter(
        onCancel = { download -> viewModel.cancelDownload(download.id) },
        onRetry = { download -> viewModel.startDownload(download.sourceUrl) }
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
        setupRecyclerViews()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        binding.rvDownloadedMusic.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
        }

        // Add download list if needed
    }

    private fun observeData() {
        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            musicAdapter.submitList(tracks)
            binding.emptyStateContainer.visibility = if (tracks.isEmpty()) View.VISIBLE else View.GONE
            binding.rvDownloadedMusic.visibility = if (tracks.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.downloads.observe(viewLifecycleOwner) { downloads ->
            // Filter active downloads
            val activeDownloads = downloads.filter { it.status != com.eqnitymusic.damn.data.model.DownloadStatus.COMPLETED }
            // Could show in a separate section
        }
    }

    private fun setupClickListeners() {
        binding.btnDownload.setOnClickListener { showDownloadDialog() }
        binding.btnEmptyStateDownload.setOnClickListener { showDownloadDialog() }
    }

    private fun showDownloadDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.btnDialogDownload.setOnClickListener {
            val url = dialogView.etDownloadUrl.text.toString().trim()
            if (isValidYoutubeUrl(url)) {
                dialog.dismiss()
                startDownload(url)
            } else {
                dialogView.tilDownloadUrl.error = getString(R.string.download_invalid_url)
            }
        }

        dialogView.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun isValidYoutubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be") || url.contains("youtube.com/shorts")
    }

    private fun startDownload(url: String) {
        lifecycleScope.launch {
            val downloadId = viewModel.startDownload(url)
            requireActivity().getSystemService(androidx.work.WorkManager::class.java)
                .enqueueUniqueWork(
                    "download_$downloadId",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    androidx.work.OneTimeWorkRequestBuilder<com.eqnitymusic.damn.worker.DownloadWorker>()
                        .setInputData(
                            androidx.work.Data.Builder()
                                .putLong("download_id", downloadId)
                                .putString("source_url", url)
                                .build()
                        )
                        .build()
                )
            showSnackbar(binding.root, getString(R.string.download_started))
        }
    }

    private fun playTrack(track: MusicTrack) {
        viewModel.incrementPlayCount(track)
        val activity = requireActivity() as MainActivity
        activity.playTracks(viewModel.tracks.value ?: emptyList(), viewModel.tracks.value?.indexOf(track) ?: 0)
    }

    private fun showTrackOptions(track: MusicTrack, view: View) {
        // Handled by adapter's popup menu
    }

    private fun showAddToPlaylistDialog(track: MusicTrack) {
        val navController = findNavController()
        navController.navigate(HomeFragmentDirections.actionHomeToAddToPlaylist(track.id))
    }

    private fun confirmDeleteTrack(track: MusicTrack) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.menu_delete)
            .setMessage(getString(R.string.playlist_delete_confirm, track.getDisplayTitle()))
            .setPositiveButton(R.string.playlist_delete) { _, _ ->
                viewModel.deleteTrack(track)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}