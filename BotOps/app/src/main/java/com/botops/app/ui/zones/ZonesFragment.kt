package com.botops.app.ui.zones

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.botops.app.R
import com.botops.app.data.database.BotOpsDatabase
import com.botops.app.data.entities.ZoneEntity
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.databinding.FragmentZonesBinding
import com.botops.app.viewmodel.BotOpsViewModel
import kotlinx.coroutines.launch

class ZonesFragment : Fragment(R.layout.fragment_zones) {

    private var _binding: FragmentZonesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BotOpsViewModel by activityViewModels {
        val database = BotOpsDatabase.getInstance(requireContext())
        val repository = BotOpsRepository(
            database.zoneDao(),
            database.waypointDao(),
            database.robotDao(),
            database.taskDao(),
            requireContext().applicationContext
        )
        BotOpsViewModel.Factory(repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentZonesBinding.bind(view)

        val adapter = ZonesAdapter(
            onEditClick = { zone -> showEditZoneDialog(zone) },
            onDeleteClick = { zone ->
                viewModel.deleteZone(zone.id)
                Toast.makeText(requireContext(), "Zone deleted", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewZones.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewZones.adapter = adapter

        binding.buttonAddZone.setOnClickListener {
            val name = binding.inputZoneName.text.toString().trim()
            val description = binding.inputZoneDescription.text.toString().trim()

            if (name.isNotEmpty()) {
                viewModel.insertZone(name, description)
                binding.inputZoneName.text.clear()
                binding.inputZoneDescription.text.clear()
                Toast.makeText(requireContext(), "Zone added successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a zone name", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.zones.collect { zonesList ->
                    adapter.submitList(zonesList)
                }
            }
        }
    }

    private fun showEditZoneDialog(zone: ZoneEntity) {
        // Since fragment_zones has EditTexts, let's create a simple dialog programmatically or inflate a dialog layout
        val inputName = EditText(requireContext()).apply {
            hint = "Zone Name"
            setText(zone.name)
            setPadding(40, 30, 40, 30)
        }
        val inputDesc = EditText(requireContext()).apply {
            hint = "Description"
            setText(zone.description)
            setPadding(40, 30, 40, 30)
        }
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(inputName)
            addView(inputDesc)
            setPadding(40, 20, 40, 20)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Zone")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = inputName.text.toString().trim()
                val desc = inputDesc.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.updateZone(zone.id, name, desc)
                    Toast.makeText(requireContext(), "Zone updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
