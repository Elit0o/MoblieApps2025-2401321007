package com.botops.app.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.botops.app.R
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.viewmodel.BotOpsViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
        BotOpsViewModel.Factory(repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textTotalRobots = view.findViewById<TextView>(R.id.textTotalRobots)
        val textActiveIdleRobots = view.findViewById<TextView>(R.id.textActiveIdleRobots)
        val textTasksMetrics = view.findViewById<TextView>(R.id.textTasksMetrics)
        val textLowBatteryCount = view.findViewById<TextView>(R.id.textLowBatteryCount)
        val textActiveOperationsSummary = view.findViewById<TextView>(R.id.textActiveOperationsSummary)

        val buttonGoToRobots = view.findViewById<MaterialButton>(R.id.buttonGoToRobots)
        val buttonGoToTasks = view.findViewById<MaterialButton>(R.id.buttonGoToTasks)
        val buttonGoToWaypoints = view.findViewById<MaterialButton>(R.id.buttonGoToWaypoints)
        val buttonGoToLiveMap = view.findViewById<MaterialButton>(R.id.buttonGoToLiveMap)

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.dashboardFragment, false, saveState = true)
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .build()

        buttonGoToRobots.setOnClickListener {
            findNavController().navigate(R.id.robotsFragment, null, navOptions)
        }
        buttonGoToTasks.setOnClickListener {
            findNavController().navigate(R.id.tasksFragment, null, navOptions)
        }
        buttonGoToWaypoints.setOnClickListener {
            findNavController().navigate(R.id.waypointsFragment, null, navOptions)
        }
        buttonGoToLiveMap.setOnClickListener {
            findNavController().navigate(R.id.fleetMapFragment, null, navOptions)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.robots.collectLatest { robots ->
                        val total = robots.size
                        val active = robots.count { it.status.equals("Active", true) }
                        val idle = robots.count { it.status.equals("Idle", true) }
                        val lowBattery = robots.count { it.batteryLevel < BotOpsRepository.MIN_BATTERY_LEVEL }

                        textTotalRobots.text = total.toString()
                        textActiveIdleRobots.text = "$active / $idle"
                        textLowBatteryCount.text = lowBattery.toString()
                    }
                }
                launch {
                    viewModel.tasks.collectLatest { tasks ->
                        val activeTasks = tasks.count { it.status.equals("Active", true) }
                        val pendingTasks = tasks.count { it.status.equals("Pending", true) }

                        textTasksMetrics.text = "$activeTasks / $pendingTasks"
                    }
                }
                launch {
                    viewModel.tasksWithDetails.collectLatest { taskDetails ->
                        val activeTasks = taskDetails.filter { it.task.status.equals("Active", true) }
                        if (activeTasks.isNotEmpty()) {
                            val summary = activeTasks.joinToString("\n\n") { detail ->
                                val robotName = detail.robotName ?: "Robot #${detail.task.robotId}"
                                val battery = detail.robotBattery ?: 0
                                val title = detail.task.title
                                val progress = detail.task.progress
                                val waypoint = detail.waypointName ?: "Destination"
                                "• $robotName ($battery% Battery)\n  Executing: \"$title\"\n  Target: $waypoint ($progress%)"
                            }
                            textActiveOperationsSummary.text = summary
                        } else {
                            textActiveOperationsSummary.text = "No robots currently executing tasks."
                        }
                    }
                }
            }
        }
    }
}
