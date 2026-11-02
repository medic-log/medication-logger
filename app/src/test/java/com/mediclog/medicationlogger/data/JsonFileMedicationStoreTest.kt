package com.mediclog.medicationlogger.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class JsonFileMedicationStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File get() = File(folder.root, "data.json")
    private val day = LocalDate.of(2026, 11, 2)

    @Test
    fun `no file means empty data`() {
        assertEquals(MedicationData(), JsonFileMedicationStore(file).load())
    }

    @Test
    fun `everything is read back as it was saved`() {
        val data = MedicationData(
            reminderSettings = ReminderSettings(
                isRefillEnabled = true,
                refillDay = DayOfWeek.FRIDAY,
                refillTime = LocalTime.of(7, 45),
                isPickupEnabled = true,
                pickupDaysAfterOrder = 5,
                isRunOutEnabled = true,
                runOutDaysBefore = 20,
                isPillEnabled = true,
                pillTime = LocalTime.of(21, 15),
            ),
        )
            .addPrescription(PrescriptionDraft("Dr \"Quote\"", "Pharmacy, ünï", 7, 1, 2), day)
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 2, 1), day)
            .buyRepetition(1, day)
            .orderRepetition(1, day)
            .buyRepetition(2, day)
            .refillWeekly(1, day)
            .saveAsExtra(3)
            .logMiss(MedicationMiss.Kind.MISSED, day)

        JsonFileMedicationStore(file).save(data)

        assertEquals(data, JsonFileMedicationStore(file).load())
    }

    @Test
    fun `a file saved before the pill reminder existed still loads`() {
        val data = MedicationData().addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 1, 1), day)
        JsonFileMedicationStore(file).save(data)
        val json = JSONObject(file.readText())
        json.getJSONObject("reminderSettings").apply {
            remove("isPillEnabled")
            remove("pillTime")
        }
        file.writeText(json.toString())

        assertEquals(data, JsonFileMedicationStore(file).load())
    }

    @Test
    fun `an unreadable file is kept aside instead of being overwritten`() {
        file.writeText("not json")

        assertEquals(MedicationData(), JsonFileMedicationStore(file).load())

        assertEquals("not json", File(file.path + ".unreadable").readText())
        assertTrue(!file.exists())
    }
}
