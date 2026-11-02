package com.mediclog.medicationlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mediclog.medicationlogger.ui.main.MainRoute
import com.mediclog.medicationlogger.ui.theme.MedicationLoggerTheme

/** Hosts the Compose UI. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicationLoggerTheme {
                MainRoute()
            }
        }
    }
}
