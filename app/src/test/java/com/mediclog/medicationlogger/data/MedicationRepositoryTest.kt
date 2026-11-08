package com.mediclog.medicationlogger.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationRepositoryTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val store = FakeMedicationStore()
    private val draft = PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 1, 1)

    private fun repository() = MedicationRepository(store, clock, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `actions are dated by the clock`() {
        val repository = repository()

        repository.addPrescription(draft)

        assertEquals(today, repository.data.value.prescriptions.single().prescribedDate)
    }

    @Test
    fun `changes survive a restart`() {
        val first = repository()
        first.addPrescription(draft)
        first.buyRepetition(1)
        first.refillWeekly(1)
        first.logMiss(MedicationMiss.Kind.DELAYED)

        assertEquals(first.data.value, repository().data.value)
        assertEquals(23, repository().data.value.boxes.single().pillsLeft)
    }

    @Test
    fun `replacing records keeps the settings and tutorial progress`() {
        val settings = ReminderSettings(isPillEnabled = false)
        store.saved = MedicationData(reminderSettings = settings).finishTutorial()
        val repository = repository()
        repository.addPrescription(draft)
        val records = MedicationData().addPrescription(PrescriptionDraft("Test medication", "Dr Other", "Other", 7, 1, 1), today)

        repository.replaceRecords(records)

        assertEquals(records.prescriptions, repository.data.value.prescriptions)
        assertEquals(settings, repository.data.value.reminderSettings)
        assertTrue(repository.data.value.isTutorialDone)
        assertEquals(repository.data.value, store.saved)
    }

    @Test
    fun `a failed save is reported and cleared by the next good one`() {
        val repository = repository()

        store.isFailing = true
        repository.addPrescription(draft)
        assertTrue(repository.isSaveFailed.value)
        assertEquals(1, repository.data.value.prescriptions.size)

        store.isFailing = false
        repository.orderRepetition(1)
        assertFalse(repository.isSaveFailed.value)
        assertEquals(repository.data.value, store.saved)
    }

    @Test
    fun `settings without an offset take the device's without shifting`() {
        val repository = repository()

        assertNull(repository.followDeviceTimeZone())

        assertEquals(ReminderSettings(utcOffset = ZoneOffset.UTC), repository.data.value.reminderSettings)
    }

    @Test
    fun `a new time zone shifts the reminders to the same moments`() {
        val before = ReminderSettings(
            refillDay = DayOfWeek.SUNDAY,
            refillTime = LocalTime.of(20, 0),
            pillTime = LocalTime.of(7, 30),
            utcOffset = ZoneOffset.ofHours(9),
        )
        store.saved = MedicationData(reminderSettings = before)
        val repository = repository()

        assertEquals(before, repository.followDeviceTimeZone())

        val after = repository.data.value.reminderSettings
        assertEquals(LocalTime.of(22, 30), after.pillTime)
        assertEquals(DayOfWeek.SUNDAY, after.refillDay)
        assertEquals(LocalTime.of(11, 0), after.refillTime)
        assertEquals(ZoneOffset.UTC, after.utcOffset)
        assertEquals(repository.data.value, store.saved)
        assertNull(repository.followDeviceTimeZone())
    }

    @Test
    fun `a shift past midnight moves the refill to the day before`() {
        store.saved = MedicationData(
            reminderSettings = ReminderSettings(
                refillDay = DayOfWeek.MONDAY,
                refillTime = LocalTime.of(2, 0),
                utcOffset = ZoneOffset.ofHours(5),
            ),
        )
        val repository = repository()

        repository.followDeviceTimeZone()

        val after = repository.data.value.reminderSettings
        assertEquals(DayOfWeek.SUNDAY, after.refillDay)
        assertEquals(LocalTime.of(21, 0), after.refillTime)
    }

    @Test
    fun `a new time zone at the same offset changes nothing`() {
        val settings = ReminderSettings(utcOffset = ZoneOffset.UTC)
        store.saved = MedicationData(reminderSettings = settings)
        val inLondon = MedicationRepository(
            store,
            clock.withZone(ZoneId.of("Europe/London")),
            CoroutineScope(UnconfinedTestDispatcher()),
        )

        assertNull(inLondon.followDeviceTimeZone())

        assertEquals(settings, inLondon.data.value.reminderSettings)
    }

    @Test
    fun `a change to summer time shifts the reminders an hour later`() {
        store.saved = MedicationData(
            reminderSettings = ReminderSettings(pillTime = LocalTime.of(7, 30), utcOffset = ZoneOffset.ofHours(1)),
        )
        val summer = LocalDate.of(2026, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val inAmsterdam = MedicationRepository(
            store,
            Clock.fixed(summer, ZoneId.of("Europe/Amsterdam")),
            CoroutineScope(UnconfinedTestDispatcher()),
        )

        assertEquals(LocalTime.of(7, 30), inAmsterdam.followDeviceTimeZone()?.pillTime)

        assertEquals(LocalTime.of(8, 30), inAmsterdam.data.value.reminderSettings.pillTime)
        assertEquals(ZoneOffset.ofHours(2), inAmsterdam.data.value.reminderSettings.utcOffset)
    }
}
