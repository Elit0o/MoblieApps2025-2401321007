package com.botops.app.ui.tasks

import android.os.Bundle
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTasks = view.findViewById(R.id.recyclerTasks)
        fabAddTask = view.findViewById(R.id.fabAddTask)

        setupRecyclerView()
        observeTasks()

        fabAddTask.setOnClickListener {
            viewModel.insertTask(robotId = 1L, waypointId = 1L, status = "Pending", priority = 1)
            Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show()
        }
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

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasks.collectLatest { tasksList ->
                    tasksAdapter.submitList(tasksList)
                }
            }
        }
    }
}