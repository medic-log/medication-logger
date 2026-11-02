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

    /**
     * A refill, the name and number of the box it came from, and the most it could be edited up to.
     * [leftRange] is what its pills left can be edited to while its box stays valid; null without a box.
     */
    data class RefillEntry(
        val refill: Refill,
        val boxName: String,
        val boxNumber: Int?,
        val maxAmount: Int,
        val leftRange: IntRange?,
    ) : RefillLogEntry {
        override val key get() = "refill-${refill.id}"
        override val date get() = refill.date
    }

    /**
     * Cards pasted together, each newest first: [refills] that belong together, either partial ones made one after
     * another on one day that add up to a full week or a single one, and the [missedDoses] since then.
     */
    data class RefillGroupEntry(
        val refills: List<RefillEntry>,
        val missedDoses: List<MissEntry> = emptyList(),
    ) : RefillLogEntry {
        val members: List<RefillLogEntry> get() = missedDoses + refills
        override val key get() = "group-${refills.last().refill.id}"
        override val date get() = members.maxOf { it.date }
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
            val leftRange = box?.let {
                (refill.amountRemaining - it.pillsLeft).coerceAtLeast(0)..
                    refill.amountRemaining + it.pillsTotal - it.pillsLeft
            }
            RefillLogEntry.RefillEntry(refill, box?.name.orEmpty(), box?.boxNumber, maxAmount, leftRange)
        }
        val refillEntries = groupingFullWeeks(refills)
        val (missed, others) = data.misses.map { RefillLogEntry.MissEntry(it) }
            .partition { it.miss.kind == MedicationMiss.Kind.MISSED }
        // A missed dose belongs to the last refill made by then; before any refill, it stands alone
        val missedByRefill = missed.groupBy { dose -> refillEntries.indexOfLast { it.date <= dose.date } }
        val stacked = refillEntries.mapIndexed { index, entry ->
            val doses = missedByRefill[index].orEmpty().asReversed()
            when {
                doses.isEmpty() -> entry
                entry is RefillLogEntry.RefillGroupEntry -> entry.copy(missedDoses = doses)
                entry is RefillLogEntry.RefillEntry -> RefillLogEntry.RefillGroupEntry(listOf(entry), doses)
                else -> entry
            }
        }
        val alone = (others + missedByRefill[-1].orEmpty()).sortedByDescending { it.miss.id }
        return RefillsUiState((alone + stacked.asReversed()).sortedByDescending { it.date })
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
