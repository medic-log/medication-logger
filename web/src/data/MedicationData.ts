// Everything the app stores, with the actions that change it: a port of the Android app's MedicationData.kt.
// Lists are in creation order. An action that isn't allowed in the current state returns the data unchanged
// (the same object).
import {
  type Box,
  type DateStyle,
  type MedicationMiss,
  type MissKind,
  type Prescription,
  type PrescriptionDraft,
  type Refill,
  type ReminderSettings,
  DEFAULT_REMINDER_SETTINGS,
  canBuy,
  canOrder,
  canRefill,
  canSaveAsExtra,
  isMissed,
  nextBoxNumber,
} from './models'
import { nextRefill } from './ReminderCalculator'
import {
  type LocalDate,
  type LocalDateTime,
  type LocalTime,
  type YearMonth,
  atTime,
  daysBetween,
  plusDays,
  plusDaysTime,
  toLocalDate,
} from './time'

/** Pills in a full weekly refill. */
export const PILLS_PER_WEEK = 7

export interface MedicationData {
  prescriptions: Prescription[]
  boxes: Box[]
  refills: Refill[]
  misses: MedicationMiss[]
  reminderSettings: ReminderSettings
  isTutorialDone: boolean
  dateStyle: DateStyle
}

export function emptyData(overrides: Partial<MedicationData> = {}): MedicationData {
  return {
    prescriptions: [],
    boxes: [],
    refills: [],
    misses: [],
    reminderSettings: DEFAULT_REMINDER_SETTINGS,
    isTutorialDone: false,
    dateStyle: 'YEAR_MONTH_DAY',
    ...overrides,
  }
}

const nextId = (records: { id: number }[]) => Math.max(0, ...records.map(it => it.id)) + 1
const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

/**
 * Pills in the weekly lot at [now], counted from the first refill: refills add their pills and missed doses one,
 * and each day's pill time takes one. Records have only a date, so they count as made after that day's pill.
 */
export function lotPills(data: MedicationData, now: LocalDateTime): number {
  if (data.refills.length === 0) return 0
  const firstDay = data.refills.map(it => it.date).reduce((a, b) => (b < a ? b : a))
  const today = toLocalDate(now)
  const addedByDay = new Map<LocalDate, number>()
  const add = (day: LocalDate, pills: number) => addedByDay.set(day, (addedByDay.get(day) ?? 0) + pills)
  data.refills.forEach(it => add(it.date, it.amount))
  data.misses.filter(isMissed).forEach(it => add(it.date, 1))
  let pills = 0
  for (let day = firstDay; day <= today; day = plusDays(day, 1)) {
    if (!(atTime(day, data.reminderSettings.pillTime) > now)) pills = Math.max(pills - 1, 0)
    pills += addedByDay.get(day) ?? 0
  }
  return pills
}

/**
 * Pills the weekly lot is short of lasting until the next weekly refill; 0 when it lasts,
 * or before the first refill, as there's nothing to make up yet.
 */
export function pendingRefillAmount(data: MedicationData, now: LocalDateTime): number {
  if (data.refills.length === 0) return 0
  const doses = pillTimesBetween(now, nextRefill(data.reminderSettings, now), data.reminderSettings.pillTime)
  return Math.max(doses - lotPills(data, now), 0)
}

/** How many times [pillTime] comes after [from] and by [to]. */
function pillTimesBetween(from: LocalDateTime, to: LocalDateTime, pillTime: LocalTime): number {
  const todayPill = atTime(toLocalDate(from), pillTime)
  const first = todayPill > from ? todayPill : plusDaysTime(todayPill, 1)
  const lastPill = atTime(toLocalDate(to), pillTime)
  const last = lastPill > to ? plusDaysTime(lastPill, -1) : lastPill
  return last < first ? 0 : daysBetween(toLocalDate(first), toLocalDate(last)) + 1
}

