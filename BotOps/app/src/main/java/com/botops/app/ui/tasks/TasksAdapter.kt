package com.botops.app.ui.tasks

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.R
import com.botops.app.data.entities.TaskEntity
import com.botops.app.data.entities.TaskWithDetails

class TasksAdapter(
    private val onCompleteClick: (TaskWithDetails) -> Unit,
    private val onPauseClick: (TaskWithDetails) -> Unit,
    private val onCancelClick: (TaskWithDetails) -> Unit,
    private val onEditClick: (TaskWithDetails) -> Unit,
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
        private val textTaskPriority: TextView = itemView.findViewById(R.id.textTaskPriority)
        private val textTaskTitle: TextView = itemView.findViewById(R.id.textTaskTitle)
        private val textTaskRobot: TextView = itemView.findViewById(R.id.textTaskRobot)
        private val textTaskWaypoint: TextView = itemView.findViewById(R.id.textTaskWaypoint)
        private val progressBarTask: ProgressBar = itemView.findViewById(R.id.progressBarTask)
        private val textTaskProgressPercent: TextView = itemView.findViewById(R.id.textTaskProgressPercent)
        private val buttonCompleteTask: ImageButton = itemView.findViewById(R.id.buttonCompleteTask)
        private val buttonPauseTask: ImageButton = itemView.findViewById(R.id.buttonPauseTask)
        private val buttonCancelTask: ImageButton = itemView.findViewById(R.id.buttonCancelTask)
        private val buttonEditTask: ImageButton = itemView.findViewById(R.id.buttonEditTask)
        private val buttonDeleteTask: ImageButton = itemView.findViewById(R.id.buttonDeleteTask)

        fun bind(item: TaskWithDetails) {
            val task = item.task
            val status = task.status
            textTaskStatus.text = status

            when {
                status.equals("Active", ignoreCase = true) -> {
                    textTaskStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    textTaskStatus.setTextColor(Color.parseColor("#2E7D32"))
                }
                status.equals("Completed", ignoreCase = true) -> {
                    textTaskStatus.setBackgroundColor(Color.parseColor("#ECEFF1"))
                    textTaskStatus.setTextColor(Color.parseColor("#455A64"))
                }
                status.equals("Paused", ignoreCase = true) -> {
                    textTaskStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
                    textTaskStatus.setTextColor(Color.parseColor("#E65100"))
                }
                status.equals("Cancelled", ignoreCase = true) -> {
                    textTaskStatus.setBackgroundColor(Color.parseColor("#FFEBEE"))
                    textTaskStatus.setTextColor(Color.parseColor("#C62828"))
                }
                else -> {
                    textTaskStatus.setBackgroundColor(Color.parseColor("#FFF8E1"))
                    textTaskStatus.setTextColor(Color.parseColor("#F57F17"))
                }
            }

            textTaskPriority.text = "Priority: ${task.priority}"
            textTaskTitle.text = task.title.ifEmpty { "Task #${task.id}" }

            val robotInfo = if (item.robotName != null) {
                "${item.robotName} (${item.robotBattery ?: 0}% Battery)"
            } else {
                "Unassigned Robot"
            }
            textTaskRobot.text = "Robot: $robotInfo"

            val destinationInfo = if (item.waypointName != null) {
                "${item.waypointName} (${item.zoneName ?: "No Zone"})"
            } else {
                "Unassigned Waypoint"
            }
            textTaskWaypoint.text = "Destination: $destinationInfo"

            progressBarTask.progress = task.progress
            textTaskProgressPercent.text = "${task.progress}%"

            val isFinished = status.equals("Completed", true) || status.equals("Cancelled", true)
            buttonCompleteTask.visibility = if (isFinished) View.GONE else View.VISIBLE
            buttonPauseTask.visibility = if (isFinished) View.GONE else View.VISIBLE
            buttonCancelTask.visibility = if (isFinished) View.GONE else View.VISIBLE
            buttonEditTask.visibility = if (isFinished) View.GONE else View.VISIBLE
            buttonDeleteTask.visibility = View.VISIBLE

            buttonCompleteTask.setOnClickListener {
                onCompleteClick(item)
            }
            buttonPauseTask.setOnClickListener {
                onPauseClick(item)
            }
            buttonCancelTask.setOnClickListener {
                onCancelClick(item)
            }
            buttonEditTask.setOnClickListener {
                onEditClick(item)
            }
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
