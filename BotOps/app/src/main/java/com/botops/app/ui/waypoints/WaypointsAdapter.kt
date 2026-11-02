package com.botops.app.ui.waypoints

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.R
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.WaypointWithZone

class WaypointsAdapter(
    private val onDeleteClick: (WaypointEntity) -> Unit
) : ListAdapter<WaypointWithZone, WaypointsAdapter.WaypointViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waypoint, parent, false)
        return WaypointViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WaypointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textWaypointName: TextView = itemView.findViewById(R.id.textWaypointName)
        private val textWaypointCoords: TextView = itemView.findViewById(R.id.textWaypointCoords)
        private val textWaypointZone: TextView = itemView.findViewById(R.id.textWaypointZone)
        private val buttonDeleteWaypoint: ImageButton = itemView.findViewById(R.id.buttonDeleteWaypoint)

        fun bind(item: WaypointWithZone) {
            val waypoint = item.waypoint
            textWaypointName.text = waypoint.name
            textWaypointCoords.text = "X: ${waypoint.xCord}, Y: ${waypoint.yCord}"
            textWaypointZone.text = "Zone: ${item.zoneName ?: "None"}"

            buttonDeleteWaypoint.setOnClickListener {
                onDeleteClick(waypoint)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WaypointWithZone>() {
        override fun areItemsTheSame(oldItem: WaypointWithZone, newItem: WaypointWithZone): Boolean {
            return oldItem.waypoint.id == newItem.waypoint.id
        }

        override fun areContentsTheSame(oldItem: WaypointWithZone, newItem: WaypointWithZone): Boolean {
            return oldItem == newItem
        }
    }
}
