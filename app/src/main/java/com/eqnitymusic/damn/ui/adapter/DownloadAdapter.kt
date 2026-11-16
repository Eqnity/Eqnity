package com.eqnitymusic.damn.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import com.eqnitymusic.damn.databinding.ItemDownloadBinding

class DownloadAdapter(
    private val onCancel: (Download) -> Unit,
    private val onRetry: (Download) -> Unit
) : ListAdapter<Download, DownloadAdapter.DownloadViewHolder>(DiffCallback()) {

    inner class DownloadViewHolder(private val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(download: Download) {
            binding.tvDownloadTitle.text = if (download.title.isNotBlank()) download.title else "Downloading..."
            binding.tvDownloadStatus.text = download.status.name.capitalize()
            binding.progressDownload.progress = download.progress

            // Status color
            val statusColor = when (download.status) {
                DownloadStatus.DOWNLOADING -> binding.root.context.getColor(R.color.download_active)
                DownloadStatus.COMPLETED -> binding.root.context.getColor(R.color.download_complete)
                DownloadStatus.FAILED -> binding.root.context.getColor(R.color.download_error)
                DownloadStatus.PENDING -> binding.root.context.getColor(R.color.download_pending)
                else -> binding.root.context.getColor(R.color.text_tertiary)
            }
            binding.tvDownloadStatus.setTextColor(statusColor)
            binding.progressDownload.indicatorColor = statusColor

            if (download.status == DownloadStatus.FAILED && download.errorMessage.isNotBlank()) {
                binding.tvDownloadError.text = download.errorMessage
                binding.tvDownloadError.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDownloadError.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> onCancel(download)
                    DownloadStatus.FAILED -> onRetry(download)
                    DownloadStatus.PENDING -> onCancel(download)
                    else -> {}
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Download>() {
        override fun areItemsTheSame(oldItem: Download, newItem: Download): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Download, newItem: Download): Boolean {
            return oldItem == newItem
        }
    }
}