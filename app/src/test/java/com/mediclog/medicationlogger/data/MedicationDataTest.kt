package com.mediclog.medicationlogger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MedicationDataTest {
    private val day1 = LocalDate.of(2026, 1, 5)
    private val day2 = LocalDate.of(2026, 2, 5)

    private fun draft(pillsPerBox: Int = 30, boxesPerRepetition: Int = 3, repetitionsTotal: Int = 2) =
        PrescriptionDraft("Dr Test", "Test Pharmacy", pillsPerBox, boxesPerRepetition, repetitionsTotal)

    private val prescribed = MedicationData().addPrescription(draft(), day1)

    @Test
    fun `new prescription is dated today, not started, with computed totals`() {
        val prescription = prescribed.prescriptions.single()

        assertEquals(day1, prescription.prescribedDate)
        assertEquals(Prescription.Status.NOT_STARTED, prescription.status)
        assertEquals(90, prescription.pillsPerRepetition)
        assertEquals(6, prescription.boxesTotal)
        assertEquals(180, prescription.pillsTotal)
    }

    @Test
    fun `ordering sets the status without buying anything`() {
        val ordered = prescribed.orderRepetition(1, day1)

        val prescription = ordered.prescriptions.single()
        assertEquals(Prescription.Status.ORDERED, prescription.status)
        assertEquals(day1, prescription.orderedDate)
        assertEquals(0, prescription.repetitionsBought)
        assertTrue(ordered.boxes.isEmpty())
    }

    @Test
    fun `buying numbers boxes across repetitions and links them`() {
        val first = prescribed.buyRepetition(1, day1)
        assertEquals(listOf(1, 2, 3), first.boxes.map { it.boxNumber })

        val second = first.buyRepetition(1, day2)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), second.boxes.map { it.boxNumber })
        assertTrue(second.boxes.all { it.prescriptionId == 1L && it.pharmacy == "Test Pharmacy" })
        assertEquals(30, second.boxes.last().pillsLeft)
        assertEquals("$day2 #4", second.boxes[3].name)
    }

    @Test
    fun `buying updates dates, repetitions and status until finished`() {
        val first = prescribed.orderRepetition(1, day1).buyRepetition(1, day1)
        with(first.prescriptions.single()) {
            assertEquals(Prescription.Status.IN_PROGRESS, status)
            assertEquals(1, repetitionsBought)
            assertEquals(day1, boughtFirstDate)
            assertEquals(day1, boughtLastDate)
            assertNull(orderedDate)
        }

        val second = first.buyRepetition(1, day2)
        with(second.prescriptions.single()) {
            assertEquals(Prescription.Status.FINISHED, status)
            assertEquals(2, repetitionsBought)
            assertEquals(day1, boughtFirstDate)
            assertEquals(day2, boughtLastDate)
            assertFalse(isActive)
        }
    }

    @Test
    fun `a finished prescription can't be ordered or bought again`() {
        val finished = prescribed.buyRepetition(1, day1).buyRepetition(1, day1)

        assertEquals(finished, finished.buyRepetition(1, day2))
        assertEquals(finished, finished.orderRepetition(1, day2))
    }

    @Test
    fun `refills follow the example in the concept`() {
        var data = MedicationData()
            .addPrescription(draft(pillsPerBox = 25, boxesPerRepetition = 1, repetitionsTotal = 1), day1)
            .addPrescription(draft(pillsPerBox = 30, boxesPerRepetition = 1, repetitionsTotal = 1), day1)
            .buyRepetition(1, day1)
            .buyRepetition(2, day1)

        repeat(4) { data = data.refillWeekly(1, day1) }
        assertEquals(3, data.pendingRefillAmount)
        data = data.refillPartial(2, day2)
        assertEquals(0, data.pendingRefillAmount)
        data = data.refillWeekly(2, day2)

        assertEquals(
            listOf(
                Triple(7, 18, false),
                Triple(7, 11, false),
                Triple(7, 4, false),
                Triple(4, 0, true),
                Triple(3, 27, true),
                Triple(7, 20, false),
            ),
            data.refills.map { Triple(it.amount, it.amountRemaining, it.isPartial) },
        )
        assertEquals(listOf(1L, 1L, 1L, 1L, 2L, 2L), data.refills.map { it.boxId })
        assertEquals(listOf(1L, 1L, 1L, 1L, 2L, 2L), data.refills.map { it.prescriptionId })
        assertEquals(day2, data.refills.last().date)
    }

    @Test
    fun `a partial refill from a nearly empty box leaves the rest pending`() {
        var data = MedicationData()
            .addPrescription(draft(pillsPerBox = 4, boxesPerRepetition = 2, repetitionsTotal = 2), day1)
            .buyRepetition(1, day1)
            .buyRepetition(1, day1)
        data = data.refillWeekly(1, day1)
        assertEquals(3, data.pendingRefillAmount)

        // Box 2 has only one pill left after this
        data = data.refillWeekly(2, day1).refillWeekly(3, day1)
        assertEquals(2, data.pendingRefillAmount)
        data = data.refillPartial(4, day1)
        assertEquals(0, data.pendingRefillAmount)
        assertEquals(2, data.refills.last().amount)
    }

    @Test
    fun `partial refill does nothing when the last refill is complete`() {
        val data = prescribed.buyRepetition(1, day1).refillWeekly(1, day1)

        assertEquals(data, data.refillPartial(1, day1))
    }

    @Test
    fun `an empty box can't be refilled`() {
        var data = MedicationData()
            .addPrescription(draft(pillsPerBox = 7, boxesPerRepetition = 1, repetitionsTotal = 1), day1)
            .buyRepetition(1, day1)
            .refillWeekly(1, day1)

        assertEquals(Box.Status.FINISHED, data.boxes.single().status)
        data = data.refillWeekly(1, day1)
        assertEquals(1, data.refills.size)
    }

    @Test
    fun `box status follows pills left`() {
        val bought = prescribed.buyRepetition(1, day1)
        assertEquals(Box.Status.NOT_STARTED, bought.boxes.first().status)

        val refilled = bought.refillWeekly(1, day1)
        assertEquals(Box.Status.IN_PROGRESS, refilled.boxes.first().status)
        assertEquals(23, refilled.boxes.first().pillsLeft)
    }

    @Test
    fun `saving as extra disables refills and undoing restores the status`() {
        val started = prescribed.buyRepetition(1, day1).refillWeekly(1, day1)

        val extra = started.saveAsExtra(1).saveAsExtra(2)
        assertEquals(Box.Status.EXTRA, extra.boxes[0].status)
        assertEquals(extra, extra.refillWeekly(1, day1))

        val restored = extra.backToInventory(1).backToInventory(2)
        assertEquals(Box.Status.IN_PROGRESS, restored.boxes[0].status)
        assertEquals(Box.Status.NOT_STARTED, restored.boxes[1].status)
    }

    @Test
    fun `an empty box can't be saved as extra`() {
        val data = MedicationData()
            .addPrescription(draft(pillsPerBox = 7, boxesPerRepetition = 1, repetitionsTotal = 1), day1)
            .buyRepetition(1, day1)
            .refillWeekly(1, day1)

        assertEquals(data, data.saveAsExtra(1))
    }

    @Test
    fun `misses are logged with their date and kind`() {
        val data = MedicationData()
            .logMiss(MedicationMiss.Kind.MISSED, day1)
            .logMiss(MedicationMiss.Kind.DELAYED, day2)

        assertEquals(
            listOf(MedicationMiss(1, day1, MedicationMiss.Kind.MISSED), MedicationMiss(2, day2, MedicationMiss.Kind.DELAYED)),
            data.misses,
        )
    }
}
