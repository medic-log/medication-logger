// Works out when reminders are due: a port of the Android app's ReminderCalculator.kt. Holds no state, so it's the
// same after a reload or a missed check.
import type { MedicationData } from './MedicationData'
import { lotPills } from './MedicationData'
import { type Box, type ReminderKind, type ReminderSettings, pillsPerRepetition, repetitionsLeft } from './models'
import {
  type LocalDate,
  type LocalDateTime,
  atEndOfMonth,
  atTime,
  nextOrSame,
  plusDays,
  plusDaysTime,
  plusWeeksTime,
  toLocalDate,
} from './time'

// Pickup, run-out and expiry reminders have only a number of days, so they arrive at a fixed time
const DAILY_REMINDER_TIME = '09:00'

const sum = (values: number[]) => values.reduce((a, b) => a + b, 0)

/** Pills that can still be taken: those in non-extra boxes and in repetitions not bought yet. */
export function pillsRemaining(data: MedicationData): number {
  return pillsBought(data) + sum(data.prescriptions.map(it => repetitionsLeft(it) * pillsPerRepetition(it)))
}

/** The day the medication runs out at one pill a day, or null when nothing has been logged yet. */
export function runOutDate(data: MedicationData, now: LocalDateTime): LocalDate | null {
  const covered = coveredUntil(data, now)
  return covered === null ? null : plusDays(covered, pillsRemaining(data))
}

/**
 * The day the pills already bought run out, or null when nothing has been logged yet. The run-out reminder
 * follows this date, as repetitions not bought can't be taken until someone fetches them.
 */
export function boughtPillsRunOutDate(data: MedicationData, now: LocalDateTime): LocalDate | null {
  const covered = coveredUntil(data, now)
  return covered === null ? null : plusDays(covered, pillsBought(data))
}

function pillsBought(data: MedicationData): number {
  return sum(data.boxes.filter(it => !it.isExtra).map(it => it.pillsLeft))
}

// The first day with no pill left in the weekly lot
function coveredUntil(data: MedicationData, now: LocalDateTime): LocalDate | null {
  if (data.refills.length === 0 && pillsRemaining(data) === 0) return null
  return plusDays(toLocalDate(nextPill(data.reminderSettings, now)), lotPills(data, now))
}

/** The next weekly refill reminder after [now]. */
export function nextRefill(settings: ReminderSettings, now: LocalDateTime): LocalDateTime {
  const candidate = atTime(nextOrSame(toLocalDate(now), settings.refillDay), settings.refillTime)
  return candidate > now ? candidate : plusWeeksTime(candidate, 1)
}

/** The next daily reminder to take a pill after [now]. */
export function nextPill(settings: ReminderSettings, now: LocalDateTime): LocalDateTime {
  const candidate = atTime(toLocalDate(now), settings.pillTime)
  return candidate > now ? candidate : plusDaysTime(candidate, 1)
}

/** The first day a pickup reminder is due, or null when nothing is ordered. */
export function pickupDate(data: MedicationData): LocalDate | null {
  const ordered = data.prescriptions.map(it => it.orderedDate).filter((it): it is LocalDate => it !== null)
  if (ordered.length === 0) return null
  return plusDays(ordered.reduce((a, b) => (b < a ? b : a)), data.reminderSettings.pickupDaysAfterOrder)
}

/** The box with pills left that expires first, extra ones included, or null when none has an expiry date. */
export function expiringBox(data: MedicationData): Box | null {
  let first: Box | null = null
  for (const box of data.boxes) {
    if (box.pillsLeft > 0 && box.expiryDate !== null && (first === null || box.expiryDate < first.expiryDate!)) {
      first = box
    }
  }
  return first
}

/** When the reminder of [kind] should fire next, or null when it shouldn't be set. */
export function nextTrigger(kind: ReminderKind, data: MedicationData, now: LocalDateTime): LocalDateTime | null {
  const settings = data.reminderSettings
  switch (kind) {
    case 'REFILL':
      return settings.isRefillEnabled ? nextRefill(settings, now) : null
    case 'PILL':
      return settings.isPillEnabled ? nextPill(settings, now) : null
    default: {
      const date = dailyReminderDate(kind, data, now)
      return date === null ? null : nextDailyTrigger(date, now)
    }
  }
}

/** Whether a reminder of [kind] should be shown at [now], as data may have changed since it was set. */
export function isDue(kind: ReminderKind, data: MedicationData, now: LocalDateTime): boolean {
  switch (kind) {
    case 'REFILL':
      return data.reminderSettings.isRefillEnabled
    case 'PILL':
      return data.reminderSettings.isPillEnabled
    default: {
      const date = dailyReminderDate(kind, data, now)
      return date !== null && !(atTime(date, DAILY_REMINDER_TIME) > now)
    }
  }
}

function dailyReminderDate(kind: ReminderKind, data: MedicationData, now: LocalDateTime): LocalDate | null {
  const settings = data.reminderSettings
  if (kind === 'PICKUP' && settings.isPickupEnabled) return pickupDate(data)
  if (kind === 'RUN_OUT' && settings.isRunOutEnabled) {
    const date = boughtPillsRunOutDate(data, now)
    return date === null ? null : plusDays(date, -settings.runOutDaysBefore)
  }
  // A month on the box means usable until that month ends
  if (kind === 'EXPIRY' && settings.isExpiryEnabled) {
    const expiry = expiringBox(data)?.expiryDate
    return expiry ? plusDays(atEndOfMonth(expiry), -settings.expiryDaysBefore) : null
  }
  return null
}

// Once due, these repeat daily until resolved
function nextDailyTrigger(date: LocalDate, now: LocalDateTime): LocalDateTime {
  const first = atTime(date, DAILY_REMINDER_TIME)
  if (first > now) return first
  const today = atTime(toLocalDate(now), DAILY_REMINDER_TIME)
  return today > now ? today : plusDaysTime(today, 1)
}
