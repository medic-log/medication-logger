package com.mediclog.medicationlogger.data

import java.time.DayOfWeek
import java.time.LocalTime

/** The user's reminder configuration. */
data class ReminderSettings(
    val isRefillEnabled: Boolean = false,
    val refillDay: DayOfWeek = DayOfWeek.SUNDAY,
    val refillTime: LocalTime = LocalTime.of(9, 0),
    val isPickupEnabled: Boolean = false,
    val pickupDaysAfterOrder: Int = 3,
    val isRunOutEnabled: Boolean = false,
    val runOutDaysBefore: Int = 14,
)
