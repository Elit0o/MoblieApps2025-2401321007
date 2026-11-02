package com.botops.app.ui.waypoints

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerWaypoints = view.findViewById(R.id.recyclerWaypoints)
        fabAddWaypoint = view.findViewById(R.id.fabAddWaypoint)

        setupRecyclerView()
        observeWaypoints()

        fabAddWaypoint.setOnClickListener {
            val randomVal = (10..99).random()
            // Приемаме примерен zoneId = 1 (увери се, че имаш съществуваща зона или я подай според логиката си)
            viewModel.insertWaypoint("Waypoint $randomVal", randomVal.toDouble(), (randomVal * 1.5), 1L)
            Toast.makeText(requireContext(), "Waypoint added successfully", Toast.LENGTH_SHORT).show()
        }
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

    private fun observeWaypoints() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.waypoints.collectLatest { waypointsList ->
                    waypointsAdapter.submitList(waypointsList)
                }
            }
        }
    }
}