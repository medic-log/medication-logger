package com.mediclog.medicationlogger.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** Works out when reminders are due. Holds no state, so it's the same after a reboot or a missed alarm. */
object ReminderCalculator {
    // The concept gives pickup and run-out reminders only a number of days, so they arrive at a fixed time
    private val DAILY_REMINDER_TIME = LocalTime.of(9, 0)

    /** Pills that can still be taken: those in non-extra boxes and in repetitions not bought yet. */
    fun pillsRemaining(data: MedicationData): Int =
        data.boxes.filterNot { it.isExtra }.sumOf { it.pillsLeft } +
            data.prescriptions.sumOf { it.repetitionsLeft * it.pillsPerRepetition }

    /** The day the medication runs out at one pill a day, or null when nothing has been logged yet. */
    fun runOutDate(data: MedicationData, today: LocalDate): LocalDate? {
        val lastRefill = data.refills.lastOrNull()
        val pills = pillsRemaining(data)
        if (lastRefill == null && pills == 0) return null
        val coveredUntil = lastRefill?.date?.plusDays((PILLS_PER_WEEK - data.pendingRefillAmount).toLong())
        val start = if (coveredUntil != null && coveredUntil.isAfter(today)) coveredUntil else today
        return start.plusDays(pills.toLong())
    }

    /** The next weekly refill reminder after [now]. */
    fun nextRefill(settings: ReminderSettings, now: LocalDateTime): LocalDateTime {
        val candidate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(settings.refillDay))
            .atTime(settings.refillTime)
        return if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
    }

    /** The first day a pickup reminder is due, or null when nothing is ordered. */
    fun pickupDate(data: MedicationData): LocalDate? = data.prescriptions
        .mapNotNull { it.orderedDate }
        .minOrNull()
        ?.plusDays(data.reminderSettings.pickupDaysAfterOrder.toLong())

    /** When the alarm for [kind] should fire next, or null when it shouldn't be set. */
    fun nextTrigger(kind: ReminderKind, data: MedicationData, now: LocalDateTime): LocalDateTime? {
        val settings = data.reminderSettings
        return when (kind) {
            ReminderKind.REFILL -> if (settings.isRefillEnabled) nextRefill(settings, now) else null
            ReminderKind.PICKUP, ReminderKind.RUN_OUT -> dailyReminderDate(kind, data, now.toLocalDate())
                ?.let { nextDailyTrigger(it, now) }
        }
    }

    /** Whether a reminder of [kind] should be shown at [now], as data may have changed since its alarm was set. */
    fun isDue(kind: ReminderKind, data: MedicationData, now: LocalDateTime): Boolean = when (kind) {
        ReminderKind.REFILL -> data.reminderSettings.isRefillEnabled
        ReminderKind.PICKUP, ReminderKind.RUN_OUT -> dailyReminderDate(kind, data, now.toLocalDate())
            ?.atTime(DAILY_REMINDER_TIME)
            ?.isAfter(now) == false
    }

    private fun dailyReminderDate(kind: ReminderKind, data: MedicationData, today: LocalDate): LocalDate? {
        val settings = data.reminderSettings
        return when {
            kind == ReminderKind.PICKUP && settings.isPickupEnabled -> pickupDate(data)
            kind == ReminderKind.RUN_OUT && settings.isRunOutEnabled ->
                runOutDate(data, today)?.minusDays(settings.runOutDaysBefore.toLong())
            else -> null
        }
    }

    // Once due, these repeat daily until resolved
    private fun nextDailyTrigger(date: LocalDate, now: LocalDateTime): LocalDateTime {
        val first = date.atTime(DAILY_REMINDER_TIME)
        if (first.isAfter(now)) return first
        val today = now.toLocalDate().atTime(DAILY_REMINDER_TIME)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}
