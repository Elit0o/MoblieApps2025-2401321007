package com.botops.app.ui.tasks

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.R
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.TaskWithDetails
import com.botops.app.data.entities.WaypointWithZone
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.viewmodel.BotOpsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
        BotOpsViewModel.Factory(repository)
    }

    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var recyclerTasks: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var spinnerTaskStatusFilter: Spinner
    private lateinit var spinnerTaskRobotFilter: Spinner

    private var cachedRobots = listOf<RobotEntity>()
    private var cachedWaypointsWithZone = listOf<WaypointWithZone>()
    private var cachedTasksWithDetails = listOf<TaskWithDetails>()

    private var selectedStatusFilter = "All Tasks"
    private var selectedRobotFilterIndex = 0
    private var lastRobotNames = listOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTasks = view.findViewById(R.id.recyclerTasks)
        fabAddTask = view.findViewById(R.id.fabAddTask)
        spinnerTaskStatusFilter = view.findViewById(R.id.spinnerTaskStatusFilter)
        spinnerTaskRobotFilter = view.findViewById(R.id.spinnerTaskRobotFilter)

        val statusFilters = listOf("All Tasks", "Active", "Pending", "Completed", "Paused", "Cancelled")
        spinnerTaskStatusFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusFilters)
        spinnerTaskStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatusFilter = statusFilters[position]
                filterAndSubmitTasks()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTaskRobotFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRobotFilterIndex = position
                filterAndSubmitTasks()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupRecyclerView()
        observeData()

        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun filterAndSubmitTasks() {
        var filtered = cachedTasksWithDetails

        if (selectedStatusFilter != "All Tasks") {
            filtered = filtered.filter { it.task.status.equals(selectedStatusFilter, true) }
        }

        if (selectedRobotFilterIndex > 0 && cachedRobots.isNotEmpty()) {
            val selectedRobot = cachedRobots.getOrNull(selectedRobotFilterIndex - 1)
            filtered = filtered.filter { it.task.robotId == selectedRobot?.id }
        }

        tasksAdapter.submitList(ArrayList(filtered))
    }

    private fun showAddTaskDialog() {
        if (cachedRobots.isEmpty() || cachedWaypointsWithZone.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one Robot and one Waypoint first!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val inputTitle = dialogView.findViewById<EditText>(R.id.inputTaskTitle)
        val spinnerRobot = dialogView.findViewById<Spinner>(R.id.spinnerRobot)
        val spinnerWaypoint = dialogView.findViewById<Spinner>(R.id.spinnerWaypoint)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerTaskPriority)

        val robotDisplayList = cachedRobots.map { "${it.robotName} (${it.batteryLevel}% Battery)" }
        spinnerRobot.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, robotDisplayList)

        var currentValidWaypoints = cachedWaypointsWithZone

        spinnerRobot.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRobot = cachedRobots.getOrNull(position)
                val robotZoneId = selectedRobot?.zoneId

                currentValidWaypoints = if (robotZoneId != null) {
                    cachedWaypointsWithZone.filter { it.waypoint.zoneId == robotZoneId }
                } else {
                    cachedWaypointsWithZone
                }

                val waypointDisplayList = if (currentValidWaypoints.isNotEmpty()) {
                    currentValidWaypoints.map { "${it.waypoint.name} (${it.zoneName ?: "No Zone"})" }
                } else {
                    listOf("No waypoints in robot's zone")
                }

                spinnerWaypoint.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, waypointDisplayList)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val priorityOptions = listOf("High (Priority 3)", "Medium (Priority 2)", "Low (Priority 1)")
        val priorityValues = listOf(3, 2, 1)
        spinnerPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityOptions)

        AlertDialog.Builder(requireContext())
            .setTitle("Create Task")
            .setView(dialogView)
            .setPositiveButton("Add Task") { _, _ ->
                if (currentValidWaypoints.isEmpty()) {
                    Toast.makeText(requireContext(), "Selected robot has no accessible waypoints in its zone!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val title = inputTitle.text.toString().trim().ifEmpty { "Inspection Task" }
                val selectedRobot = cachedRobots[spinnerRobot.selectedItemPosition]
                val selectedWaypoint = currentValidWaypoints[spinnerWaypoint.selectedItemPosition].waypoint
                val selectedPriority = priorityValues[spinnerPriority.selectedItemPosition]

                viewModel.insertTask(title, selectedRobot.id, selectedWaypoint.id, selectedPriority)
                Toast.makeText(requireContext(), "Task created & simulation running!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTaskDialog(item: TaskWithDetails) {
        val task = item.task
        if (cachedRobots.isEmpty() || cachedWaypointsWithZone.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one Robot and one Waypoint first!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val inputTitle = dialogView.findViewById<EditText>(R.id.inputTaskTitle)
        val spinnerRobot = dialogView.findViewById<Spinner>(R.id.spinnerRobot)
        val spinnerWaypoint = dialogView.findViewById<Spinner>(R.id.spinnerWaypoint)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerTaskPriority)

        inputTitle.setText(task.title)

        val robotDisplayList = cachedRobots.map { "${it.robotName} (${it.batteryLevel}% Battery)" }
        spinnerRobot.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, robotDisplayList)
        val robotIndex = cachedRobots.indexOfFirst { it.id == task.robotId }
        if (robotIndex >= 0) spinnerRobot.setSelection(robotIndex)

        var currentValidWaypoints = cachedWaypointsWithZone

        spinnerRobot.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRobot = cachedRobots.getOrNull(position)
                val robotZoneId = selectedRobot?.zoneId

                currentValidWaypoints = if (robotZoneId != null) {
                    cachedWaypointsWithZone.filter { it.waypoint.zoneId == robotZoneId }
                } else {
                    cachedWaypointsWithZone
                }

                val waypointDisplayList = if (currentValidWaypoints.isNotEmpty()) {
                    currentValidWaypoints.map { "${it.waypoint.name} (${it.zoneName ?: "No Zone"})" }
                } else {
                    listOf("No waypoints in robot's zone")
                }

                spinnerWaypoint.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, waypointDisplayList)
                val waypointIndex = currentValidWaypoints.indexOfFirst { it.waypoint.id == task.waypointId }
                if (waypointIndex >= 0) spinnerWaypoint.setSelection(waypointIndex)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val priorityOptions = listOf("High (Priority 3)", "Medium (Priority 2)", "Low (Priority 1)")
        val priorityValues = listOf(3, 2, 1)
        spinnerPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityOptions)
        val priorityIndex = priorityValues.indexOf(task.priority)
        if (priorityIndex >= 0) spinnerPriority.setSelection(priorityIndex)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (currentValidWaypoints.isEmpty()) {
                    Toast.makeText(requireContext(), "Selected robot has no accessible waypoints in its zone!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val title = inputTitle.text.toString().trim().ifEmpty { "Inspection Task" }
                val selectedRobot = cachedRobots[spinnerRobot.selectedItemPosition]
                val selectedWaypoint = currentValidWaypoints[spinnerWaypoint.selectedItemPosition].waypoint
                val selectedPriority = priorityValues[spinnerPriority.selectedItemPosition]

                viewModel.updateTask(task.id, title, selectedRobot.id, selectedWaypoint.id, task.status, selectedPriority, task.progress, task.createdAt)
                Toast.makeText(requireContext(), "Task updated successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(
            onCompleteClick = { item ->
                viewModel.markTaskAsCompleted(item.task.id)
                Toast.makeText(requireContext(), "Task marked as Completed", Toast.LENGTH_SHORT).show()
            },
            onPauseClick = { item ->
                viewModel.pauseTask(item.task.id)
                Toast.makeText(requireContext(), "Task pause/resume toggled", Toast.LENGTH_SHORT).show()
            },
            onCancelClick = { item ->
                viewModel.cancelTask(item.task.id)
                Toast.makeText(requireContext(), "Task cancelled", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { item -> showEditTaskDialog(item) },
            onDeleteClick = { task ->
                viewModel.deleteTask(task.id)
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerTasks.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasksWithDetails.collect { tasksList ->
                        cachedTasksWithDetails = tasksList
                        filterAndSubmitTasks()
                    }
                }
                launch {
                    viewModel.robots.collect { robotsList ->
                        cachedRobots = robotsList
                        val robotNames = robotsList.map { it.robotName }
                        if (robotNames != lastRobotNames) {
                            lastRobotNames = robotNames
                            val filterOptions = listOf("All Robots") + robotNames
                            val currentSelection = spinnerTaskRobotFilter.selectedItemPosition.coerceAtMost(filterOptions.size - 1)
                            spinnerTaskRobotFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
                            spinnerTaskRobotFilter.setSelection(currentSelection)
                        }
                        filterAndSubmitTasks()
                    }
                }
                launch {
                    viewModel.waypointsWithZone.collect { waypointsList ->
                        cachedWaypointsWithZone = waypointsList
                    }
                }
            }
        }
    }
}
