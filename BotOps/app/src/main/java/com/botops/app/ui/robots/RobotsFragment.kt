package com.botops.app.ui.robots

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
import com.botops.app.data.entities.RobotWithZone
import com.botops.app.data.entities.ZoneEntity
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.viewmodel.BotOpsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RobotsFragment : Fragment(R.layout.fragment_robots) {

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
        BotOpsViewModel.Factory(repository)
    }

    private lateinit var robotsAdapter: RobotsAdapter
    private lateinit var recyclerRobots: RecyclerView
    private lateinit var fabAddRobot: FloatingActionButton
    private lateinit var spinnerZoneFilter: Spinner

    private var cachedZones = listOf<ZoneEntity>()
    private var cachedRobotsWithZone = listOf<RobotWithZone>()
    private var selectedZoneFilterIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerRobots = view.findViewById(R.id.recyclerRobots)
        fabAddRobot = view.findViewById(R.id.fabAddRobot)
        spinnerZoneFilter = view.findViewById(R.id.spinnerZoneFilter)

        spinnerZoneFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedZoneFilterIndex = position
                filterAndSubmitRobots()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupRecyclerView()
        observeData()

        fabAddRobot.setOnClickListener {
            showAddRobotDialog()
        }
    }

    private fun filterAndSubmitRobots() {
        var filtered = cachedRobotsWithZone
        if (selectedZoneFilterIndex > 0 && cachedZones.isNotEmpty()) {
            val selectedZone = cachedZones.getOrNull(selectedZoneFilterIndex - 1)
            filtered = filtered.filter { it.robot.zoneId == selectedZone?.id }
        }
        robotsAdapter.submitList(filtered)
    }

    private fun showAddRobotDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_robot, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputRobotName)
        val inputNumber = dialogView.findViewById<EditText>(R.id.inputRobotNumber)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerRobotStatus)
        val sliderBattery = dialogView.findViewById<Slider>(R.id.sliderBattery)
        val textBatteryVal = dialogView.findViewById<TextView>(R.id.textBatteryLabel)
        val spinnerZone = dialogView.findViewById<Spinner>(R.id.spinnerZone)

        val statuses = listOf("Idle", "Active", "Charging", "Maintenance")
        spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses)

        sliderBattery.addOnChangeListener { _, value, _ ->
            textBatteryVal.text = "Battery Level: ${value.toInt()}%"
        }

        val zoneNames = listOf("No Zone (Unassigned)") + cachedZones.map { it.name }
        spinnerZone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, zoneNames)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Robot")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = inputName.text.toString().trim()
                val number = inputNumber.text.toString().trim()
                val status = statuses[spinnerStatus.selectedItemPosition]
                val battery = sliderBattery.value.toInt()
                val zonePos = spinnerZone.selectedItemPosition
                val zoneId = if (zonePos > 0) cachedZones[zonePos - 1].id else null

                if (name.isNotEmpty() && number.isNotEmpty()) {
                    viewModel.insertRobot(name, number, status, battery, zoneId)
                    Toast.makeText(requireContext(), "Robot added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Name and number cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditRobotDialog(item: RobotWithZone) {
        val robot = item.robot
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_robot, null)
        val inputName = dialogView.findViewById<EditText>(R.id.inputRobotName)
        val inputNumber = dialogView.findViewById<EditText>(R.id.inputRobotNumber)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerRobotStatus)
        val sliderBattery = dialogView.findViewById<Slider>(R.id.sliderBattery)
        val textBatteryVal = dialogView.findViewById<TextView>(R.id.textBatteryLabel)
        val spinnerZone = dialogView.findViewById<Spinner>(R.id.spinnerZone)

        inputName.setText(robot.robotName)
        inputNumber.setText(robot.robotNumber)
        sliderBattery.value = robot.batteryLevel.toFloat()
        textBatteryVal.text = "Battery Level: ${robot.batteryLevel}%"

        val statuses = listOf("Idle", "Active", "Charging", "Maintenance")
        spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses)
        val statusIndex = statuses.indexOf(robot.status)
        if (statusIndex >= 0) spinnerStatus.setSelection(statusIndex)

        val zoneNames = listOf("No Zone (Unassigned)") + cachedZones.map { it.name }
        spinnerZone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, zoneNames)
        val zoneIndex = cachedZones.indexOfFirst { it.id == robot.zoneId }
        if (zoneIndex >= 0) spinnerZone.setSelection(zoneIndex + 1)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Robot")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = inputName.text.toString().trim()
                val number = inputNumber.text.toString().trim()
                val status = statuses[spinnerStatus.selectedItemPosition]
                val battery = sliderBattery.value.toInt()
                val zonePos = spinnerZone.selectedItemPosition
                val zoneId = if (zonePos > 0) cachedZones[zonePos - 1].id else null

                if (name.isNotEmpty() && number.isNotEmpty()) {
                    viewModel.updateRobot(robot.id, name, number, status, battery, zoneId)
                    Toast.makeText(requireContext(), "Robot updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Name and number cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        robotsAdapter = RobotsAdapter(
            onEditClick = { item -> showEditRobotDialog(item) },
            onDeleteClick = { robot ->
                viewModel.deleteRobot(robot.id)
                Toast.makeText(requireContext(), "Robot deleted", Toast.LENGTH_SHORT).show()
            }
        )

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
                        cachedRobotsWithZone = robotsList
                        filterAndSubmitRobots()
                    }
                }
                launch {
                    viewModel.zones.collectLatest { zonesList ->
                        cachedZones = zonesList
                        val filterOptions = listOf("All Zones") + zonesList.map { it.name }
                        val currentSelection = spinnerZoneFilter.selectedItemPosition.coerceAtMost(filterOptions.size - 1)
                        spinnerZoneFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
                        spinnerZoneFilter.setSelection(currentSelection)
                        filterAndSubmitRobots()
                    }
                }
            }
        }
    }
}
