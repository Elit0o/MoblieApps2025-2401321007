package com.botops.app.ui.robots

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.data.entities.RobotEntity
import com.botops.app.databinding.ItemRobotBinding

class RobotsAdapter(
    private val onDeleteClick: (RobotEntity) -> Unit
) : ListAdapter<RobotEntity, RobotsAdapter.RobotViewHolder>(RobotDiffCallback()) {

    inner class RobotViewHolder(private val binding: ItemRobotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(robot: RobotEntity) {
            binding.textRobotName.text = robot.robotName
            binding.textRobotNumber.text = "Number: ${robot.robotNumber}"
            binding.textRobotStatus.text = "Status: ${robot.status}"
            binding.textRobotBattery.text = "Battery: ${robot.batteryLevel}%"

            // Бутон за изтриване на робота
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

    class RobotDiffCallback : DiffUtil.ItemCallback<RobotEntity>() {
        override fun areItemsTheSame(oldItem: RobotEntity, newItem: RobotEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RobotEntity, newItem: RobotEntity): Boolean {
            return oldItem == newItem
        }
    }
}