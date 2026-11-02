package com.mediclog.medicationlogger.data

import java.time.LocalDate
import java.time.YearMonth

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

    /** Pills left unused by missed doses. A dose missed before any refill had no pill to leave. */
    val missedPills: Int
        get() = misses.count { miss ->
            miss.kind == MedicationMiss.Kind.MISSED && refills.any { it.date <= miss.date }
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

    /** Buys a repetition's boxes, each with its date from [expiryDates]. */
    fun buyRepetition(
        prescriptionId: Long,
        today: LocalDate,
        expiryDates: List<YearMonth> = emptyList(),
    ): MedicationData {
        val prescription = prescriptions.find { it.id == prescriptionId } ?: return this
        if (!prescription.canBuy) return this
        val firstBoxId = (boxes.maxOfOrNull { it.id } ?: 0) + 1
        val newBoxes = List(prescription.boxesPerRepetition) { index ->
            Box(
                id = firstBoxId + index,
                prescriptionId = prescription.id,
                boxNumber = prescription.nextBoxNumber + index,
                boughtDate = today,
                pillsLeft = prescription.pillsPerBox,
                pillsTotal = prescription.pillsPerBox,
                expiryDate = expiryDates.getOrNull(index),
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

    /** Replaces a prescription's fields. Its status follows from them, and its boxes stay as they are. */
    fun editPrescription(updated: Prescription): MedicationData {
        val isValid = prescriptions.any { it.id == updated.id } &&
            updated.prescribedBy.isNotBlank() &&
            updated.pharmacy.isNotBlank() &&
            updated.pillsPerBox > 0 &&
            updated.boxesPerRepetition > 0 &&
            updated.repetitionsTotal > 0 &&
            updated.repetitionsBought in 0..updated.repetitionsTotal
        if (!isValid) return this
        val isFinished = updated.repetitionsBought == updated.repetitionsTotal
        val orderedDate = updated.orderedDate.takeUnless { isFinished }
        return withPrescription(
            updated.copy(
                orderedDate = orderedDate,
                status = when {
                    isFinished -> Prescription.Status.FINISHED
                    orderedDate != null -> Prescription.Status.ORDERED
                    updated.repetitionsBought > 0 -> Prescription.Status.IN_PROGRESS
                    else -> Prescription.Status.NOT_STARTED
                },
            ),
        )
    }

    /**
     * Deletes a prescription with its boxes and their refills, or with [keepBoxes] only the prescription.
     * Whatever stays loses its link, as ids get reused and it would otherwise link to a later record.
     */
    fun deletePrescription(prescriptionId: Long, keepBoxes: Boolean = false): MedicationData {
        val deletedBoxIds = if (keepBoxes) {
            emptySet()
        } else {
            boxes.filter { it.prescriptionId == prescriptionId }.map { it.id }.toSet()
        }
        return copy(
            prescriptions = prescriptions.filterNot { it.id == prescriptionId },
            boxes = boxes
                .filterNot { it.id in deletedBoxIds }
                .map { if (it.prescriptionId == prescriptionId) it.copy(prescriptionId = null) else it },
            refills = refills
                .filterNot { it.boxId in deletedBoxIds }
                .map { if (it.prescriptionId == prescriptionId) it.copy(prescriptionId = null) else it },
        )
    }

    /** Replaces a box's fields. An empty box can't stay saved as extra. */
    fun editBox(updated: Box): MedicationData {
        val isValid = boxes.any { it.id == updated.id } &&
            updated.boxNumber > 0 &&
            updated.pillsTotal > 0 &&
            updated.pillsLeft in 0..updated.pillsTotal
        if (!isValid) return this
        return withBox(updated.copy(isExtra = updated.isExtra && updated.pillsLeft > 0))
    }

    /** Deletes a box with its refills, or with [keepRefills] only the box, and the refills lose their link to it. */
    fun deleteBox(boxId: Long, keepRefills: Boolean = false): MedicationData = copy(
        boxes = boxes.filterNot { it.id == boxId },
        refills = if (keepRefills) {
            refills.map { if (it.boxId == boxId) it.copy(boxId = null) else it }
        } else {
            refills.filterNot { it.boxId == boxId }
        },
    )

    /**
     * Replaces a refill's date, amount, pills left and partial flag.
     * A change in pills left moves pills to or from its box, and later refills follow.
     */
    fun editRefill(updated: Refill): MedicationData {
        val refill = refills.find { it.id == updated.id } ?: return this
        val extraPills = updated.amount - refill.amount
        val box = boxes.find { it.id == refill.boxId }
        val leftChange = updated.amountRemaining - refill.amountRemaining
        val isValid = updated.amount >= 1 && updated.amountRemaining >= 0 &&
            (box == null || (extraPills <= box.pillsLeft && box.pillsLeft + leftChange in 0..box.pillsTotal))
        if (!isValid) return this
        val edited = refill.copy(
            date = updated.date,
            amount = updated.amount,
            amountRemaining = updated.amountRemaining,
            isPartial = updated.isPartial,
        )
        return copy(refills = refills.map { if (it.id == refill.id) edited else it })
            .takingPills(refill, -leftChange)
    }

    /** Deletes a refill, and its pills go back to its box. */
    fun deleteRefill(refillId: Long): MedicationData {
        val refill = refills.find { it.id == refillId } ?: return this
        return copy(refills = refills - refill).takingPills(refill, -refill.amount)
    }

    fun editMiss(updated: MedicationMiss): MedicationData =
        copy(misses = misses.map { if (it.id == updated.id) updated else it })

    fun deleteMiss(missId: Long): MedicationData = copy(misses = misses.filterNot { it.id == missId })

    // Later refills recorded how many pills were left after them, so they change along with the box
    private fun takingPills(refill: Refill, pills: Int) = copy(
        boxes = boxes.map { box ->
            if (box.id == refill.boxId) {
                val pillsLeft = (box.pillsLeft - pills).coerceIn(0, box.pillsTotal)
                // Full again means not started again, so its next first refill opens it anew
                box.copy(pillsLeft = pillsLeft, openDate = box.openDate.takeIf { pillsLeft < box.pillsTotal })
            } else {
                box
            }
        },
        refills = refills.map { later ->
            if (refill.boxId != null && later.boxId == refill.boxId && later.id > refill.id) {
                later.copy(amountRemaining = (later.amountRemaining - pills).coerceAtLeast(0))
            } else {
                later
            }
        },
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
        val openDate = box.openDate ?: today.takeIf { box.pillsLeft == box.pillsTotal }
        return copy(refills = refills + refill).withBox(box.copy(pillsLeft = pillsLeft, openDate = openDate))
    }

    private fun withPrescription(updated: Prescription) =
        copy(prescriptions = prescriptions.map { if (it.id == updated.id) updated else it })

    private fun withBox(updated: Box) = copy(boxes = boxes.map { if (it.id == updated.id) updated else it })
}
