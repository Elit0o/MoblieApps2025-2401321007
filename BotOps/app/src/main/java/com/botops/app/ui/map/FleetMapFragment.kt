package com.botops.app.ui.map

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.botops.app.R
import com.botops.app.data.database.BotOpsDatabase
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.ZoneEntity
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.ui.views.FleetLiveMapView
import com.botops.app.viewmodel.BotOpsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FleetMapFragment : Fragment(R.layout.fragment_fleet_map) {

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
        BotOpsViewModel.Factory(repository)
    }

    private lateinit var fleetLiveMapView: FleetLiveMapView
    private lateinit var spinnerLiveMapZoneFilter: Spinner

    private var currentZones = emptyList<ZoneEntity>()
    private var currentWaypoints = emptyList<WaypointEntity>()
    private var currentRobots = emptyList<RobotEntity>()
    private var selectedZoneIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fleetLiveMapView = view.findViewById(R.id.fleetLiveMapView)
        spinnerLiveMapZoneFilter = view.findViewById(R.id.spinnerLiveMapZoneFilter)

        spinnerLiveMapZoneFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedZoneIndex = position
                updateMap()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.zones.collectLatest { zones ->
                        currentZones = zones
                        val filterOptions = listOf("All Rooms / Zones") + zones.map { it.name }
                        val currentSelection = spinnerLiveMapZoneFilter.selectedItemPosition.coerceAtMost(filterOptions.size - 1)
                        spinnerLiveMapZoneFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
                        spinnerLiveMapZoneFilter.setSelection(currentSelection)
                        updateMap()
                    }
                }
                launch {
                    viewModel.waypoints.collectLatest { waypoints ->
                        currentWaypoints = waypoints
                        updateMap()
                    }
                }
                launch {
                    viewModel.robots.collectLatest { robots ->
                        currentRobots = robots
                        updateMap()
                    }
                }
            }
        }
    }

    private fun updateMap() {
        val filteredZones = if (selectedZoneIndex > 0 && currentZones.isNotEmpty()) {
            val selZone = currentZones.getOrNull(selectedZoneIndex - 1)
            if (selZone != null) listOf(selZone) else currentZones
        } else {
            currentZones
        }

        val filteredWaypoints = if (selectedZoneIndex > 0 && currentZones.isNotEmpty()) {
            val selZone = currentZones.getOrNull(selectedZoneIndex - 1)
            if (selZone != null) currentWaypoints.filter { it.zoneId == selZone.id } else currentWaypoints
        } else {
            currentWaypoints
        }

        val filteredRobots = if (selectedZoneIndex > 0 && currentZones.isNotEmpty()) {
            val selZone = currentZones.getOrNull(selectedZoneIndex - 1)
            if (selZone != null) currentRobots.filter { it.zoneId == selZone.id } else currentRobots
        } else {
            currentRobots
        }

        fleetLiveMapView.setData(filteredZones, filteredWaypoints, filteredRobots)
    }
}
