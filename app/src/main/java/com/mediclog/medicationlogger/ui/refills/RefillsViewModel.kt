package com.mediclog.medicationlogger.ui.refills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.PILLS_PER_WEEK
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

    /** A refill, the name and number of the box it came from, and the most it could be edited up to. */
    data class RefillEntry(
        val refill: Refill,
        val boxName: String,
        val boxNumber: Int?,
        val maxAmount: Int,
    ) : RefillLogEntry {
        override val key get() = "refill-${refill.id}"
        override val date get() = refill.date
    }

    /** Partial refills made one after another on one day that add up to a full week, newest first. */
    data class RefillGroupEntry(val refills: List<RefillEntry>) : RefillLogEntry {
        override val key get() = "group-${refills.last().refill.id}"
        override val date get() = refills.first().date
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

    fun editRefill(updated: Refill) = repository.editRefill(updated)

    fun deleteRefill(refillId: Long) = repository.deleteRefill(refillId)

    fun editMiss(updated: MedicationMiss) = repository.editMiss(updated)

    fun deleteMiss(missId: Long) = repository.deleteMiss(missId)

    private fun toUiState(data: MedicationData): RefillsUiState {
        val boxes = data.boxes.associateBy { it.id }
        val refills = data.refills.map { refill ->
            val box = boxes[refill.boxId]
            val maxAmount = refill.amount + (box?.pillsLeft ?: 0)
            RefillLogEntry.RefillEntry(refill, box?.name.orEmpty(), box?.boxNumber, maxAmount)
        }
        val misses = data.misses.asReversed().map { RefillLogEntry.MissEntry(it) }
        return RefillsUiState((misses + groupingFullWeeks(refills).asReversed()).sortedByDescending { it.date })
    }

    private fun groupingFullWeeks(refills: List<RefillLogEntry.RefillEntry>): List<RefillLogEntry> {
        val entries = mutableListOf<RefillLogEntry>()
        var start = 0
        while (start < refills.size) {
            val week = fullWeekFrom(refills, start)
            if (week == null) {
                entries += refills[start]
                start++
            } else {
                entries += RefillLogEntry.RefillGroupEntry(week.asReversed())
                start += week.size
            }
        }
        return entries
    }

    private fun fullWeekFrom(
        refills: List<RefillLogEntry.RefillEntry>,
        start: Int,
    ): List<RefillLogEntry.RefillEntry>? {
        var pills = 0
        for (end in start until refills.size) {
            val refill = refills[end].refill
            if (!refill.isPartial || refill.date != refills[start].date) return null
            pills += refill.amount
            if (pills == PILLS_PER_WEEK) return refills.subList(start, end + 1).takeIf { it.size > 1 }
            if (pills > PILLS_PER_WEEK) return null
        }
        return null
    }
}
