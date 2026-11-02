package com.mediclog.medicationlogger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.mediclog.medicationlogger.ui.main.MainRoute
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** Hosts the Compose UI, and asks for the notification permission that reminders depend on. */
class MainActivity : ComponentActivity() {
    // The reminders tab checks the permission again when it resumes, so the answer needs no handling here
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicationLoggerTheme {
                MainRoute()
            }
        }
        if (savedInstanceState == null) askForNotificationsIfNeeded()
    }

    // Reminders start on, so no switch gets flipped to ask from. Android stops showing this after two denials
    private fun askForNotificationsIfNeeded() {
        val settings = (application as MedicationLoggerApplication).repository.data.value.reminderSettings
        if (
            settings.hasAnyEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
