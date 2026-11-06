package com.mediclog.medicationlogger.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediclog.medicationlogger.BuildConfig
import com.mediclog.medicationlogger.MedicationLoggerApplication
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.LatestRelease
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.ui.boxes.BoxesViewModel
import com.mediclog.medicationlogger.ui.main.MainViewModel
import com.mediclog.medicationlogger.ui.prescriptions.PrescriptionsViewModel
import com.mediclog.medicationlogger.ui.refills.RefillsViewModel
import com.mediclog.medicationlogger.ui.reminders.RemindersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Creates the app's ViewModels with what they need from the application. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = application()
            val sampleDraft = PrescriptionDraft(
                medication = application.getString(R.string.tutorial_sample_medication),
                prescribedBy = application.getString(R.string.tutorial_sample_doctor),
                pharmacy = application.getString(R.string.tutorial_sample_pharmacy),
                pillsPerBox = 30,
                boxesPerRepetition = 2,
                repetitionsTotal = 3,
            )
            MainViewModel(application.repository, application.clock, sampleDraft) {
                // The debug build can't install the release, and its tests mustn't reach the network
                if (BuildConfig.DEBUG) {
                    null
                } else {
                    withContext(Dispatchers.IO) { LatestRelease.fetch(BuildConfig.SITE_URL) }
                        ?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
                }
            }
        }
        initializer { RemindersViewModel(application().repository, application().clock) }
        initializer { PrescriptionsViewModel(application().repository) }
        initializer { BoxesViewModel(application().repository, application().clock) }
        initializer { RefillsViewModel(application().repository, application().clock) }
    }

    private fun CreationExtras.application() = this[APPLICATION_KEY] as MedicationLoggerApplication
}
