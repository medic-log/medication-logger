package com.mediclog.medicationlogger.data

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * The pills set out for the week's doses, as counted at [countedAt], or never counted when that's null.
 * Each pill time since then has taken one, so the count is worked out from the clock rather than stored daily.
 */
data class WeeklyLot(val pills: Int = 0, val countedAt: LocalDateTime? = null) {
    /** Pills left at [now], when a pill is taken every day at [pillTime]. */
    fun pillsAt(now: LocalDateTime, pillTime: LocalTime): Int =
        countedAt?.let { (pills - pillTimesBetween(it, now, pillTime)).coerceAtLeast(0) } ?: 0

    /** The lot counted again at [now], with [change] pills added. */
    fun adding(change: Int, now: LocalDateTime, pillTime: LocalTime): WeeklyLot =
        WeeklyLot(pills = (pillsAt(now, pillTime) + change).coerceAtLeast(0), countedAt = now)
}

/** How many times [pillTime] comes after [from] and by [to]. */
fun pillTimesBetween(from: LocalDateTime, to: LocalDateTime, pillTime: LocalTime): Int {
    val first = from.toLocalDate().atTime(pillTime).let { if (it.isAfter(from)) it else it.plusDays(1) }
    val last = to.toLocalDate().atTime(pillTime).let { if (it.isAfter(to)) it.minusDays(1) else it }
    return if (last.isBefore(first)) 0 else ChronoUnit.DAYS.between(first, last).toInt() + 1
}
