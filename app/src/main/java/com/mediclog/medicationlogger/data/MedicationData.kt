package com.mediclog.medicationlogger.data

import java.time.LocalDate

/** Pills in a full weekly refill. */
const val PILLS_PER_WEEK = 7

/**
 * Everything the app stores, with the actions that change it. Lists are in creation order.
 * An action that isn't allowed in the current state returns the data unchanged.
 */
data class MedicationData(
    val prescriptions: List<Prescription> = emptyList(),
    val boxes: List<Box> = emptyList(),
    val refills: List<Refill> = emptyList(),
    val misses: List<MedicationMiss> = emptyList(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
) {
    /** Pills still missing from the last refill to reach a full week; 0 when it's complete. */
    val pendingRefillAmount: Int
        get() {
            val partialPills = refills.asReversed().takeWhile { it.isPartial }.sumOf { it.amount }
            return (PILLS_PER_WEEK - partialPills % PILLS_PER_WEEK) % PILLS_PER_WEEK
        }

    fun addPrescription(draft: PrescriptionDraft, today: LocalDate): MedicationData = copy(
        prescriptions = prescriptions + Prescription(
            id = (prescriptions.maxOfOrNull { it.id } ?: 0) + 1,
            prescribedDate = today,
            prescribedBy = draft.prescribedBy,
            pharmacy = draft.pharmacy,
            pillsPerBox = draft.pillsPerBox,
            boxesPerRepetition = draft.boxesPerRepetition,
            repetitionsTotal = draft.repetitionsTotal,
        ),
    )

    fun orderRepetition(prescriptionId: Long, today: LocalDate): MedicationData {
        val prescription = prescriptions.find { it.id == prescriptionId } ?: return this
        if (!prescription.canOrder) return this
        return withPrescription(prescription.copy(status = Prescription.Status.ORDERED, orderedDate = today))
    }

    fun buyRepetition(prescriptionId: Long, today: LocalDate): MedicationData {
        val prescription = prescriptions.find { it.id == prescriptionId } ?: return this
        if (!prescription.canBuy) return this
        val firstBoxNumber = prescription.repetitionsBought * prescription.boxesPerRepetition + 1
        val firstBoxId = (boxes.maxOfOrNull { it.id } ?: 0) + 1
        val newBoxes = List(prescription.boxesPerRepetition) { index ->
            Box(
                id = firstBoxId + index,
                prescriptionId = prescription.id,
                boxNumber = firstBoxNumber + index,
                pharmacy = prescription.pharmacy,
                boughtDate = today,
                pillsLeft = prescription.pillsPerBox,
                pillsTotal = prescription.pillsPerBox,
            )
        }
        val repetitionsBought = prescription.repetitionsBought + 1
        val isFinished = repetitionsBought == prescription.repetitionsTotal
        return copy(boxes = boxes + newBoxes).withPrescription(
            prescription.copy(
                status = if (isFinished) Prescription.Status.FINISHED else Prescription.Status.IN_PROGRESS,
                orderedDate = null,
                boughtFirstDate = prescription.boughtFirstDate ?: today,
                boughtLastDate = today,
                repetitionsBought = repetitionsBought,
            ),
        )
    }

    fun refillWeekly(boxId: Long, today: LocalDate): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return refill(box, wanted = PILLS_PER_WEEK, isPartial = box.pillsLeft < PILLS_PER_WEEK, today = today)
    }

    fun refillPartial(boxId: Long, today: LocalDate): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return refill(box, wanted = pendingRefillAmount, isPartial = true, today = today)
    }

    fun saveAsExtra(boxId: Long): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return if (box.canSaveAsExtra) withBox(box.copy(isExtra = true)) else this
    }

    fun backToInventory(boxId: Long): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return withBox(box.copy(isExtra = false))
    }

    fun logMiss(kind: MedicationMiss.Kind, today: LocalDate): MedicationData = copy(
        misses = misses + MedicationMiss(id = (misses.maxOfOrNull { it.id } ?: 0) + 1, date = today, kind = kind),
    )

    private fun refill(box: Box, wanted: Int, isPartial: Boolean, today: LocalDate): MedicationData {
        val amount = minOf(wanted, box.pillsLeft)
        if (!box.canRefill || amount == 0) return this
        val pillsLeft = box.pillsLeft - amount
        val refill = Refill(
            id = (refills.maxOfOrNull { it.id } ?: 0) + 1,
            boxId = box.id,
            prescriptionId = box.prescriptionId,
            date = today,
            amount = amount,
            amountRemaining = pillsLeft,
            isPartial = isPartial,
        )
        return copy(refills = refills + refill).withBox(box.copy(pillsLeft = pillsLeft))
    }

    private fun withPrescription(updated: Prescription) =
        copy(prescriptions = prescriptions.map { if (it.id == updated.id) updated else it })

    private fun withBox(updated: Box) = copy(boxes = boxes.map { if (it.id == updated.id) updated else it })
}
