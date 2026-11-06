package com.mediclog.medicationlogger.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
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
}
