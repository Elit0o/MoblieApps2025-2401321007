package com.botops.app.ui.robots

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.RobotWithZone
import com.botops.app.databinding.ItemRobotBinding

class RobotsAdapter(
    private val onEditClick: (RobotWithZone) -> Unit,
    private val onDeleteClick: (RobotEntity) -> Unit
) : ListAdapter<RobotWithZone, RobotsAdapter.RobotViewHolder>(RobotDiffCallback()) {

    inner class RobotViewHolder(private val binding: ItemRobotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RobotWithZone) {
            val robot = item.robot
            binding.textRobotName.text = robot.robotName
            binding.textRobotNumber.text = "Number: ${robot.robotNumber}"
            binding.textRobotStatus.text = "Status: ${robot.status} | Zone: ${item.zoneName ?: "None"}"
            binding.textRobotBattery.text = "Battery: ${robot.batteryLevel}%"

            binding.buttonEditRobot.setOnClickListener {
                onEditClick(item)
            }
            binding.buttonDeleteRobot.setOnClickListener {
                onDeleteClick(robot)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RobotViewHolder {
        val binding = ItemRobotBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RobotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RobotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RobotDiffCallback : DiffUtil.ItemCallback<RobotWithZone>() {
        override fun areItemsTheSame(oldItem: RobotWithZone, newItem: RobotWithZone): Boolean {
            return oldItem.robot.id == newItem.robot.id
        }

        override fun areContentsTheSame(oldItem: RobotWithZone, newItem: RobotWithZone): Boolean {
            return oldItem == newItem
        }
    }
}
