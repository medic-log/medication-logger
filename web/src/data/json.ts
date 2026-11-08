// The JSON the Android app's JsonFileMedicationStore writes, read and written the same way, so an export moves
// between the two apps and the web app's saved data has the Android file's shape.
// Like org.json's put(), a null link or date leaves its key out; a miss's time is written as null.
import { DATE_STYLES, DEFAULT_REMINDER_SETTINGS } from './models'
import type { Box, MedicationMiss, MissKind, Prescription, PrescriptionStatus, Refill, ReminderSettings } from './models'
import { type MedicationData, emptyData } from './MedicationData'
import { DAYS_OF_WEEK, type DayOfWeek, parseDate, parseOffset, parseTime, parseYearMonth } from './time'

/** Thrown when the JSON isn't the app's, like org.json's JSONException or a DateTimeParseException. */
export class DataFormatError extends Error {}

type Json = Record<string, unknown>

const YEAR_MONTH_LENGTH = 7

function withoutNulls(object: Json): Json {
  return Object.fromEntries(Object.entries(object).filter(([, value]) => value !== null && value !== undefined))
}

export function recordsToJson(data: MedicationData): Json {
  return {
    prescriptions: data.prescriptions.map(prescriptionToJson),
    boxes: data.boxes.map(boxToJson),
    refills: data.refills.map(refillToJson),
    misses: data.misses.map(missToJson),
  }
}

export function dataToJson(data: MedicationData): Json {
  return {
    ...recordsToJson(data),
    reminderSettings: data.reminderSettings,
    isTutorialDone: data.isTutorialDone,
    dateStyle: data.dateStyle,
  }
}

/** Reads what [recordsToJson] writes, with everything else at its default. */
export function jsonToRecords(json: unknown): MedicationData {
  const root = object(json)
  return emptyData({
    prescriptions: array(root, 'prescriptions').map(toPrescription),
    boxes: array(root, 'boxes').map(toBox),
    refills: array(root, 'refills').map(toRefill),
    misses: array(root, 'misses').map(toMiss),
  })
}

export function jsonToData(json: unknown): MedicationData {
  const root = object(json)
  return {
    ...jsonToRecords(root),
    reminderSettings: toReminderSettings(object(root.reminderSettings)),
    // Files saved before the tutorial existed show it once too
    isTutorialDone: optBoolean(root, 'isTutorialDone', false),
    // Files saved before the date style could be chosen show year-month-day, as the app did then
    dateStyle: DATE_STYLES.find(it => it === root.dateStyle) ?? 'YEAR_MONTH_DAY',
  }
}

function prescriptionToJson(p: Prescription): Json {
  return withoutNulls({ ...p })
}

function boxToJson(box: Box): Json {
  return withoutNulls({ ...box })
}

function refillToJson(refill: Refill): Json {
  return withoutNulls({ ...refill })
}

function missToJson(miss: MedicationMiss): Json {
  return { id: miss.id, date: miss.date, kind: miss.kind, time: miss.time }
}

const PRESCRIPTION_STATUSES: PrescriptionStatus[] = ['NOT_STARTED', 'ORDERED', 'IN_PROGRESS', 'FINISHED']
const MISS_KINDS: MissKind[] = ['MISSED', 'DELAYED']

function toPrescription(json: unknown): Prescription {
  const o = object(json)
  return {
    id: int(o, 'id'),
    // Files and exports from before it was added don't have it
    medication: optString(o, 'medication', ''),
    prescribedDate: date(o, 'prescribedDate'),
    prescribedBy: string(o, 'prescribedBy'),
    pharmacy: string(o, 'pharmacy'),
    pillsPerBox: int(o, 'pillsPerBox'),
    boxesPerRepetition: int(o, 'boxesPerRepetition'),
    repetitionsTotal: int(o, 'repetitionsTotal'),
    status: oneOf(o, 'status', PRESCRIPTION_STATUSES),
    orderedDate: dateOrNull(o, 'orderedDate'),
    boughtFirstDate: dateOrNull(o, 'boughtFirstDate'),
    boughtLastDate: dateOrNull(o, 'boughtLastDate'),
    repetitionsBought: int(o, 'repetitionsBought'),
  }
}

function toBox(json: unknown): Box {
  const o = object(json)
  return {
    id: int(o, 'id'),
    prescriptionId: intOrNull(o, 'prescriptionId'),
    boxNumber: int(o, 'boxNumber'),
    boughtDate: date(o, 'boughtDate'),
    pillsLeft: int(o, 'pillsLeft'),
    pillsTotal: int(o, 'pillsTotal'),
    isExtra: boolean(o, 'isExtra'),
    openDate: dateOrNull(o, 'openDate'),
    expiryDate: yearMonthOrNull(o, 'expiryDate'),
    // Files saved before notes existed don't have them
    notes: optString(o, 'notes', ''),
  }
}

