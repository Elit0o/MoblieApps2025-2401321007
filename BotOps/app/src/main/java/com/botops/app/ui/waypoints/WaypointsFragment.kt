package com.botops.app.ui.waypoints

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

class WaypointsFragment : Fragment(R.layout.fragment_waypoints) {

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

    private lateinit var waypointsAdapter: WaypointsAdapter
    private lateinit var recyclerWaypoints: RecyclerView
    private lateinit var fabAddWaypoint: FloatingActionButton

    private var cachedZones = listOf<ZoneEntity>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerWaypoints = view.findViewById(R.id.recyclerWaypoints)
        fabAddWaypoint = view.findViewById(R.id.fabAddWaypoint)

        setupRecyclerView()
        observeData()

        fabAddWaypoint.setOnClickListener {
            showAddWaypointDialog()
        }
    }

    private fun showAddWaypointDialog() {
        if (cachedZones.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one Zone first!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_waypoint, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputWaypointName)
        val inputX = dialogView.findViewById<EditText>(R.id.inputWaypointX)
        val inputY = dialogView.findViewById<EditText>(R.id.inputWaypointY)
        val spinnerZone = dialogView.findViewById<Spinner>(R.id.spinnerZone)

        spinnerZone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cachedZones.map { it.name })

        AlertDialog.Builder(requireContext())
            .setTitle("Add Waypoint")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = inputName.text.toString().trim()
                val x = inputX.text.toString().toDoubleOrNull() ?: 0.0
                val y = inputY.text.toString().toDoubleOrNull() ?: 0.0
                val selectedZone = cachedZones[spinnerZone.selectedItemPosition]

                if (name.isNotEmpty()) {
                    viewModel.insertWaypoint(name, x, y, selectedZone.id)
                    Toast.makeText(requireContext(), "Waypoint added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Waypoint name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        waypointsAdapter = WaypointsAdapter { waypoint ->
            viewModel.deleteWaypoint(waypoint.id)
            Toast.makeText(requireContext(), "Waypoint deleted", Toast.LENGTH_SHORT).show()
        }

        recyclerWaypoints.apply {
            adapter = waypointsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.waypointsWithZone.collectLatest { waypointsList ->
                        waypointsAdapter.submitList(waypointsList)
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
