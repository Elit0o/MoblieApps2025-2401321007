package com.botops.app.ui.robots

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
import com.botops.app.data.entities.ZoneEntity
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

    private var cachedZones = listOf<ZoneEntity>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerRobots = view.findViewById(R.id.recyclerRobots)
        fabAddRobot = view.findViewById(R.id.fabAddRobot)

        setupRecyclerView()
        observeData()

        fabAddRobot.setOnClickListener {
            showAddRobotDialog()
        }
    }

    private fun showAddRobotDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_robot, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputRobotName)
        val inputNumber = dialogView.findViewById<EditText>(R.id.inputRobotNumber)
        val spinnerZone = dialogView.findViewById<Spinner>(R.id.spinnerZone)

        val zoneNames = listOf("None") + cachedZones.map { it.name }
        spinnerZone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, zoneNames)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Robot")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = inputName.text.toString().trim()
                val number = inputNumber.text.toString().trim()
                val selectedZoneIndex = spinnerZone.selectedItemPosition
                val zoneId = if (selectedZoneIndex > 0) cachedZones[selectedZoneIndex - 1].id else null

                if (name.isNotEmpty() && number.isNotEmpty()) {
                    // Status defaults to "Idle", battery defaults to 100
                    viewModel.insertRobot(name, number, "Idle", 100, zoneId)
                    Toast.makeText(requireContext(), "Robot added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Name and Number are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.robotsWithZone.collectLatest { robotsList ->
                        robotsAdapter.submitList(robotsList)
                    }
                }
                launch {
                    viewModel.zones.collectLatest { zonesList ->
                        cachedZones = zonesList
                    }
                }
            }
        }
    }
}
