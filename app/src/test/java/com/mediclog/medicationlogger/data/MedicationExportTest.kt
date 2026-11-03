package com.mediclog.medicationlogger.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

// org.json is an Android class, stubbed out in plain JVM tests
@RunWith(RobolectricTestRunner::class)
class MedicationExportTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val records = MedicationData()
        .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 7, 1, 2), day)
        .buyRepetition(1, day)
        .refillWeekly(1, day.atTime(21, 0))
        .logMiss(MedicationMiss.Kind.MISSED, day.atTime(21, 0))
        .logMiss(MedicationMiss.Kind.DELAYED, day.atTime(22, 0), LocalTime.of(23, 30))

    @Test
    fun `records are read back as they were exported`() {
        assertEquals(records, MedicationExport.read(MedicationExport.write(records)))
    }

    @Test
    fun `settings and tutorial progress are left out`() {
        val data = records.copy(reminderSettings = ReminderSettings(isPillEnabled = false)).finishTutorial()

        val json = JSONObject(MedicationExport.write(data))

        assertFalse(json.has("reminderSettings"))
        assertFalse(json.has("isTutorialDone"))
    }

    @Test
    fun `text that isn't an export is rejected`() {
        assertNull(MedicationExport.read("not json"))
        assertNull(MedicationExport.read("{}"))
    }
}
