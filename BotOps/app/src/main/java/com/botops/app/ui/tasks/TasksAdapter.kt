package com.botops.app.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.R
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.TaskWithDetails

class TasksAdapter(
    private val onDeleteClick: (TaskEntity) -> Unit
) : ListAdapter<TaskWithDetails, TasksAdapter.TaskViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTaskStatus: TextView = itemView.findViewById(R.id.textTaskStatus)
        private val textTaskRobot: TextView = itemView.findViewById(R.id.textTaskRobot)
        private val textTaskWaypoint: TextView = itemView.findViewById(R.id.textTaskWaypoint)
        private val textTaskPriority: TextView = itemView.findViewById(R.id.textTaskPriority)
        private val buttonDeleteTask: ImageButton = itemView.findViewById(R.id.buttonDeleteTask)

        fun bind(item: TaskWithDetails) {
            val task = item.task
            textTaskStatus.text = "Status: ${task.status}"
            textTaskRobot.text = "Robot: ${item.robotName ?: "Unknown"}"
            textTaskWaypoint.text = "Waypoint: ${item.waypointName ?: "Unknown"}"
            textTaskPriority.text = "Priority: ${task.priority}"

            buttonDeleteTask.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TaskWithDetails>() {
        override fun areItemsTheSame(oldItem: TaskWithDetails, newItem: TaskWithDetails): Boolean {
            return oldItem.task.id == newItem.task.id
        }

        override fun areContentsTheSame(oldItem: TaskWithDetails, newItem: TaskWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}
