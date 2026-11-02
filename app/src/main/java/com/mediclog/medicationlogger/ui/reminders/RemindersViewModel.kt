package com.mediclog.medicationlogger.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** The reminder configuration, and when each reminder applies. */
data class RemindersUiState(
    val settings: ReminderSettings = ReminderSettings(),
    val nextRefill: LocalDateTime = LocalDateTime.MIN,
    val nextPill: LocalDateTime = LocalDateTime.MIN,
    val pickupDate: LocalDate? = null,
    val runOutDate: LocalDate? = null,
    val pillsRemaining: Int = 0,
    val expiringBox: Box? = null,
)

/** Shows and changes the reminder configuration. */
class RemindersViewModel(private val repository: MedicationRepository, private val clock: Clock) : ViewModel() {
    val uiState: StateFlow<RemindersUiState> = repository.data
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(repository.data.value))

    fun updateSettings(settings: ReminderSettings) = repository.updateReminderSettings(settings)

    private fun toUiState(data: MedicationData) = RemindersUiState(
        settings = data.reminderSettings,
        nextRefill = ReminderCalculator.nextRefill(data.reminderSettings, LocalDateTime.now(clock)),
        nextPill = ReminderCalculator.nextPill(data.reminderSettings, LocalDateTime.now(clock)),
        pickupDate = ReminderCalculator.pickupDate(data),
        runOutDate = ReminderCalculator.runOutDate(data, LocalDate.now(clock)),
        pillsRemaining = ReminderCalculator.pillsRemaining(data),
        expiringBox = ReminderCalculator.expiringBox(data),
    )
}
