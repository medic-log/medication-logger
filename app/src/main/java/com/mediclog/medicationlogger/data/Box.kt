package com.mediclog.medicationlogger.data

import java.time.LocalDate
import java.time.YearMonth

/**
 * A box of pills bought with a prescription; [prescriptionId] is null once that's deleted.
 * [notes] say what an extra box is kept for, and are cleared when it goes back to the inventory.
 */
data class Box(
    val id: Long,
    val prescriptionId: Long?,
    val boxNumber: Int,
    val boughtDate: LocalDate,
    val pillsLeft: Int,
    val pillsTotal: Int,
    val isExtra: Boolean = false,
    val openDate: LocalDate? = null,
    val expiryDate: YearMonth? = null,
    val notes: String = "",
) {
    /** How much of the box is used, or that it's saved as extra. */
    enum class Status { NOT_STARTED, IN_PROGRESS, FINISHED, EXTRA }

    val status: Status
        get() = when {
            isExtra -> Status.EXTRA
            pillsLeft == 0 -> Status.FINISHED
            pillsLeft == pillsTotal -> Status.NOT_STARTED
            else -> Status.IN_PROGRESS
        }

    val isActive: Boolean get() = status == Status.NOT_STARTED || status == Status.IN_PROGRESS
    val canRefill: Boolean get() = isActive
    val canSaveAsExtra: Boolean get() = isActive
}
