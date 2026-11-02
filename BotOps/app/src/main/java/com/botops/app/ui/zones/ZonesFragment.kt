package com.botops.app.ui.zones

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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

    private val viewModel: BotOpsViewModel by viewModels {
        val database = BotOpsDatabase.getInstance(requireContext())
        val repository = BotOpsRepository(
            database.zoneDao(),
            database.waypointDao(),
            database.robotDao(),
            database.taskDao()
        )
        BotOpsViewModel.Factory(repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentZonesBinding.bind(view)

        val adapter = ZonesAdapter { zone: ZoneEntity ->
            viewModel.deleteZone(zone.id)
            Toast.makeText(requireContext(), "Zone deleted", Toast.LENGTH_SHORT).show()
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}