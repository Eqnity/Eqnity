package com.eqnitymusic.damn.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.databinding.ItemPlaylistOptionBinding

class PlaylistOptionAdapter(
    private val onSelectionChange: (Playlist, Boolean) -> Unit
) : ListAdapter<Playlist, PlaylistOptionAdapter.PlaylistOptionViewHolder>(DiffCallback()) {

    inner class PlaylistOptionViewHolder(private val binding: ItemPlaylistOptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist, isSelected: Boolean) {
            binding.cbPlaylistSelected.isChecked = isSelected
            binding.tvPlaylistOptionName.text = playlist.getDisplayName()
            binding.tvPlaylistOptionCount.text = "${playlist.songCount} songs"

            Glide.with(binding.root)
                .load(playlist.coverArtPath)
                .placeholder(R.drawable.ic_playlist)
                .error(R.drawable.ic_playlist)
                .apply(RequestOptions().transform(RoundedCorners(8)))
                .into(binding.ivPlaylistOptionCover)

            binding.cbPlaylistSelected.setOnCheckedChangeListener { _, checked ->
                onSelectionChange(playlist, checked)
            }

            binding.root.setOnClickListener {
                binding.cbPlaylistSelected.toggle()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistOptionViewHolder {
        val binding = ItemPlaylistOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistOptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistOptionViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist, false) // Selection state managed externally
    }

    class DiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
}