package com.botops.app.ui.waypoints

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botops.app.R
import com.botops.app.data.entities.WaypointWithZone
import com.botops.app.data.entities.ZoneEntity
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.ui.views.ZoneMapView
import com.botops.app.viewmodel.BotOpsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WaypointsFragment : Fragment(R.layout.fragment_waypoints) {

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
        BotOpsViewModel.Factory(repository)
    }

    private lateinit var waypointsAdapter: WaypointsAdapter
    private lateinit var recyclerWaypoints: RecyclerView
    private lateinit var fabAddWaypoint: FloatingActionButton
    private lateinit var spinnerZoneFilter: Spinner

    private var cachedZones = listOf<ZoneEntity>()
    private var cachedWaypointsWithZone = listOf<WaypointWithZone>()
    private var selectedZoneFilterIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerWaypoints = view.findViewById(R.id.recyclerWaypoints)
        fabAddWaypoint = view.findViewById(R.id.fabAddWaypoint)
        spinnerZoneFilter = view.findViewById(R.id.spinnerZoneFilter)

        spinnerZoneFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedZoneFilterIndex = position
                filterAndSubmitWaypoints()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupRecyclerView()
        observeData()

        fabAddWaypoint.setOnClickListener {
            showAddWaypointDialog()
        }
    }

    private fun filterAndSubmitWaypoints() {
        var filtered = cachedWaypointsWithZone
        if (selectedZoneFilterIndex > 0 && cachedZones.isNotEmpty()) {
            val selectedZone = cachedZones.getOrNull(selectedZoneFilterIndex - 1)
            filtered = filtered.filter { it.waypoint.zoneId == selectedZone?.id }
        }
        waypointsAdapter.submitList(filtered)
    }

    private fun showAddWaypointDialog() {
        if (cachedZones.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one Zone first!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_waypoint, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputWaypointName)
        val spinnerZone = dialogView.findViewById<Spinner>(R.id.spinnerZone)
        val zoneMapView = dialogView.findViewById<ZoneMapView>(R.id.zoneMapView)
        val textCoords = dialogView.findViewById<TextView>(R.id.textSelectedCoords)

        val zoneNames = cachedZones.map { it.name }
        spinnerZone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, zoneNames)

        var selX = 50.0
        var selY = 50.0

        spinnerZone.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedZone = cachedZones[position]
                zoneMapView.setZoneBounds(selectedZone.minX, selectedZone.maxX, selectedZone.minY, selectedZone.maxY)
                val zoneWps = cachedWaypointsWithZone.filter { it.waypoint.zoneId == selectedZone.id }.map { it.waypoint }
                zoneMapView.setExistingWaypoints(zoneWps)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        zoneMapView.onCoordinateSelectedListener = { x, y ->
            selX = x
            selY = y
            textCoords.text = "Selected: X: $x, Y: $y"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Waypoint")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = inputName.text.toString().trim()
                val selectedZone = cachedZones[spinnerZone.selectedItemPosition]

                if (name.isNotEmpty()) {
                    viewModel.insertWaypoint(name, selX, selY, selectedZone.id)
                    Toast.makeText(requireContext(), "Waypoint added successfully within room parameters", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Waypoint name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditWaypointDialog(item: WaypointWithZone) {
        val waypoint = item.waypoint
        if (cachedZones.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one Zone first!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_waypoint, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputWaypointName)
        val spinnerZone = dialogView.findViewById<Spinner>(R.id.spinnerZone)
        val zoneMapView = dialogView.findViewById<ZoneMapView>(R.id.zoneMapView)
        val textCoords = dialogView.findViewById<TextView>(R.id.textSelectedCoords)

        inputName.setText(waypoint.name)

        val zoneNames = cachedZones.map { it.name }
        spinnerZone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, zoneNames)
        val zoneIndex = cachedZones.indexOfFirst { it.id == waypoint.zoneId }
        if (zoneIndex >= 0) spinnerZone.setSelection(zoneIndex)

        var selX = waypoint.xCord
        var selY = waypoint.yCord
        textCoords.text = "Selected: X: $selX, Y: $selY"

        spinnerZone.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedZone = cachedZones[position]
                zoneMapView.setZoneBounds(selectedZone.minX, selectedZone.maxX, selectedZone.minY, selectedZone.maxY)
                val zoneWps = cachedWaypointsWithZone.filter { it.waypoint.zoneId == selectedZone.id }.map { it.waypoint }
                zoneMapView.setExistingWaypoints(zoneWps)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        zoneMapView.setSelectedCoordinate(waypoint.xCord, waypoint.yCord)
        zoneMapView.onCoordinateSelectedListener = { x, y ->
            selX = x
            selY = y
            textCoords.text = "Selected: X: $x, Y: $y"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Waypoint")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = inputName.text.toString().trim()
                val selectedZone = cachedZones[spinnerZone.selectedItemPosition]

                if (name.isNotEmpty()) {
                    viewModel.updateWaypoint(waypoint.id, name, selX, selY, selectedZone.id)
                    Toast.makeText(requireContext(), "Waypoint updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Waypoint name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        waypointsAdapter = WaypointsAdapter(
            onEditClick = { item -> showEditWaypointDialog(item) },
            onDeleteClick = { waypoint ->
                viewModel.deleteWaypoint(waypoint.id)
                Toast.makeText(requireContext(), "Waypoint deleted", Toast.LENGTH_SHORT).show()
            }
        )

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
                        cachedWaypointsWithZone = waypointsList
                        filterAndSubmitWaypoints()
                    }
                }
                launch {
                    viewModel.zones.collectLatest { zonesList ->
                        cachedZones = zonesList
                        val filterOptions = listOf("All Zones") + zonesList.map { it.name }
                        val currentSelection = spinnerZoneFilter.selectedItemPosition.coerceAtMost(filterOptions.size - 1)
                        spinnerZoneFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
                        spinnerZoneFilter.setSelection(currentSelection)
                        filterAndSubmitWaypoints()
                    }
                }
            }
        }
    }
}
