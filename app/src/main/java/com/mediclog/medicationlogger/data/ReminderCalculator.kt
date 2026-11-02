package com.mediclog.medicationlogger.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Works out when reminders are due. Holds no state, so it's the same after a reboot or a missed alarm. */
object ReminderCalculator {
    // Pickup, run-out and expiry reminders have only a number of days, so they arrive at a fixed time
    private val DAILY_REMINDER_TIME = LocalTime.of(9, 0)

    /** Pills that can still be taken: those in non-extra boxes and in repetitions not bought yet. */
    fun pillsRemaining(data: MedicationData): Int =
        pillsBought(data) + data.prescriptions.sumOf { it.repetitionsLeft * it.pillsPerRepetition }

    /** The day the medication runs out at one pill a day, or null when nothing has been logged yet. */
    fun runOutDate(data: MedicationData, today: LocalDate): LocalDate? =
        coveredUntil(data, today)?.plusDays(pillsRemaining(data).toLong())

    /**
     * The day the pills already bought run out, or null when nothing has been logged yet. The run-out reminder
     * follows this date, as repetitions not bought can't be taken until someone fetches them.
     */
    fun boughtPillsRunOutDate(data: MedicationData, today: LocalDate): LocalDate? =
        coveredUntil(data, today)?.plusDays(pillsBought(data).toLong())

    private fun pillsBought(data: MedicationData): Int = data.boxes.filterNot { it.isExtra }.sumOf { it.pillsLeft }

    private fun coveredUntil(data: MedicationData, today: LocalDate): LocalDate? {
        val lastRefill = data.refills.lastOrNull()
        if (lastRefill == null && pillsRemaining(data) == 0) return null
        val refillEnd = lastRefill?.date?.plusDays((PILLS_PER_WEEK - data.pendingRefillAmount).toLong())
        return if (refillEnd != null && refillEnd.isAfter(today)) refillEnd else today
    }

    /** The next weekly refill reminder after [now]. */
    fun nextRefill(settings: ReminderSettings, now: LocalDateTime): LocalDateTime {
        val candidate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(settings.refillDay))
            .atTime(settings.refillTime)
        return if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
    }

    /** The next daily reminder to take a pill after [now]. */
    fun nextPill(settings: ReminderSettings, now: LocalDateTime): LocalDateTime {
        val candidate = now.toLocalDate().atTime(settings.pillTime)
        return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
    }

    /** The first day a pickup reminder is due, or null when nothing is ordered. */
    fun pickupDate(data: MedicationData): LocalDate? = data.prescriptions
        .mapNotNull { it.orderedDate }
        .minOrNull()
        ?.plusDays(data.reminderSettings.pickupDaysAfterOrder.toLong())

    /** The box with pills left that expires first, extra ones included, or null when none has an expiry date. */
    fun expiringBox(data: MedicationData): Box? = data.boxes
        .filter { it.pillsLeft > 0 && it.expiryDate != null }
        .minByOrNull { it.expiryDate ?: YearMonth.from(LocalDate.MAX) }

    /** When the alarm for [kind] should fire next, or null when it shouldn't be set. */
    fun nextTrigger(kind: ReminderKind, data: MedicationData, now: LocalDateTime): LocalDateTime? {
        val settings = data.reminderSettings
        return when (kind) {
            ReminderKind.REFILL -> if (settings.isRefillEnabled) nextRefill(settings, now) else null
            ReminderKind.PILL -> if (settings.isPillEnabled) nextPill(settings, now) else null
            ReminderKind.PICKUP, ReminderKind.RUN_OUT, ReminderKind.EXPIRY ->
                dailyReminderDate(kind, data, now.toLocalDate())?.let { nextDailyTrigger(it, now) }
        }
    }

    /** Whether a reminder of [kind] should be shown at [now], as data may have changed since its alarm was set. */
    fun isDue(kind: ReminderKind, data: MedicationData, now: LocalDateTime): Boolean = when (kind) {
        ReminderKind.REFILL -> data.reminderSettings.isRefillEnabled
        ReminderKind.PILL -> data.reminderSettings.isPillEnabled
        ReminderKind.PICKUP, ReminderKind.RUN_OUT, ReminderKind.EXPIRY ->
            dailyReminderDate(kind, data, now.toLocalDate())?.atTime(DAILY_REMINDER_TIME)?.isAfter(now) == false
    }

    private fun dailyReminderDate(kind: ReminderKind, data: MedicationData, today: LocalDate): LocalDate? {
        val settings = data.reminderSettings
        return when {
            kind == ReminderKind.PICKUP && settings.isPickupEnabled -> pickupDate(data)
            kind == ReminderKind.RUN_OUT && settings.isRunOutEnabled ->
                boughtPillsRunOutDate(data, today)?.minusDays(settings.runOutDaysBefore.toLong())
            // A month on the box means usable until that month ends
            kind == ReminderKind.EXPIRY && settings.isExpiryEnabled ->
                expiringBox(data)?.expiryDate?.atEndOfMonth()?.minusDays(settings.expiryDaysBefore.toLong())
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
