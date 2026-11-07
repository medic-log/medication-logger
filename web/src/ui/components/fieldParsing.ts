import { type LocalDate, parseDate } from '../../data/time'

// Keeps multiplied totals well within range
const MAX_COUNT = 999

/** The field's count from 0 up, or null when it isn't one, like Kotlin's toIntOrNull. */
export function countOrNull(text: string): number | null {
  const trimmed = text.trim()
  if (!/^[+-]?\d+$/.test(trimmed)) return null
  const value = Number(trimmed)
  return value >= 0 && value <= MAX_COUNT ? value : null
}

/** The field's count from 1 up, or null when it isn't one. */
export function positiveCountOrNull(text: string): number | null {
  const count = countOrNull(text)
  return count !== null && count >= 1 ? count : null
}

/** The field's date as year-month-day, or null when it isn't one. */
export const dateOrNull = (text: string): LocalDate | null => parseDate(text.trim())

/** Whether the field is empty or a date. */
export const isOptionalDate = (text: string): boolean => text.trim() === '' || dateOrNull(text) !== null
