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
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

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
    fun `a save interrupted after setting the file aside as a backup loads the backup`() {
        val data = MedicationData().addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 2, 1), day)
        JsonFileMedicationStore(file).save(data)
        // What Android 9 and 10 leave when the app is killed between renaming the file and writing the new one
        file.renameTo(File(file.path + ".bak"))

        assertEquals(data, JsonFileMedicationStore(file).load())
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
                utcOffset = ZoneOffset.ofHoursMinutes(5, 30),
            ),
        )
            .addPrescription(PrescriptionDraft("Ibuprofen \"600\" mg", "Dr \"Quote\"", "Pharmacy, ünï", 7, 1, 2), day)
            .addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 2, 1), day)
            .buyRepetition(1, day)
            .orderRepetition(1, day)
            .buyRepetition(2, day)
            .refillWeekly(1, day.atTime(21, 0))
            .saveAsExtra(3)
            .setBoxNotes(3, "For travel, \"spare\"")
            .logMiss(MedicationMiss.Kind.MISSED, day.atTime(21, 0))
            .finishTutorial()
            .copy(dateStyle = DateStyle.LANGUAGE)

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
    fun `a file saved before the date style could be chosen shows year-month-day`() {
        JsonFileMedicationStore(file).save(MedicationData(dateStyle = DateStyle.MONTH_DAY_YEAR))
        file.writeText(JSONObject(file.readText()).apply { remove("dateStyle") }.toString())

        assertEquals(DateStyle.YEAR_MONTH_DAY, JsonFileMedicationStore(file).load().dateStyle)
    }

    @Test
    fun `open and expiry dates load back`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 2, 1), day)
            .buyRepetition(1, day, listOf(YearMonth.of(2030, 1), YearMonth.of(2031, 6)))
            .refillWeekly(1, day.atTime(21, 0))

        JsonFileMedicationStore(file).save(data)

        assertEquals(data, JsonFileMedicationStore(file).load())
    }

    @Test
    fun `an expiry saved as a full date loads as its year and month`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 1, 1), day)
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
            .addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 2, 1), day)
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
        val data = MedicationData().addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 1, 1), day)
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
    fun `a prescription saved before it had a medication loads with a blank one`() {
        val data = MedicationData()
            .addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 30, 1, 1), day)
        JsonFileMedicationStore(file).save(data)
        val json = JSONObject(file.readText())
        json.getJSONArray("prescriptions").getJSONObject(0).remove("medication")
        file.writeText(json.toString())

        assertEquals("", JsonFileMedicationStore(file).load().prescriptions.single().medication)
    }

    @Test
    fun `an unreadable file is kept aside instead of being overwritten`() {
        file.writeText("not json")

        assertEquals(MedicationData(), JsonFileMedicationStore(file, clockAt("2026-11-08T13:05:25Z")).load())

        assertEquals("not json", File(file.path + ".unreadable-20261108T130525Z").readText())
        assertTrue(!file.exists())
    }

    @Test
    fun `a second unreadable file doesn't replace the first`() {
        file.writeText("first")
        JsonFileMedicationStore(file, clockAt("2026-11-08T13:05:25Z")).load()
        file.writeText("second")

        JsonFileMedicationStore(file, clockAt("2026-11-09T08:00:00Z")).load()

        assertEquals("first", File(file.path + ".unreadable-20261108T130525Z").readText())
        assertEquals("second", File(file.path + ".unreadable-20261109T080000Z").readText())
    }

    private fun clockAt(instant: String) = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}
