package com.botops.app.ui.zones

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.botops.app.R
import com.botops.app.data.entities.ZoneEntity
import com.botops.app.data.repository.BotOpsRepository
import com.botops.app.databinding.FragmentZonesBinding
import com.botops.app.viewmodel.BotOpsViewModel
import kotlinx.coroutines.launch

class ZonesFragment : Fragment(R.layout.fragment_zones) {

    private var _binding: FragmentZonesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BotOpsViewModel by activityViewModels {
        val repository = BotOpsRepository.getInstance(requireContext())
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
            val minX = binding.inputMinX.text.toString().toDoubleOrNull() ?: 0.0
            val maxX = binding.inputMaxX.text.toString().toDoubleOrNull() ?: 100.0
            val minY = binding.inputMinY.text.toString().toDoubleOrNull() ?: 0.0
            val maxY = binding.inputMaxY.text.toString().toDoubleOrNull() ?: 100.0

            if (name.isNotEmpty()) {
                viewModel.insertZone(name, description, minX, maxX, minY, maxY)
                binding.inputZoneName.text.clear()
                binding.inputZoneDescription.text.clear()
                Toast.makeText(requireContext(), "Room / Zone mapped successfully", Toast.LENGTH_SHORT).show()
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
        val inputName = EditText(requireContext()).apply {
            hint = "Zone Name"
            setText(zone.name)
            setPadding(30, 20, 30, 20)
        }
        val inputDesc = EditText(requireContext()).apply {
            hint = "Description"
            setText(zone.description)
            setPadding(30, 20, 30, 20)
        }
        val inputMinX = EditText(requireContext()).apply {
            hint = "Min X"
            setText(zone.minX.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(30, 20, 30, 20)
        }
        val inputMaxX = EditText(requireContext()).apply {
            hint = "Max X"
            setText(zone.maxX.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(30, 20, 30, 20)
        }
        val inputMinY = EditText(requireContext()).apply {
            hint = "Min Y"
            setText(zone.minY.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(30, 20, 30, 20)
        }
        val inputMaxY = EditText(requireContext()).apply {
            hint = "MaxY"
            setText(zone.maxY.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(30, 20, 30, 20)
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputName)
            addView(inputDesc)
            addView(inputMinX)
            addView(inputMaxX)
            addView(inputMinY)
            addView(inputMaxY)
            setPadding(40, 20, 40, 20)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Room / Zone Parameters")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = inputName.text.toString().trim()
                val desc = inputDesc.text.toString().trim()
                val minX = inputMinX.text.toString().toDoubleOrNull() ?: 0.0
                val maxX = inputMaxX.text.toString().toDoubleOrNull() ?: 100.0
                val minY = inputMinY.text.toString().toDoubleOrNull() ?: 0.0
                val maxY = inputMaxY.text.toString().toDoubleOrNull() ?: 100.0

                if (name.isNotEmpty()) {
                    viewModel.updateZone(zone.id, name, desc, minX, maxX, minY, maxY)
                    Toast.makeText(requireContext(), "Zone parameters updated", Toast.LENGTH_SHORT).show()
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
