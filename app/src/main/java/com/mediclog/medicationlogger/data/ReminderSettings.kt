package com.mediclog.medicationlogger.data

import java.time.DayOfWeek
import java.time.LocalTime

/** The user's reminder configuration. Every reminder starts on. */
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
) {
    val hasAnyEnabled: Boolean get() = isRefillEnabled || isPickupEnabled || isRunOutEnabled || isPillEnabled
}
