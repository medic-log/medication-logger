package com.mediclog.medicationlogger.data

/** What the user enters for a new prescription. */
data class PrescriptionDraft(
    val prescribedBy: String,
    val pharmacy: String,
    val pillsPerBox: Int,
    val boxesPerRepetition: Int,
    val repetitionsTotal: Int,
)
