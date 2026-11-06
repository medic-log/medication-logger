package com.mediclog.medicationlogger.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

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
    val isTutorialDone: Boolean = false,
) {
    /**
     * Pills in the weekly lot at [now], counted from the first refill: refills add their pills and missed doses one,
     * and each day's pill time takes one. Records have only a date, so they count as made after that day's pill.
     */
    fun lotPills(now: LocalDateTime): Int {
        val firstDay = refills.minOfOrNull { it.date } ?: return 0
        val today = now.toLocalDate()
        val addedByDay = (refills.map { it.date to it.amount } + misses.filter { it.isMissed }.map { it.date to 1 })
            .groupBy({ it.first }, { it.second })
        var pills = 0
        var day = firstDay
        while (day <= today) {
            if (!day.atTime(reminderSettings.pillTime).isAfter(now)) pills = (pills - 1).coerceAtLeast(0)
            pills += addedByDay[day]?.sum() ?: 0
            day = day.plusDays(1)
        }
        return pills
    }

    /**
     * Pills the weekly lot is short of lasting until the next weekly refill; 0 when it lasts,
     * or before the first refill, as there's nothing to make up yet.
     */
    fun pendingRefillAmount(now: LocalDateTime): Int {
        if (refills.isEmpty()) return 0
        val nextRefill = ReminderCalculator.nextRefill(reminderSettings, now)
        val doses = pillTimesBetween(now, nextRefill, reminderSettings.pillTime)
        return (doses - lotPills(now)).coerceAtLeast(0)
    }

    fun addPrescription(draft: PrescriptionDraft, today: LocalDate): MedicationData = copy(
        prescriptions = prescriptions + Prescription(
            id = (prescriptions.maxOfOrNull { it.id } ?: 0) + 1,
            medication = draft.medication,
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

    fun refillWeekly(boxId: Long, now: LocalDateTime): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return refill(box, wanted = PILLS_PER_WEEK, isPartial = box.pillsLeft < PILLS_PER_WEEK, now = now)
    }

    fun refillPartial(boxId: Long, now: LocalDateTime): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return refill(box, wanted = pendingRefillAmount(now), isPartial = true, now = now)
    }

    fun saveAsExtra(boxId: Long): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return if (box.canSaveAsExtra) withBox(box.copy(isExtra = true)) else this
    }

    /** Sets the notes of an extra box; other boxes can't have notes. */
    fun setBoxNotes(boxId: Long, notes: String): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return if (box.isExtra) withBox(box.copy(notes = notes.trim())) else this
    }

    fun backToInventory(boxId: Long): MedicationData {
        val box = boxes.find { it.id == boxId } ?: return this
        return withBox(box.copy(isExtra = false, notes = ""))
    }

    /**
     * Logs a dose dated today, a delayed one taken at [time]. A missed one leaves its pill in the weekly lot, once
     * there's been a refill.
     */
    fun logMiss(kind: MedicationMiss.Kind, now: LocalDateTime, time: LocalTime? = null): MedicationData = copy(
        misses = misses + MedicationMiss(
            id = (misses.maxOfOrNull { it.id } ?: 0) + 1,
            date = now.toLocalDate(),
            kind = kind,
            time = time.takeIf { kind == MedicationMiss.Kind.DELAYED },
        ),
    )

    /** Replaces a prescription's fields. Its status follows from them, and its boxes stay as they are. */
    fun editPrescription(updated: Prescription): MedicationData {
        val isValid = prescriptions.any { it.id == updated.id } &&
            updated.medication.isNotBlank() &&
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
        val isExtra = updated.isExtra && updated.pillsLeft > 0
        return withBox(updated.copy(isExtra = isExtra, notes = if (isExtra) updated.notes.trim() else ""))
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

    /** Marks the tutorial as seen, so it no longer opens on its own. */
    fun finishTutorial(): MedicationData = copy(isTutorialDone = true)

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

    private fun refill(box: Box, wanted: Int, isPartial: Boolean, now: LocalDateTime): MedicationData {
        val today = now.toLocalDate()
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

/** How many times [pillTime] comes after [from] and by [to]. */
private fun pillTimesBetween(from: LocalDateTime, to: LocalDateTime, pillTime: LocalTime): Int {
    val first = from.toLocalDate().atTime(pillTime).let { if (it.isAfter(from)) it else it.plusDays(1) }
    val last = to.toLocalDate().atTime(pillTime).let { if (it.isAfter(to)) it.minusDays(1) else it }
    return if (last.isBefore(first)) 0 else ChronoUnit.DAYS.between(first, last).toInt() + 1
}
