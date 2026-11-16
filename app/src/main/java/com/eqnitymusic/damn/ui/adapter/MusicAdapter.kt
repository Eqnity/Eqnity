package com.eqnitymusic.damn.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.ItemMusicBinding
import com.eqnitymusic.damn.util.getDominantColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicAdapter(
    private val onTrackClick: (MusicTrack) -> Unit,
    private val onTrackLongClick: (MusicTrack, View) -> Unit,
    private val onPlayClick: (MusicTrack) -> Unit,
    private val onAddToPlaylist: (MusicTrack) -> Unit,
    private val onDelete: (MusicTrack) -> Unit,
    private val onShare: (MusicTrack) -> Unit,
    private val onInfo: (MusicTrack) -> Unit
) : ListAdapter<MusicTrack, MusicAdapter.MusicViewHolder>(DiffCallback()) {

    inner class MusicViewHolder(private val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: MusicTrack, isPlaying: Boolean) {
            binding.tvMusicTitle.text = track.getDisplayTitle()
            binding.tvMusicArtist.text = track.getDisplayArtist()
            binding.tvMusicDuration.text = track.getFormattedDuration()

            // Load album art
            val artUrl = if (track.artworkPath.isNotBlank()) track.artworkPath else track.sourceUrl
            Glide.with(binding.root)
                .load(artUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .apply(RequestOptions().transform(RoundedCorners(16)))
                .into(binding.ivMusicArt)

            // Show playing indicator
            binding.ivMusicPlayingIndicator.visibility = if (isPlaying) View.VISIBLE else View.GONE
            binding.vMusicArtGradient.visibility = if (isPlaying) View.VISIBLE else View.GONE

            // More options menu
            binding.btnMusicMore.setOnClickListener { view ->
                showOptionsMenu(view, track)
            }

            // Click listeners
            binding.root.setOnClickListener { onTrackClick(track) }
            binding.root.setOnLongClickListener { 
                onTrackLongClick(track, binding.root)
                true
            }
        }

        private fun showOptionsMenu(view: View, track: MusicTrack) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.music_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_play -> onPlayClick(track)
                    R.id.menu_add_to_playlist -> onAddToPlaylist(track)
                    R.id.menu_delete -> onDelete(track)
                    R.id.menu_share -> onShare(track)
                    R.id.menu_info -> onInfo(track)
                }
                true
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track, false) // isPlaying will be updated externally
    }

    class DiffCallback : DiffUtil.ItemCallback<MusicTrack>() {
        override fun areItemsTheSame(oldItem: MusicTrack, newItem: MusicTrack): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MusicTrack, newItem: MusicTrack): Boolean {
            return oldItem == newItem
        }
    }
}