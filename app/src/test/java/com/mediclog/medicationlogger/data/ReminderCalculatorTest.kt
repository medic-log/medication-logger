package com.mediclog.medicationlogger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderCalculatorTest {
    // A Monday
    private val today = LocalDate.of(2026, 11, 2)
    private val draft = PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 1, 2)

    @Test
    fun `refill reminder is next on the chosen weekday and time`() {
        val settings = ReminderSettings(
            isRefillEnabled = true,
            refillDay = DayOfWeek.WEDNESDAY,
            refillTime = LocalTime.of(8, 30),
        )

        assertEquals(
            LocalDateTime.of(2026, 11, 4, 8, 30),
            ReminderCalculator.nextRefill(settings, today.atTime(12, 0)),
        )
    }

    @Test
    fun `refill reminder moves to next week once today's time has passed`() {
        val settings = ReminderSettings(refillDay = DayOfWeek.MONDAY, refillTime = LocalTime.of(9, 0))

        assertEquals(today.atTime(9, 0), ReminderCalculator.nextRefill(settings, today.atTime(8, 0)))
        assertEquals(today.plusWeeks(1).atTime(9, 0), ReminderCalculator.nextRefill(settings, today.atTime(9, 0)))
    }

    @Test
    fun `pill reminder is next today until its time has passed, then tomorrow`() {
        val settings = ReminderSettings(isPillEnabled = true, pillTime = LocalTime.of(21, 30))
        val data = MedicationData(reminderSettings = settings)

        assertEquals(today.atTime(21, 30), ReminderCalculator.nextTrigger(ReminderKind.PILL, data, today.atTime(12, 0)))
        assertEquals(
            today.plusDays(1).atTime(21, 30),
            ReminderCalculator.nextTrigger(ReminderKind.PILL, data, today.atTime(21, 30)),
        )
        assertTrue(ReminderCalculator.isDue(ReminderKind.PILL, data, today.atTime(21, 30)))
    }

    @Test
    fun `disabled reminders aren't scheduled`() {
        val data = MedicationData(reminderSettings = AllRemindersOff)
            .addPrescription(draft, today)
            .orderRepetition(1, today)

        ReminderKind.entries.forEach { kind ->
            assertNull(ReminderCalculator.nextTrigger(kind, data, today.atTime(12, 0)))
            assertFalse(kind == ReminderKind.REFILL && ReminderCalculator.isDue(kind, data, today.atTime(12, 0)))
        }
    }

    @Test
    fun `pickup reminder fires the chosen days after ordering, then daily until bought`() {
        val settings = ReminderSettings(isPickupEnabled = true, pickupDaysAfterOrder = 3)
        val ordered = MedicationData(reminderSettings = settings).addPrescription(draft, today).orderRepetition(1, today)
        val dueDay = today.plusDays(3)

        assertEquals(
            dueDay.atTime(9, 0),
            ReminderCalculator.nextTrigger(ReminderKind.PICKUP, ordered, today.atTime(12, 0)),
        )
        assertFalse(ReminderCalculator.isDue(ReminderKind.PICKUP, ordered, today.atTime(12, 0)))
        assertTrue(ReminderCalculator.isDue(ReminderKind.PICKUP, ordered, dueDay.atTime(9, 0)))
        assertEquals(
            dueDay.plusDays(1).atTime(9, 0),
            ReminderCalculator.nextTrigger(ReminderKind.PICKUP, ordered, dueDay.atTime(9, 0)),
        )

        val bought = ordered.buyRepetition(1, dueDay)
        assertNull(ReminderCalculator.nextTrigger(ReminderKind.PICKUP, bought, dueDay.atTime(9, 0)))
        assertFalse(ReminderCalculator.isDue(ReminderKind.PICKUP, bought, dueDay.atTime(9, 0)))
    }

    @Test
    fun `nothing logged has no run-out date`() {
        assertNull(ReminderCalculator.runOutDate(MedicationData(), today))
    }

    @Test
    fun `run-out date counts one pill a day, including repetitions not bought`() {
        val data = MedicationData().addPrescription(draft, today).buyRepetition(1, today)

        assertEquals(60, ReminderCalculator.pillsRemaining(data))
        assertEquals(today.plusDays(60), ReminderCalculator.runOutDate(data, today))
    }

    @Test
    fun `run-out date starts after the days the last refill covers`() {
        val data = MedicationData().addPrescription(draft, today).buyRepetition(1, today).refillWeekly(1, today)

        assertEquals(53, ReminderCalculator.pillsRemaining(data))
        assertEquals(today.plusDays(7 + 53), ReminderCalculator.runOutDate(data, today))
    }

    @Test
    fun `an old refill no longer covers any days`() {
        val data = MedicationData().addPrescription(draft, today).buyRepetition(1, today).refillWeekly(1, today)
        val later = today.plusDays(10)

        assertEquals(later.plusDays(53), ReminderCalculator.runOutDate(data, later))
    }

    @Test
    fun `extra boxes don't count towards the run-out date`() {
        val data = MedicationData().addPrescription(draft, today).buyRepetition(1, today).saveAsExtra(1)

        assertEquals(30, ReminderCalculator.pillsRemaining(data))
    }

    @Test
    fun `run-out reminder fires the chosen days before the bought pills run out`() {
        val settings = ReminderSettings(isRunOutEnabled = true, runOutDaysBefore = 14)
        val data = MedicationData(reminderSettings = settings).addPrescription(draft, today).buyRepetition(1, today)
        val dueDay = today.plusDays(30 - 14)

        assertEquals(
            dueDay.atTime(9, 0),
            ReminderCalculator.nextTrigger(ReminderKind.RUN_OUT, data, today.atTime(12, 0)),
        )
        assertFalse(ReminderCalculator.isDue(ReminderKind.RUN_OUT, data, today.atTime(12, 0)))
    }

    @Test
    fun `run-out reminder is due daily once the date is close`() {
        val settings = ReminderSettings(isRunOutEnabled = true, runOutDaysBefore = 14)
        val data = MedicationData(reminderSettings = settings)
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 7, 1, 1), today)

        val now = today.atTime(10, 0)
        assertTrue(ReminderCalculator.isDue(ReminderKind.RUN_OUT, data, now))
        assertEquals(today.plusDays(1).atTime(9, 0), ReminderCalculator.nextTrigger(ReminderKind.RUN_OUT, data, now))
    }
}
