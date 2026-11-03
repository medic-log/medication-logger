package com.mediclog.medicationlogger.data

import java.time.LocalDate
import java.time.LocalTime

/** A dose that was missed or taken late; a delayed one keeps the [time] it was taken, if known. */
data class MedicationMiss(
    val id: Long,
    val date: LocalDate,
    val kind: Kind,
    val time: LocalTime? = null,
) {
    /** What went wrong with the dose. */
    enum class Kind { MISSED, DELAYED }

    val isMissed: Boolean get() = kind == Kind.MISSED
}
