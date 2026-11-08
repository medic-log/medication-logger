package com.mediclog.medicationlogger.ui.refills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.PILLS_PER_WEEK
import com.mediclog.medicationlogger.data.Refill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** One line of the medication log. */
sealed interface RefillLogEntry {
    val key: String
    val date: LocalDate

    /**
     * A refill, the box it came from, and the most it could be edited up to.
     * [leftRange] is what its pills left can be edited to while its box stays valid; null without a box.
     */
    data class RefillEntry(
        val refill: Refill,
        val box: Box?,
        val maxAmount: Int,
        val leftRange: IntRange?,
    ) : RefillLogEntry {
        override val key get() = "refill-${refill.id}"
        override val date get() = refill.date
    }

    /**
     * Cards pasted together, each newest first: [refills] that belong together, either partial ones made one after
     * another on one day that add up to a full week or a single one, and the missed or delayed [doses] taken from them.
     */
    data class RefillGroupEntry(
        val refills: List<RefillEntry>,
        val doses: List<MissEntry> = emptyList(),
    ) : RefillLogEntry {
        val members: List<RefillLogEntry> get() = doses + refills
        override val key get() = "group-${refills.last().refill.id}"
        override val date get() = members.maxOf { it.date }
    }

    /** A missed or delayed dose. */
    data class MissEntry(val miss: MedicationMiss) : RefillLogEntry {
        override val key get() = "miss-${miss.id}"
        override val date get() = miss.date
    }
}

/** The medication log, newest first, and the pills in the weekly lot. */
data class RefillsUiState(val entries: List<RefillLogEntry> = emptyList(), val lotPills: Int = 0)

/** Lists refills, misses and delays, and registers misses and delays. */
class RefillsViewModel(private val repository: MedicationRepository, private val clock: Clock) : ViewModel() {
    val uiState: StateFlow<RefillsUiState> = repository.shownData
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(repository.shownData.value))

    fun logMiss(kind: MedicationMiss.Kind, time: LocalTime? = null) = repository.logMiss(kind, time)

    /** The time now, to the minute, that new and edited delays start from. */
    fun currentTime(): LocalTime = LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES)

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
            RefillLogEntry.RefillEntry(refill, box, maxAmount, leftRange)
        }
        val refillEntries = groupingFullWeeks(refills)
        // A dose is its day's pill, which comes before that day's refills as in lotPills, so it belongs to the last
        // refill made before its day; until there's one, it stands alone
        val dosesByRefill = data.misses.map { RefillLogEntry.MissEntry(it) }
            .groupBy { dose -> refillEntries.indexOfLast { it.date < dose.date } }
        val stacked = refillEntries.mapIndexed { index, entry ->
            val doses = dosesByRefill[index].orEmpty().asReversed()
            when {
                doses.isEmpty() -> entry
                entry is RefillLogEntry.RefillGroupEntry -> entry.copy(doses = doses)
                entry is RefillLogEntry.RefillEntry -> RefillLogEntry.RefillGroupEntry(listOf(entry), doses)
                else -> entry
            }
        }
        val alone = dosesByRefill[-1].orEmpty().sortedByDescending { it.miss.id }
        return RefillsUiState(
            entries = (alone + stacked.asReversed()).sortedByDescending { it.date },
            lotPills = data.lotPills(LocalDateTime.now(clock)),
        )
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
