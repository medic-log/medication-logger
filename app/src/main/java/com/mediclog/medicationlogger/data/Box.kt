package com.mediclog.medicationlogger.data

import java.time.LocalDate

/** A box of pills bought with a prescription; [prescriptionId] is null once that's deleted. */
data class Box(
    val id: Long,
    val prescriptionId: Long?,
    val boxNumber: Int,
    val boughtDate: LocalDate,
    val pillsLeft: Int,
    val pillsTotal: Int,
    val isExtra: Boolean = false,
) {
    /** How much of the box is used, or that it's saved as extra. */
    enum class Status { NOT_STARTED, IN_PROGRESS, FINISHED, EXTRA }

    val name: String get() = "$boughtDate #$boxNumber"

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
