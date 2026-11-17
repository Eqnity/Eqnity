package com.eqnitymusic.damn.ui.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.ItemTrackPickBinding

class TrackPickAdapter(
    private val isSelected: (Long) -> Boolean,
    private val onToggle: (Long) -> Unit
) : ListAdapter<MusicTrack, TrackPickAdapter.VH>(DiffCallback()) {

    inner class VH(private val binding: ItemTrackPickBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(track: MusicTrack) {
            binding.tvMusicTitle.text = track.getDisplayTitle()
            binding.tvMusicArtist.text = track.getDisplayArtist()
            binding.tvMusicDuration.text = track.getFormattedDuration()

            Glide.with(binding.root)
                .load(track.artworkPath)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .apply(RequestOptions().transform(RoundedCorners(12)))
                .into(binding.ivMusicArt)

            binding.cbTrackSelected.isChecked = isSelected(track.id)
            binding.root.setOnClickListener { onToggle(track.id) }
            binding.cbTrackSelected.setOnClickListener { onToggle(track.id) }
        }
    }

    fun notifySelectionChanged() {
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTrackPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<MusicTrack>() {
        override fun areItemsTheSame(oldItem: MusicTrack, newItem: MusicTrack): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MusicTrack, newItem: MusicTrack): Boolean =
            oldItem == newItem
    }
}