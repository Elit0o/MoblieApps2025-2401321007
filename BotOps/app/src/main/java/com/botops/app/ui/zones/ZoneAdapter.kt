package com.botops.app.ui.zones

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.data.entities.ZoneEntity
import com.botops.app.databinding.ItemZoneBinding

class ZonesAdapter(
    private val onDeleteClick: (ZoneEntity) -> Unit
) : ListAdapter<ZoneEntity, ZonesAdapter.ZoneViewHolder>(ZoneDiffCallback()) {

    inner class ZoneViewHolder(private val binding: ItemZoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(zone: ZoneEntity) {
            binding.textZoneName.text = zone.name
            binding.textZoneDescription.text = zone.description

            binding.buttonDeleteZone.setOnClickListener {
                onDeleteClick(zone)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val binding = ItemZoneBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ZoneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ZoneDiffCallback : DiffUtil.ItemCallback<ZoneEntity>() {
        override fun areItemsTheSame(oldItem: ZoneEntity, newItem: ZoneEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ZoneEntity, newItem: ZoneEntity): Boolean {
            return oldItem == newItem
        }
    }
}