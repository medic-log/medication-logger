package com.mediclog.medicationlogger.ui.refills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.Refill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** One line of the medication log. */
sealed interface RefillLogEntry {
    val key: String
    val date: LocalDate

    /** A refill and the name and number of the box it came from. */
    data class RefillEntry(val refill: Refill, val boxName: String, val boxNumber: Int?) : RefillLogEntry {
        override val key get() = "refill-${refill.id}"
        override val date get() = refill.date
    }

    /** A missed or delayed dose. */
    data class MissEntry(val miss: MedicationMiss) : RefillLogEntry {
        override val key get() = "miss-${miss.id}"
        override val date get() = miss.date
    }
}

/** The medication log, newest first. */
data class RefillsUiState(val entries: List<RefillLogEntry> = emptyList())

/** Lists refills, misses and delays, and registers misses and delays. */
class RefillsViewModel(private val repository: MedicationRepository) : ViewModel() {
    val uiState: StateFlow<RefillsUiState> = repository.data
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(repository.data.value))

    fun logMiss(kind: MedicationMiss.Kind) = repository.logMiss(kind)

    private fun toUiState(data: MedicationData): RefillsUiState {
        val boxes = data.boxes.associateBy { it.id }
        val refills = data.refills.asReversed().map { refill ->
            val box = boxes[refill.boxId]
            RefillLogEntry.RefillEntry(refill, box?.name.orEmpty(), box?.boxNumber)
        }
        val misses = data.misses.asReversed().map { RefillLogEntry.MissEntry(it) }
        return RefillsUiState((misses + refills).sortedByDescending { it.date })
    }
}
