package com.mediclog.medicationlogger.ui.main

import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderSettings
import java.time.LocalDateTime
import java.time.YearMonth

private const val SAMPLE_WEEKS = 4
private const val EXPIRY_MONTHS = 14L

/**
 * Made-up records for the tutorial to point at, dated up to [now]: [draft]'s prescription with one repetition
 * bought and the next ordered, a few weeks of refills, the last made partly from a second box, and a missed dose.
 */
fun tutorialSample(draft: PrescriptionDraft, settings: ReminderSettings, now: LocalDateTime): MedicationData {
    val lastRefill = ReminderCalculator.nextRefill(settings, now).minusWeeks(1)
    val start = lastRefill.minusWeeks(SAMPLE_WEEKS.toLong())
    val expiry = YearMonth.from(now).plusMonths(EXPIRY_MONTHS)
    var data = MedicationData(reminderSettings = settings)
        .addPrescription(draft, start.toLocalDate())
        .buyRepetition(1, start.toLocalDate(), List(draft.boxesPerRepetition) { expiry.plusMonths(it.toLong()) })
    for (week in SAMPLE_WEEKS downTo 0) {
        data = data.refillWeekly(data.boxes.first().id, lastRefill.minusWeeks(week.toLong()))
    }
    data.boxes.getOrNull(1)?.let { data = data.refillPartial(it.id, lastRefill) }
    return data
        .logMiss(MedicationMiss.Kind.MISSED, now)
        .orderRepetition(1, now.toLocalDate())
}
