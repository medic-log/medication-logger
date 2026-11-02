package com.mediclog.medicationlogger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

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
        assertTrue(second.boxes.all { it.prescriptionId == 1L })
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

    private val refilled = MedicationData()
        .addPrescription(draft(), day1)
        .buyRepetition(1, day1)
        .refillWeekly(1, day1)
        .refillWeekly(1, day2)

    @Test
    fun `editing a prescription replaces its fields and works out its status`() {
        val prescription = prescribed.prescriptions.single()

        val edited = prescribed
            .editPrescription(prescription.copy(prescribedBy = "Dr Other", orderedDate = day2))
            .prescriptions
            .single()

        assertEquals("Dr Other", edited.prescribedBy)
        assertEquals(Prescription.Status.ORDERED, edited.status)
    }

    @Test
    fun `a prescription with every repetition bought is finished and no longer ordered`() {
        val prescription = prescribed.prescriptions.single()

        val edited = prescribed.editPrescription(
            prescription.copy(repetitionsBought = prescription.repetitionsTotal, orderedDate = day2),
        ).prescriptions.single()

        assertEquals(Prescription.Status.FINISHED, edited.status)
        assertEquals(null, edited.orderedDate)
    }

    @Test
    fun `an edit with more repetitions bought than in total changes nothing`() {
        val prescription = prescribed.prescriptions.single()

        val edited = prescribed.editPrescription(
            prescription.copy(repetitionsBought = prescription.repetitionsTotal + 1),
        )

        assertEquals(prescribed, edited)
    }

    @Test
    fun `deleting a prescription deletes its boxes and refills only`() {
        val data = refilled
            .addPrescription(draft(), day1)
            .buyRepetition(2, day1)
            .refillWeekly(refilled.boxes.size + 1L, day2)

        val deleted = data.deletePrescription(1)

        assertEquals(listOf(2L), deleted.prescriptions.map { it.id })
        assertTrue(deleted.boxes.all { it.prescriptionId == 2L })
        assertTrue(deleted.refills.all { it.prescriptionId == 2L } && deleted.refills.isNotEmpty())
    }

    @Test
    fun `deleting only a prescription keeps its boxes and refills, without links to it`() {
        val deleted = refilled.deletePrescription(1, keepBoxes = true)

        assertTrue(deleted.prescriptions.isEmpty())
        assertEquals(refilled.boxes.size, deleted.boxes.size)
        assertTrue(deleted.boxes.all { it.prescriptionId == null })
        assertEquals(refilled.refills.map { it.boxId }, deleted.refills.map { it.boxId })
        assertTrue(deleted.refills.all { it.prescriptionId == null })
    }

    @Test
    fun `a new prescription doesn't take over the boxes of a deleted one with the same id`() {
        val data = refilled.deletePrescription(1, keepBoxes = true).addPrescription(draft(), day2)

        assertEquals(1L, data.prescriptions.single().id)
        assertTrue(data.boxes.none { it.prescriptionId == 1L })
    }

    @Test
    fun `deleting a prescription keeps refills whose box was kept when it was deleted`() {
        val data = refilled.deleteBox(1, keepRefills = true).deletePrescription(1)

        assertEquals(refilled.refills.size, data.refills.size)
        assertTrue(data.refills.all { it.boxId == null && it.prescriptionId == null })
    }

    @Test
    fun `buying gives each box its expiry date`() {
        val dates = listOf(YearMonth.of(2030, 1), YearMonth.of(2030, 1), YearMonth.of(2031, 6))

        val boxes = prescribed.buyRepetition(1, day1, dates).boxes

        assertEquals(dates, boxes.map { it.expiryDate })
    }

    @Test
    fun `a box's first refill sets its open date, and later ones keep it`() {
        val bought = prescribed.buyRepetition(1, day1)
        assertEquals(null, bought.boxes.first().openDate)

        val opened = bought.refillWeekly(1, day1).refillWeekly(1, day2)

        assertEquals(day1, opened.boxes.first().openDate)
    }

    @Test
    fun `deleting the only refill makes the box not opened again`() {
        val opened = prescribed.buyRepetition(1, day1).refillWeekly(1, day1)

        val deleted = opened.deleteRefill(opened.refills.single().id)

        assertEquals(null, deleted.boxes.first().openDate)
    }

    @Test
    fun `a box already under way without an open date doesn't get one from a later refill`() {
        val bought = prescribed.buyRepetition(1, day1)
        val underWay = bought.editBox(bought.boxes.first().copy(pillsLeft = 20))

        assertEquals(null, underWay.refillWeekly(1, day2).boxes.first().openDate)
    }

    @Test
    fun `editing a box replaces its fields, and more pills left than in total changes nothing`() {
        val box = refilled.boxes.first()

        assertEquals(5, refilled.editBox(box.copy(pillsLeft = 5)).boxes.first().pillsLeft)
        assertEquals(refilled, refilled.editBox(box.copy(pillsLeft = box.pillsTotal + 1)))
    }

    @Test
    fun `an extra box edited down to empty is no longer extra`() {
        val data = refilled.saveAsExtra(1)

        val edited = data.editBox(data.boxes.first().copy(pillsLeft = 0)).boxes.first()

        assertEquals(Box.Status.FINISHED, edited.status)
    }

    @Test
    fun `deleting a box deletes its refills`() {
        val deleted = refilled.deleteBox(1)

        assertTrue(deleted.boxes.none { it.id == 1L })
        assertTrue(deleted.refills.isEmpty())
    }

    @Test
    fun `deleting only a box keeps its refills, without a link to it`() {
        val deleted = refilled.deleteBox(1, keepRefills = true)

        assertTrue(deleted.boxes.none { it.id == 1L })
        assertEquals(refilled.refills.size, deleted.refills.size)
        assertTrue(deleted.refills.all { it.boxId == null && it.prescriptionId == 1L })
    }

    @Test
    fun `refills without a box don't affect each other`() {
        val data = refilled.refillWeekly(1, day2).deleteBox(1, keepRefills = true)
        val first = data.refills.first()

        val deleted = data.deleteRefill(first.id)

        assertEquals(data.refills - first, deleted.refills)
    }

    @Test
    fun `changing a refill's amount moves pills, and later refills follow`() {
        val pillsBefore = refilled.boxes.first().pillsLeft
        val first = refilled.refills.first()

        val edited = refilled.editRefill(first.copy(amount = first.amount + 2))

        assertEquals(pillsBefore - 2, edited.boxes.first().pillsLeft)
        assertEquals(first.amountRemaining - 2, edited.refills.first().amountRemaining)
        assertEquals(refilled.refills.last().amountRemaining - 2, edited.refills.last().amountRemaining)
    }

    @Test
    fun `a refill can't be edited to more pills than its box has`() {
        val first = refilled.refills.first()

        val edited = refilled.editRefill(first.copy(amount = first.amount + refilled.boxes.first().pillsLeft + 1))

        assertEquals(refilled, edited)
    }

    @Test
    fun `deleting a refill puts its pills back, and later refills follow`() {
        val pillsBefore = refilled.boxes.first().pillsLeft
        val first = refilled.refills.first()

        val deleted = refilled.deleteRefill(first.id)

        assertEquals(pillsBefore + first.amount, deleted.boxes.first().pillsLeft)
        assertEquals(
            refilled.refills.last().amountRemaining + first.amount,
            deleted.refills.single().amountRemaining,
        )
    }

    @Test
    fun `a miss can be edited and deleted`() {
        val data = MedicationData().logMiss(MedicationMiss.Kind.MISSED, day1)
        val miss = data.misses.single()

        val edited = data.editMiss(miss.copy(date = day2, kind = MedicationMiss.Kind.DELAYED))

        assertEquals(miss.copy(date = day2, kind = MedicationMiss.Kind.DELAYED), edited.misses.single())
        assertTrue(edited.deleteMiss(miss.id).misses.isEmpty())
    }
}
