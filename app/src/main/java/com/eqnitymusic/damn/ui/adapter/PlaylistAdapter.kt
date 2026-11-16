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
import com.eqnitymusic.damn.data.model.Playlist
import com.eqnitymusic.damn.databinding.ItemPlaylistBinding

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onPlaylistLongClick: (Playlist, View) -> Unit,
    private val onPlayClick: (Playlist) -> Unit,
    private val onRename: (Playlist) -> Unit,
    private val onDelete: (Playlist) -> Unit,
    private val onAddSongs: (Playlist) -> Unit
) : ListAdapter<Playlist, PlaylistAdapter.PlaylistViewHolder>(DiffCallback()) {

    inner class PlaylistViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist) {
            binding.tvPlaylistName.text = playlist.getDisplayName()
            binding.tvPlaylistSongCount.text = "${playlist.songCount} songs · ${playlist.getFormattedDuration()}"

            // Load cover art (use first track's art or placeholder)
            Glide.with(binding.root)
                .load(playlist.coverArtPath)
                .placeholder(R.drawable.ic_playlist)
                .error(R.drawable.ic_playlist)
                .apply(RequestOptions().transform(RoundedCorners(16)))
                .into(binding.ivPlaylistCover)

            // More options
            binding.btnPlaylistMore.setOnClickListener { view ->
                showOptionsMenu(view, playlist)
            }

            binding.root.setOnClickListener { onPlaylistClick(playlist) }
            binding.root.setOnLongClickListener { 
                onPlaylistLongClick(playlist, binding.root)
                true
            }
        }

        private fun showOptionsMenu(view: View, playlist: Playlist) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.playlist_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_play -> onPlayClick(playlist)
                    R.id.menu_rename -> onRename(playlist)
                    R.id.menu_delete -> onDelete(playlist)
                    R.id.menu_add_songs -> onAddSongs(playlist)
                }
                true
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
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