function toRefill(json: unknown): Refill {
  const o = object(json)
  return {
    id: int(o, 'id'),
    boxId: intOrNull(o, 'boxId'),
    prescriptionId: intOrNull(o, 'prescriptionId'),
    date: date(o, 'date'),
    amount: int(o, 'amount'),
    amountRemaining: int(o, 'amountRemaining'),
    isPartial: boolean(o, 'isPartial'),
  }
}

function toMiss(json: unknown): MedicationMiss {
  const o = object(json)
  return {
    id: int(o, 'id'),
    date: date(o, 'date'),
    kind: oneOf(o, 'kind', MISS_KINDS),
    time: o.time === undefined || o.time === null ? null : time(o, 'time'),
  }
}

function toReminderSettings(o: Json): ReminderSettings {
  const defaults = DEFAULT_REMINDER_SETTINGS
  return {
    isRefillEnabled: boolean(o, 'isRefillEnabled'),
    refillDay: oneOf<DayOfWeek>(o, 'refillDay', [...DAYS_OF_WEEK]),
    refillTime: time(o, 'refillTime'),
    isPickupEnabled: boolean(o, 'isPickupEnabled'),
    pickupDaysAfterOrder: int(o, 'pickupDaysAfterOrder'),
    isRunOutEnabled: boolean(o, 'isRunOutEnabled'),
    runOutDaysBefore: int(o, 'runOutDaysBefore'),
    // Files saved before the pill reminder existed don't have these
    isPillEnabled: optBoolean(o, 'isPillEnabled', defaults.isPillEnabled),
    pillTime: 'pillTime' in o ? time(o, 'pillTime') : defaults.pillTime,
    // And files saved before the expiry reminder existed don't have these
    isExpiryEnabled: optBoolean(o, 'isExpiryEnabled', defaults.isExpiryEnabled),
    expiryDaysBefore: 'expiryDaysBefore' in o ? int(o, 'expiryDaysBefore') : defaults.expiryDaysBefore,
    // And data saved before the offset was kept doesn't have this
    utcOffset: parseOffset(optString(o, 'utcOffset', '')),
  }
}

// Readers that coerce like org.json's getters, and throw where those would

function fail(message: string): never {
  throw new DataFormatError(message)
}

function object(value: unknown): Json {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? (value as Json) : fail('not an object')
}

function array(o: Json, key: string): unknown[] {
  return Array.isArray(o[key]) ? (o[key] as unknown[]) : fail(`${key} is not an array`)
}

function string(o: Json, key: string): string {
  const value = o[key]
  if (value === undefined || value === null) fail(`${key} is missing`)
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

function optString(o: Json, key: string, fallback: string): string {
  return o[key] === undefined || o[key] === null ? fallback : string(o, key)
}

function int(o: Json, key: string): number {
  const value = typeof o[key] === 'string' ? Number(o[key]) : o[key]
  return typeof value === 'number' && Number.isFinite(value) ? Math.trunc(value) : fail(`${key} is not a number`)
}

function intOrNull(o: Json, key: string): number | null {
  return o[key] === undefined || o[key] === null ? null : int(o, key)
}

function boolean(o: Json, key: string): boolean {
  const value = o[key]
  if (value === true || value === 'true') return true
  if (value === false || value === 'false') return false
  return fail(`${key} is not a boolean`)
}

function optBoolean(o: Json, key: string, fallback: boolean): boolean {
  const value = o[key]
  if (value === true || value === 'true') return true
  if (value === false || value === 'false') return false
  return fallback
}

function oneOf<T extends string>(o: Json, key: string, values: T[]): T {
  const value = string(o, key)
  return values.includes(value as T) ? (value as T) : fail(`${key} is not one of ${values}`)
}

function date(o: Json, key: string): string {
  return parseDate(string(o, key)) ?? fail(`${key} is not a date`)
}

function dateOrNull(o: Json, key: string): string | null {
  return o[key] === undefined || o[key] === null ? null : date(o, key)
}

// Taking only the year and month also reads the full dates that expiry dates were first saved as
function yearMonthOrNull(o: Json, key: string): string | null {
  if (o[key] === undefined || o[key] === null) return null
  return parseYearMonth(string(o, key).slice(0, YEAR_MONTH_LENGTH)) ?? fail(`${key} is not a month`)
}

function time(o: Json, key: string): string {
  return parseTime(string(o, key)) ?? fail(`${key} is not a time`)
}
