// The records, as in the Android app's data/ (Prescription, Box, Refill, MedicationMiss, ReminderSettings), with
// their computed properties as functions. Field names and enum values are the ones its JSON uses.
import type { DayOfWeek, LocalDate, LocalTime, YearMonth } from './time'

/** Where the prescription is in its life, from nothing bought to everything bought. */
export type PrescriptionStatus = 'NOT_STARTED' | 'ORDERED' | 'IN_PROGRESS' | 'FINISHED'

/** A prescription, bought in repetitions of boxes. */
export interface Prescription {
  id: number
  medication: string
  prescribedDate: LocalDate
  prescribedBy: string
  pharmacy: string
  pillsPerBox: number
  boxesPerRepetition: number
  repetitionsTotal: number
  status: PrescriptionStatus
  orderedDate: LocalDate | null
  boughtFirstDate: LocalDate | null
  boughtLastDate: LocalDate | null
  repetitionsBought: number
}

export const pillsPerRepetition = (p: Prescription) => p.pillsPerBox * p.boxesPerRepetition
export const boxesTotal = (p: Prescription) => p.boxesPerRepetition * p.repetitionsTotal
export const prescriptionPillsTotal = (p: Prescription) => pillsPerRepetition(p) * p.repetitionsTotal
export const repetitionsLeft = (p: Prescription) => p.repetitionsTotal - p.repetitionsBought
export const isPrescriptionActive = (p: Prescription) => p.status !== 'FINISHED'
export const canOrder = (p: Prescription) => repetitionsLeft(p) > 0 && p.status !== 'ORDERED'
export const nextBoxNumber = (p: Prescription) => p.repetitionsBought * p.boxesPerRepetition + 1
export const canBuy = (p: Prescription) => repetitionsLeft(p) > 0

/** What the user enters for a new prescription. */
export interface PrescriptionDraft {
  medication: string
  prescribedBy: string
  pharmacy: string
  pillsPerBox: number
  boxesPerRepetition: number
  repetitionsTotal: number
}

/** How much of the box is used, or that it's saved as extra. */
export type BoxStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'FINISHED' | 'EXTRA'

/**
 * A box of pills bought with a prescription; prescriptionId is null once that's deleted.
 * Notes say what an extra box is kept for, and are cleared when it goes back to the inventory.
 */
export interface Box {
  id: number
  prescriptionId: number | null
  boxNumber: number
  boughtDate: LocalDate
  pillsLeft: number
  pillsTotal: number
  isExtra: boolean
  openDate: LocalDate | null
  expiryDate: YearMonth | null
  notes: string
}

export const boxName = (box: Box) => `${box.boughtDate} #${box.boxNumber}`

export function boxStatus(box: Box): BoxStatus {
  if (box.isExtra) return 'EXTRA'
  if (box.pillsLeft === 0) return 'FINISHED'
  if (box.pillsLeft === box.pillsTotal) return 'NOT_STARTED'
  return 'IN_PROGRESS'
}

export const isBoxActive = (box: Box) => ['NOT_STARTED', 'IN_PROGRESS'].includes(boxStatus(box))
export const canRefill = isBoxActive
export const canSaveAsExtra = isBoxActive

/** Pills moved from a box for the week's doses; a link is null once what it points to is deleted. */
export interface Refill {
  id: number
  boxId: number | null
  prescriptionId: number | null
  date: LocalDate
  amount: number
  amountRemaining: number
  isPartial: boolean
}

/** What went wrong with the dose. */
export type MissKind = 'MISSED' | 'DELAYED'

/** A dose that was missed or taken late; a delayed one keeps the time it was taken, if known. */
export interface MedicationMiss {
  id: number
  date: LocalDate
  kind: MissKind
  time: LocalTime | null
}

export const isMissed = (miss: MedicationMiss) => miss.kind === 'MISSED'

/** The user's reminder configuration. Every reminder starts on. */
export interface ReminderSettings {
  isRefillEnabled: boolean
  refillDay: DayOfWeek
  refillTime: LocalTime
  isPickupEnabled: boolean
  pickupDaysAfterOrder: number
  isRunOutEnabled: boolean
  runOutDaysBefore: number
  isPillEnabled: boolean
  pillTime: LocalTime
  isExpiryEnabled: boolean
  expiryDaysBefore: number
}

export const DEFAULT_REMINDER_SETTINGS: ReminderSettings = {
  isRefillEnabled: true,
  refillDay: 'SUNDAY',
  refillTime: '20:00',
  isPickupEnabled: true,
  pickupDaysAfterOrder: 4,
  isRunOutEnabled: true,
  runOutDaysBefore: 14,
  isPillEnabled: true,
  pillTime: '08:00',
  isExpiryEnabled: true,
  expiryDaysBefore: 60,
}

export const hasAnyEnabled = (s: ReminderSettings) =>
  s.isRefillEnabled || s.isPickupEnabled || s.isRunOutEnabled || s.isPillEnabled || s.isExpiryEnabled

/** The reminders the app can send, in the Android app's order. */
export const REMINDER_KINDS = ['REFILL', 'PICKUP', 'RUN_OUT', 'PILL', 'EXPIRY'] as const
export type ReminderKind = (typeof REMINDER_KINDS)[number]
