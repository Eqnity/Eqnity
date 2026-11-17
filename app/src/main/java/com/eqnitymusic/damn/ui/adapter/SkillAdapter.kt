package com.eqnitymusic.damn.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.network.Skill
import com.eqnitymusic.damn.databinding.ItemSkillBinding

class SkillAdapter : ListAdapter<Skill, SkillAdapter.SkillViewHolder>(DiffCallback()) {

    inner class SkillViewHolder(private val binding: ItemSkillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(skill: Skill) {
            binding.tvSkillTitle.text = skill.title
            binding.tvSkillDescription.text = skill.description
            binding.tvSkillTech.text = skill.technologies

            val icon = when (skill.title.lowercase()) {
                "websites" -> R.drawable.ic_web
                "discord bots" -> R.drawable.ic_bot
                "minecraft clients" -> R.drawable.ic_minecraft
                "minecraft mods" -> R.drawable.ic_mod
                else -> R.drawable.ic_code
            }
            binding.ivSkillIcon.setImageResource(icon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val binding = ItemSkillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SkillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Skill>() {
        override fun areItemsTheSame(oldItem: Skill, newItem: Skill): Boolean =
            oldItem.title == newItem.title

        override fun areContentsTheSame(oldItem: Skill, newItem: Skill): Boolean =
            oldItem == newItem
    }
}