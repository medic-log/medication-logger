package com.mediclog.medicationlogger.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediclog.medicationlogger.MedicationLoggerApplication
import com.mediclog.medicationlogger.ui.boxes.BoxesViewModel
import com.mediclog.medicationlogger.ui.main.MainViewModel
import com.mediclog.medicationlogger.ui.prescriptions.PrescriptionsViewModel
import com.mediclog.medicationlogger.ui.refills.RefillsViewModel
import com.mediclog.medicationlogger.ui.reminders.RemindersViewModel

/** Creates the app's ViewModels with what they need from the application. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { MainViewModel(application().repository) }
        initializer { RemindersViewModel(application().repository, application().clock) }
        initializer { PrescriptionsViewModel(application().repository) }
        initializer { BoxesViewModel(application().repository) }
        initializer { RefillsViewModel(application().repository) }
    }

    private fun CreationExtras.application() = this[APPLICATION_KEY] as MedicationLoggerApplication
}
