package com.botops.app.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.botops.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val inputHost = view.findViewById<EditText>(R.id.inputSimulatorHost)
        val inputPort = view.findViewById<EditText>(R.id.inputSimulatorPort)
        val buttonSaveNetwork = view.findViewById<MaterialButton>(R.id.buttonSaveNetwork)

        val sharedPreferences = requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        // Dark mode setup
        val isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, checked ->
            sharedPreferences.edit().putBoolean("is_dark_mode", checked).apply()
            if (checked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Network Config setup
        val savedHost = sharedPreferences.getString("simulator_host", "192.168.1.100")
        val savedPort = sharedPreferences.getInt("simulator_port", 8080)
        inputHost.setText(savedHost)
        inputPort.setText(savedPort.toString())

        buttonSaveNetwork.setOnClickListener {
            val host = inputHost.text.toString().trim()
            val port = inputPort.text.toString().toIntOrNull() ?: 8080

            if (host.isNotEmpty()) {
                sharedPreferences.edit()
                    .putString("simulator_host", host)
                    .putInt("simulator_port", port)
                    .apply()
                Toast.makeText(requireContext(), "Network configuration saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid IP address", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
