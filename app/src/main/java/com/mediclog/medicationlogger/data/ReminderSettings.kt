package com.mediclog.medicationlogger.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The user's reminder configuration. Every reminder starts on. The times are at [utcOffset], the device's offset from
 * UTC when they were set, which is null only in files saved before it was kept; reminders then follow the device's.
 */
data class ReminderSettings(
    val isRefillEnabled: Boolean = true,
    val refillDay: DayOfWeek = DayOfWeek.SUNDAY,
    val refillTime: LocalTime = LocalTime.of(20, 0),
    val isPickupEnabled: Boolean = true,
    val pickupDaysAfterOrder: Int = 4,
    val isRunOutEnabled: Boolean = true,
    val runOutDaysBefore: Int = 14,
    val isPillEnabled: Boolean = true,
    val pillTime: LocalTime = LocalTime.of(8, 0),
    val isExpiryEnabled: Boolean = true,
    val expiryDaysBefore: Int = 60,
    val utcOffset: ZoneOffset? = null,
) {
    val hasAnyEnabled: Boolean
        get() = isRefillEnabled || isPickupEnabled || isRunOutEnabled || isPillEnabled || isExpiryEnabled

    /**
     * These settings at [offset], with the pill and refill reminders moved to the moments they had at [utcOffset], as
     * of [now]: a pill at 07:30 at UTC+01:00 is at 08:30 at UTC+02:00, after a move or a change to summer time.
     * Without a [utcOffset] the times stay as they are.
     */
    fun atOffset(offset: ZoneOffset, now: Instant): ReminderSettings {
        val from = utcOffset ?: return copy(utcOffset = offset)
        val localNow = LocalDateTime.ofInstant(now, from)
        val refill = ReminderCalculator.nextRefill(this, localNow).atOffset(from).withOffsetSameInstant(offset)
        val pill = ReminderCalculator.nextPill(this, localNow).atOffset(from).withOffsetSameInstant(offset)
        return copy(
            refillDay = refill.dayOfWeek,
            refillTime = refill.toLocalTime(),
            pillTime = pill.toLocalTime(),
            utcOffset = offset,
        )
    }
}
