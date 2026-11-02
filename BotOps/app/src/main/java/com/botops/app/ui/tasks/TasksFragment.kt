package com.botops.app.ui.tasks

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.R
import com.botops.app.data.database.BotOpsDatabase
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.viewmodel.BotOpsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private val viewModel: BotOpsViewModel by viewModels<BotOpsViewModel> {
        val database = BotOpsDatabase.getInstance(requireContext())
        val repository = BotOpsRepository(
            database.zoneDao(),
            database.waypointDao(),
            database.robotDao(),
            database.taskDao()
        )
        BotOpsViewModel.Factory(repository)
    }

    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var recyclerTasks: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton

    private var cachedRobots = listOf<RobotEntity>()
    private var cachedWaypoints = listOf<WaypointEntity>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTasks = view.findViewById(R.id.recyclerTasks)
        fabAddTask = view.findViewById(R.id.fabAddTask)

        setupRecyclerView()
        observeData()

        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        if (cachedRobots.isEmpty() || cachedWaypoints.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one Robot and one Waypoint first!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val spinnerRobot = dialogView.findViewById<Spinner>(R.id.spinnerRobot)
        val spinnerWaypoint = dialogView.findViewById<Spinner>(R.id.spinnerWaypoint)
        val inputStatus = dialogView.findViewById<EditText>(R.id.inputTaskStatus)
        val inputPriority = dialogView.findViewById<EditText>(R.id.inputTaskPriority)

        spinnerRobot.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cachedRobots.map { it.robotName })
        spinnerWaypoint.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cachedWaypoints.map { it.name })

        AlertDialog.Builder(requireContext())
            .setTitle("Create Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val selectedRobot = cachedRobots[spinnerRobot.selectedItemPosition]
                val selectedWaypoint = cachedWaypoints[spinnerWaypoint.selectedItemPosition]
                val status = inputStatus.text.toString().trim().ifEmpty { "Pending" }
                val priority = inputPriority.text.toString().toIntOrNull() ?: 1

                viewModel.insertTask(selectedRobot.id, selectedWaypoint.id, status, priority)
                Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter { task ->
            viewModel.deleteTask(task.id)
            Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
        }

        recyclerTasks.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasksWithDetails.collectLatest { tasksList ->
                        tasksAdapter.submitList(tasksList)
                    }
                }
                launch {
                    viewModel.robots.collectLatest { robotsList ->
                        cachedRobots = robotsList
                    }
                }
                launch {
                    viewModel.waypoints.collectLatest { waypointsList ->
                        cachedWaypoints = waypointsList
                    }
                }
            }
        }
    }
}