export function addPrescription(data: MedicationData, draft: PrescriptionDraft, today: LocalDate): MedicationData {
  return {
    ...data,
    prescriptions: [
      ...data.prescriptions,
      {
        id: nextId(data.prescriptions),
        medication: draft.medication,
        prescribedDate: today,
        prescribedBy: draft.prescribedBy,
        pharmacy: draft.pharmacy,
        pillsPerBox: draft.pillsPerBox,
        boxesPerRepetition: draft.boxesPerRepetition,
        repetitionsTotal: draft.repetitionsTotal,
        status: 'NOT_STARTED',
        orderedDate: null,
        boughtFirstDate: null,
        boughtLastDate: null,
        repetitionsBought: 0,
      },
    ],
  }
}

export function orderRepetition(data: MedicationData, prescriptionId: number, today: LocalDate): MedicationData {
  const prescription = data.prescriptions.find(it => it.id === prescriptionId)
  if (!prescription || !canOrder(prescription)) return data
  return withPrescription(data, { ...prescription, status: 'ORDERED', orderedDate: today })
}

/** Buys a repetition's boxes, each with its date from [expiryDates]. */
export function buyRepetition(
  data: MedicationData,
  prescriptionId: number,
  today: LocalDate,
  expiryDates: YearMonth[] = [],
): MedicationData {
  const prescription = data.prescriptions.find(it => it.id === prescriptionId)
  if (!prescription || !canBuy(prescription)) return data
  const firstBoxId = nextId(data.boxes)
  const newBoxes: Box[] = Array.from({ length: prescription.boxesPerRepetition }, (_, index) => ({
    id: firstBoxId + index,
    prescriptionId: prescription.id,
    boxNumber: nextBoxNumber(prescription) + index,
    boughtDate: today,
    pillsLeft: prescription.pillsPerBox,
    pillsTotal: prescription.pillsPerBox,
    isExtra: false,
    openDate: null,
    expiryDate: expiryDates[index] ?? null,
    notes: '',
  }))
  const repetitionsBought = prescription.repetitionsBought + 1
  const isFinished = repetitionsBought === prescription.repetitionsTotal
  return withPrescription(
    { ...data, boxes: [...data.boxes, ...newBoxes] },
    {
      ...prescription,
      status: isFinished ? 'FINISHED' : 'IN_PROGRESS',
      orderedDate: null,
      boughtFirstDate: prescription.boughtFirstDate ?? today,
      boughtLastDate: today,
      repetitionsBought,
    },
  )
}

export function refillWeekly(data: MedicationData, boxId: number, now: LocalDateTime): MedicationData {
  const box = data.boxes.find(it => it.id === boxId)
  if (!box) return data
  return refill(data, box, PILLS_PER_WEEK, box.pillsLeft < PILLS_PER_WEEK, now)
}

export function refillPartial(data: MedicationData, boxId: number, now: LocalDateTime): MedicationData {
  const box = data.boxes.find(it => it.id === boxId)
  if (!box) return data
  return refill(data, box, pendingRefillAmount(data, now), true, now)
}

export function saveAsExtra(data: MedicationData, boxId: number): MedicationData {
  const box = data.boxes.find(it => it.id === boxId)
  if (!box || !canSaveAsExtra(box)) return data
  return withBox(data, { ...box, isExtra: true })
}

/** Sets the notes of an extra box; other boxes can't have notes. */
export function setBoxNotes(data: MedicationData, boxId: number, notes: string): MedicationData {
  const box = data.boxes.find(it => it.id === boxId)
  if (!box || !box.isExtra) return data
  return withBox(data, { ...box, notes: notes.trim() })
}

export function backToInventory(data: MedicationData, boxId: number): MedicationData {
  const box = data.boxes.find(it => it.id === boxId)
  if (!box) return data
  return withBox(data, { ...box, isExtra: false, notes: '' })
}

