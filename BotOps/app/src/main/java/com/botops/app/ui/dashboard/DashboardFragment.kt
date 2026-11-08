package com.botops.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
        BotOpsViewModel.Factory(repository)
    }

    private var cachedTotalRobots = 0
    private var cachedActiveRobots = 0
    private var cachedIdleRobots = 0
    private var cachedActiveTasks = 0

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
        val fabShareFleetReport = view.findViewById<FloatingActionButton>(R.id.fabShareFleetReport)

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

        fabShareFleetReport.setOnClickListener {
            val timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString()
            val shareBody = """
                BotOps Fleet Status Report
                ---------------------------------
                • Total Robots: $cachedTotalRobots
                • Active Robots: $cachedActiveRobots
                • Idle Robots: $cachedIdleRobots
                • Active Tasks: $cachedActiveTasks
                • Fleet Health: Optimal
                • Timestamp: $timestamp
            """.trimIndent()

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "BotOps Fleet Status Report")
                putExtra(Intent.EXTRA_TEXT, shareBody)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Fleet Report via"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.robots.collectLatest { robots ->
                        cachedTotalRobots = robots.size
                        cachedActiveRobots = robots.count { it.status.equals("Active", true) }
                        cachedIdleRobots = robots.count { it.status.equals("Idle", true) }
                        val lowBattery = robots.count { it.batteryLevel < BotOpsRepository.MIN_BATTERY_LEVEL }

                        textTotalRobots.text = cachedTotalRobots.toString()
                        textActiveIdleRobots.text = "$cachedActiveRobots / $cachedIdleRobots"
                        textLowBatteryCount.text = lowBattery.toString()
                    }
                }
                launch {
                    viewModel.tasks.collectLatest { tasks ->
                        cachedActiveTasks = tasks.count { it.status.equals("Active", true) }
                        val pendingTasks = tasks.count { it.status.equals("Pending", true) }

                        textTasksMetrics.text = "$cachedActiveTasks / $pendingTasks"
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
