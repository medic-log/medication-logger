package com.mediclog.medicationlogger.data

import java.time.LocalDate

/** A prescription, bought in repetitions of boxes. */
data class Prescription(
    val id: Long,
    val prescribedDate: LocalDate,
    val prescribedBy: String,
    val pharmacy: String,
    val pillsPerBox: Int,
    val boxesPerRepetition: Int,
    val repetitionsTotal: Int,
    val status: Status = Status.NOT_STARTED,
    val orderedDate: LocalDate? = null,
    val boughtFirstDate: LocalDate? = null,
    val boughtLastDate: LocalDate? = null,
    val repetitionsBought: Int = 0,
) {
    /** Where the prescription is in its life, from nothing bought to everything bought. */
    enum class Status { NOT_STARTED, ORDERED, IN_PROGRESS, FINISHED }

    val pillsPerRepetition: Int get() = pillsPerBox * boxesPerRepetition
    val boxesTotal: Int get() = boxesPerRepetition * repetitionsTotal
    val pillsTotal: Int get() = pillsPerRepetition * repetitionsTotal
    val repetitionsLeft: Int get() = repetitionsTotal - repetitionsBought
    val isActive: Boolean get() = status != Status.FINISHED
    val canOrder: Boolean get() = repetitionsLeft > 0 && status != Status.ORDERED
    val canBuy: Boolean get() = repetitionsLeft > 0
}