/**
 * Logs a dose dated today, a delayed one taken at [time]. A missed one leaves its pill in the weekly lot, once
 * there's been a refill.
 */
export function logMiss(
  data: MedicationData,
  kind: MissKind,
  now: LocalDateTime,
  time: LocalTime | null = null,
): MedicationData {
  return {
    ...data,
    misses: [
      ...data.misses,
      { id: nextId(data.misses), date: toLocalDate(now), kind, time: kind === 'DELAYED' ? time : null },
    ],
  }
}

const isBlank = (text: string) => text.trim() === ''

/** Replaces a prescription's fields. Its status follows from them, and its boxes stay as they are. */
export function editPrescription(data: MedicationData, updated: Prescription): MedicationData {
  const isValid =
    data.prescriptions.some(it => it.id === updated.id) &&
    !isBlank(updated.medication) &&
    !isBlank(updated.prescribedBy) &&
    !isBlank(updated.pharmacy) &&
    updated.pillsPerBox > 0 &&
    updated.boxesPerRepetition > 0 &&
    updated.repetitionsTotal > 0 &&
    updated.repetitionsBought >= 0 &&
    updated.repetitionsBought <= updated.repetitionsTotal
  if (!isValid) return data
  const isFinished = updated.repetitionsBought === updated.repetitionsTotal
  const orderedDate = isFinished ? null : updated.orderedDate
  return withPrescription(data, {
    ...updated,
    orderedDate,
    status: isFinished
      ? 'FINISHED'
      : orderedDate !== null
        ? 'ORDERED'
        : updated.repetitionsBought > 0
          ? 'IN_PROGRESS'
          : 'NOT_STARTED',
  })
}

/**
 * Deletes a prescription with its boxes and their refills, or with [keepBoxes] only the prescription.
 * Whatever stays loses its link, as ids get reused and it would otherwise link to a later record.
 */
export function deletePrescription(data: MedicationData, prescriptionId: number, keepBoxes = false): MedicationData {
  const deletedBoxIds = new Set(
    keepBoxes ? [] : data.boxes.filter(it => it.prescriptionId === prescriptionId).map(it => it.id),
  )
  return {
    ...data,
    prescriptions: data.prescriptions.filter(it => it.id !== prescriptionId),
    boxes: data.boxes
      .filter(it => !deletedBoxIds.has(it.id))
      .map(it => (it.prescriptionId === prescriptionId ? { ...it, prescriptionId: null } : it)),
    refills: data.refills
      .filter(it => it.boxId === null || !deletedBoxIds.has(it.boxId))
      .map(it => (it.prescriptionId === prescriptionId ? { ...it, prescriptionId: null } : it)),
  }
}

/** Replaces a box's fields. An empty box can't stay saved as extra. */
export function editBox(data: MedicationData, updated: Box): MedicationData {
  const isValid =
    data.boxes.some(it => it.id === updated.id) &&
    updated.boxNumber > 0 &&
    updated.pillsTotal > 0 &&
    updated.pillsLeft >= 0 &&
    updated.pillsLeft <= updated.pillsTotal
  if (!isValid) return data
  const isExtra = updated.isExtra && updated.pillsLeft > 0
  return withBox(data, { ...updated, isExtra, notes: isExtra ? updated.notes.trim() : '' })
}

/** Deletes a box with its refills, or with [keepRefills] only the box, and the refills lose their link to it. */
export function deleteBox(data: MedicationData, boxId: number, keepRefills = false): MedicationData {
  return {
    ...data,
    boxes: data.boxes.filter(it => it.id !== boxId),
    refills: keepRefills
      ? data.refills.map(it => (it.boxId === boxId ? { ...it, boxId: null } : it))
      : data.refills.filter(it => it.boxId !== boxId),
  }
}

/**
 * Replaces a refill's date, amount, pills left and partial flag.
 * A change in pills left moves pills to or from its box, and later refills follow.
 */
