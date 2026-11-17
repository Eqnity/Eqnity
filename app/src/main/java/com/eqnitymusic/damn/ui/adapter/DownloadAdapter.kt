package com.eqnitymusic.damn.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.Download
import com.eqnitymusic.damn.data.model.DownloadStatus
import com.eqnitymusic.damn.databinding.ItemDownloadBinding

class DownloadAdapter(
    private val onCancel: (Download) -> Unit,
    private val onRetry: (Download) -> Unit
) : ListAdapter<Download, DownloadAdapter.DownloadViewHolder>(DiffCallback()) {

    inner class DownloadViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(download: Download) {
            binding.tvDownloadTitle.text =
                if (download.title.isNotBlank()) download.title else "Downloading..."
            binding.tvDownloadStatus.text = download.status.name
            binding.progressDownload.progress = download.progress

            val statusColor = when (download.status) {
                DownloadStatus.DOWNLOADING -> R.color.download_active
                DownloadStatus.COMPLETED -> R.color.download_complete
                DownloadStatus.FAILED -> R.color.download_error
                DownloadStatus.PENDING -> R.color.download_pending
                else -> R.color.text_tertiary
            }.let { binding.root.context.getColor(it) }

            binding.tvDownloadStatus.setTextColor(statusColor)
            binding.progressDownload.setIndicatorColor(statusColor)

            if (download.status == DownloadStatus.FAILED && download.errorMessage.isNotBlank()) {
                binding.tvDownloadError.text = download.errorMessage
                binding.tvDownloadError.visibility = View.VISIBLE
            } else {
                binding.tvDownloadError.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> onCancel(download)
                    DownloadStatus.PENDING -> onCancel(download)
                    DownloadStatus.FAILED -> onRetry(download)
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
        override fun areItemsTheSame(oldItem: Download, newItem: Download): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Download, newItem: Download): Boolean =
            oldItem == newItem
    }
}