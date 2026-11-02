package com.botops.app.ui.robots

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

class RobotsFragment : Fragment(R.layout.fragment_robots) {

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

    private lateinit var robotsAdapter: RobotsAdapter
    private lateinit var recyclerRobots: RecyclerView
    private lateinit var fabAddRobot: FloatingActionButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerRobots = view.findViewById(R.id.recyclerRobots)
        fabAddRobot = view.findViewById(R.id.fabAddRobot)

        setupRecyclerView()
        observeRobots()

        fabAddRobot.setOnClickListener {
            val randomNum = (100..999).random()
            viewModel.insertRobot("Robot $randomNum", "R-$randomNum", "Idle", 100, null)
            Toast.makeText(requireContext(), "Robot added successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        robotsAdapter = RobotsAdapter { robot ->
            viewModel.deleteRobot(robot.id)
            Toast.makeText(requireContext(), "Robot deleted", Toast.LENGTH_SHORT).show()
        }

        recyclerRobots.apply {
            adapter = robotsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeRobots() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.robots.collectLatest { robotsList ->
                    robotsAdapter.submitList(robotsList)
                }
            }
        }
    }
}
