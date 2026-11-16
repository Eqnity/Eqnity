package com.eqnitymusic.damn.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eqnitymusic.damn.data.network.EqnityInfoResponse
import com.eqnitymusic.damn.databinding.ItemSkillBinding

class SkillAdapter : ListAdapter<EqnityInfoResponse.Skill, SkillAdapter.SkillViewHolder>(DiffCallback()) {

    inner class SkillViewHolder(private val binding: ItemSkillBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(skill: EqnityInfoResponse.Skill) {
            binding.tvSkillTitle.text = skill.title
            binding.tvSkillDescription.text = skill.description
            // Icon would be set based on skill title
            when (skill.title.lowercase()) {
                "websites" -> binding.ivSkillIcon.setImageResource(R.drawable.ic_web)
                "discord bots" -> binding.ivSkillIcon.setImageResource(R.drawable.ic_bot)
                "minecraft clients" -> binding.ivSkillIcon.setImageResource(R.drawable.ic_minecraft)
                "minecraft mods" -> binding.ivSkillIcon.setImageResource(R.drawable.ic_mod)
                else -> binding.ivSkillIcon.setImageResource(R.drawable.ic_code)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val binding = ItemSkillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SkillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<EqnityInfoResponse.Skill>() {
        override fun areItemsTheSame(oldItem: EqnityInfoResponse.Skill, newItem: EqnityInfoResponse.Skill): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: EqnityInfoResponse.Skill, newItem: EqnityInfoResponse.Skill): Boolean {
            return oldItem == newItem
        }
    }
}