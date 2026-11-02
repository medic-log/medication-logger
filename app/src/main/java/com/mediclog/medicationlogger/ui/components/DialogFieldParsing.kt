package com.mediclog.medicationlogger.ui.components

import java.time.LocalDate
import java.time.format.DateTimeParseException

// Keeps multiplied totals well within Int
private const val MAX_COUNT = 999

/** The field's count from 1 up, or null when it isn't one. */
fun String.positiveCountOrNull(): Int? = countOrNull()?.takeIf { it >= 1 }

/** The field's count from 0 up, or null when it isn't one. */
fun String.countOrNull(): Int? = trim().toIntOrNull()?.takeIf { it in 0..MAX_COUNT }

/** Whether the field is empty or a date. */
fun String.isOptionalDate(): Boolean = isBlank() || dateOrNull() != null

/** The field's date as year-month-day, or null when it isn't one. */
fun String.dateOrNull(): LocalDate? = try {
    LocalDate.parse(trim())
} catch (_: DateTimeParseException) {
    null
}
