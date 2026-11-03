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
import java.time.YearMonth

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
                isExpiryEnabled = false,
                expiryDaysBefore = 30,
            ),
        )
            .addPrescription(PrescriptionDraft("Dr \"Quote\"", "Pharmacy, ünï", 7, 1, 2), day)
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 2, 1), day)
            .buyRepetition(1, day)
            .orderRepetition(1, day)
            .buyRepetition(2, day)
            .refillWeekly(1, day.atTime(21, 0))
            .saveAsExtra(3)
            .logMiss(MedicationMiss.Kind.MISSED, day.atTime(21, 0))

        JsonFileMedicationStore(file).save(data)

        assertEquals(data, JsonFileMedicationStore(file).load())
    }

    @Test
    fun `a file saved before the expiry reminder existed starts it on`() {
        JsonFileMedicationStore(file).save(MedicationData())
        val json = JSONObject(file.readText())
        json.getJSONObject("reminderSettings").apply {
            remove("isExpiryEnabled")
            remove("expiryDaysBefore")
        }
        file.writeText(json.toString())

        assertEquals(MedicationData(), JsonFileMedicationStore(file).load())
    }

    @Test
    fun `open and expiry dates load back`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 2, 1), day)
            .buyRepetition(1, day, listOf(YearMonth.of(2030, 1), YearMonth.of(2031, 6)))
            .refillWeekly(1, day.atTime(21, 0))

        JsonFileMedicationStore(file).save(data)

        assertEquals(data, JsonFileMedicationStore(file).load())
    }

    @Test
    fun `an expiry saved as a full date loads as its year and month`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 1, 1), day)
            .buyRepetition(1, day, listOf(YearMonth.of(2030, 1)))
        JsonFileMedicationStore(file).save(data)
        val json = JSONObject(file.readText())
        json.getJSONArray("boxes").getJSONObject(0).put("expiryDate", "2030-01-31")
        file.writeText(json.toString())

        assertEquals(data, JsonFileMedicationStore(file).load())
    }

    @Test
    fun `boxes and refills that lost their links load back without them`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 30, 2, 1), day)
            .buyRepetition(1, day)
            .refillWeekly(1, day.atTime(21, 0))
            .refillWeekly(2, day.atTime(21, 0))
            .deleteBox(1, keepRefills = true)
            .deletePrescription(1, keepBoxes = true)

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
    fun `a file saved before the weekly lot existed counts it from the last refills`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 10, 2, 1), day)
            .buyRepetition(1, day)
            .refillWeekly(1, day.atTime(21, 0))
            .refillWeekly(1, day.atTime(21, 0))
        JsonFileMedicationStore(file).save(data)
        val json = JSONObject(file.readText())
        json.remove("weeklyLot")
        file.writeText(json.toString())

        assertEquals(WeeklyLot(3, day.atTime(LocalTime.MAX)), JsonFileMedicationStore(file).load().weeklyLot)
    }

    @Test
    fun `an unreadable file is kept aside instead of being overwritten`() {
        file.writeText("not json")

        assertEquals(MedicationData(), JsonFileMedicationStore(file).load())

        assertEquals("not json", File(file.path + ".unreadable").readText())
        assertTrue(!file.exists())
    }
}
