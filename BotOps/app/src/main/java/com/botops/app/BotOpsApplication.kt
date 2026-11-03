package com.botops.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp

class BotOpsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            val isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        } catch (e: Exception) {
            Log.e("BotOpsApplication", "Failed to apply theme", e)
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("BotOpsApplication", "Failed to initialize Firebase", e)
        }
    }
}
