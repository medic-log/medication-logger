import * as a from '../../data/MedicationData'
import type { MedicationData } from '../../data/MedicationData'
import type { PrescriptionDraft, ReminderSettings } from '../../data/models'
import { nextRefill } from '../../data/ReminderCalculator'
import { type LocalDateTime, plusMonths, plusWeeksTime, toLocalDate, yearMonthFrom } from '../../data/time'

const SAMPLE_WEEKS = 4
const EXPIRY_MONTHS = 14

/**
 * Made-up records for the tutorial to point at, dated up to [now]: [draft]'s prescription with one repetition
 * bought and the next ordered, a few weeks of refills, the last made partly from a second box, and a missed dose.
 */
export function tutorialSample(draft: PrescriptionDraft, settings: ReminderSettings, now: LocalDateTime): MedicationData {
  const lastRefill = plusWeeksTime(nextRefill(settings, now), -1)
  const start = plusWeeksTime(lastRefill, -SAMPLE_WEEKS)
  const expiry = plusMonths(yearMonthFrom(toLocalDate(now)), EXPIRY_MONTHS)
  let data = a.emptyData({ reminderSettings: settings })
  data = a.addPrescription(data, draft, toLocalDate(start))
  data = a.buyRepetition(
    data,
    1,
    toLocalDate(start),
    Array.from({ length: draft.boxesPerRepetition }, (_, index) => plusMonths(expiry, index)),
  )
  for (let week = SAMPLE_WEEKS; week >= 0; week--) {
    data = a.refillWeekly(data, data.boxes[0].id, plusWeeksTime(lastRefill, -week))
  }
  if (data.boxes[1]) data = a.refillPartial(data, data.boxes[1].id, lastRefill)
  data = a.logMiss(data, 'MISSED', now)
  return a.orderRepetition(data, 1, toLocalDate(now))
}
