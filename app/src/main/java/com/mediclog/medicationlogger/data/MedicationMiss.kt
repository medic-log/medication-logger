package com.mediclog.medicationlogger.data

import java.time.LocalDate

/** A dose that was missed or taken late. */
data class MedicationMiss(
    val id: Long,
    val date: LocalDate,
    val kind: Kind,
) {
    /** What went wrong with the dose. */
    enum class Kind { MISSED, DELAYED }
}