export function editRefill(data: MedicationData, updated: Refill): MedicationData {
  const refill = data.refills.find(it => it.id === updated.id)
  if (!refill) return data
  const extraPills = updated.amount - refill.amount
  const box = data.boxes.find(it => it.id === refill.boxId)
  const leftChange = updated.amountRemaining - refill.amountRemaining
  const isValid =
    updated.amount >= 1 &&
    updated.amountRemaining >= 0 &&
    (box === undefined ||
      (extraPills <= box.pillsLeft &&
        box.pillsLeft + leftChange >= 0 &&
        box.pillsLeft + leftChange <= box.pillsTotal))
  if (!isValid) return data
  const edited: Refill = {
    ...refill,
    date: updated.date,
    amount: updated.amount,
    amountRemaining: updated.amountRemaining,
    isPartial: updated.isPartial,
  }
  return takingPills({ ...data, refills: data.refills.map(it => (it.id === refill.id ? edited : it)) }, edited, -leftChange)
}

/** Deletes a refill, and its pills go back to its box. */
export function deleteRefill(data: MedicationData, refillId: number): MedicationData {
  const refill = data.refills.find(it => it.id === refillId)
  if (!refill) return data
  return takingPills({ ...data, refills: data.refills.filter(it => it !== refill) }, refill, -refill.amount)
}

export function editMiss(data: MedicationData, updated: MedicationMiss): MedicationData {
  return { ...data, misses: data.misses.map(it => (it.id === updated.id ? updated : it)) }
}

export function deleteMiss(data: MedicationData, missId: number): MedicationData {
  return { ...data, misses: data.misses.filter(it => it.id !== missId) }
}

/** Marks the tutorial as seen, so it no longer opens on its own. */
export function finishTutorial(data: MedicationData): MedicationData {
  return { ...data, isTutorialDone: true }
}

// Later refills recorded how many pills were left after them, so they change along with the box.
// Later by date, then id, as an edited date can put a refill out of id order.
function takingPills(data: MedicationData, refill: Refill, pills: number): MedicationData {
  return {
    ...data,
    boxes: data.boxes.map(box => {
      if (box.id !== refill.boxId) return box
      const pillsLeft = clamp(box.pillsLeft - pills, 0, box.pillsTotal)
      // Full again means not started again, so its next first refill opens it anew
      return { ...box, pillsLeft, openDate: pillsLeft < box.pillsTotal ? box.openDate : null }
    }),
    refills: data.refills.map(later =>
      refill.boxId !== null && later.boxId === refill.boxId &&
      (later.date > refill.date || (later.date === refill.date && later.id > refill.id))
        ? { ...later, amountRemaining: Math.max(later.amountRemaining - pills, 0) }
        : later,
    ),
  }
}

function refill(data: MedicationData, box: Box, wanted: number, isPartial: boolean, now: LocalDateTime) {
  const today = toLocalDate(now)
  const amount = Math.min(wanted, box.pillsLeft)
  if (!canRefill(box) || amount === 0) return data
  const pillsLeft = box.pillsLeft - amount
  const newRefill: Refill = {
    id: nextId(data.refills),
    boxId: box.id,
    prescriptionId: box.prescriptionId,
    date: today,
    amount,
    amountRemaining: pillsLeft,
    isPartial,
  }
  const openDate = box.openDate ?? (box.pillsLeft === box.pillsTotal ? today : null)
  return withBox({ ...data, refills: [...data.refills, newRefill] }, { ...box, pillsLeft, openDate })
}

function withPrescription(data: MedicationData, updated: Prescription): MedicationData {
  return { ...data, prescriptions: data.prescriptions.map(it => (it.id === updated.id ? updated : it)) }
}

function withBox(data: MedicationData, updated: Box): MedicationData {
  return { ...data, boxes: data.boxes.map(it => (it.id === updated.id ? updated : it)) }
}
