package com.mediclog.medicationlogger.data

import java.time.LocalDate

/** Pills moved from a box for the week's doses; a link is null once what it points to is deleted. */
data class Refill(
    val id: Long,
    val boxId: Long?,
    val prescriptionId: Long?,
    val date: LocalDate,
    val amount: Int,
    val amountRemaining: Int,
    val isPartial: Boolean = false,
)
