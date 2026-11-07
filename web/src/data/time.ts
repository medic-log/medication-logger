// java.time's LocalDate, YearMonth, LocalTime and LocalDateTime, as the Android app uses them.
// Dates are ISO strings, as the app writes and shows them: LocalDate `2026-11-02`, YearMonth `2026-11`,
// LocalTime `08:00`. A LocalDateTime is a number, minutes since 1970-01-01T00:00 in local time, so it compares
// and adds like one; the app never needs seconds, and dropping them doesn't change any comparison with a time
// that has none.

/** A date as `YYYY-MM-DD`. */
export type LocalDate = string
/** A month as `YYYY-MM`. */
export type YearMonth = string
/** A time of day as `HH:mm`. */
export type LocalTime = string
/** A local date and time, in minutes since 1970-01-01T00:00. */
export type LocalDateTime = number

/** Java's DayOfWeek, by its name, Monday first. */
export const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const
export type DayOfWeek = (typeof DAYS_OF_WEEK)[number]

const MINUTES_PER_DAY = 24 * 60
const MILLIS_PER_DAY = 86_400_000

const pad = (value: number, length = 2) => String(value).padStart(length, '0')

function dateParts(date: LocalDate): [number, number, number] {
  return [Number(date.slice(0, 4)), Number(date.slice(5, 7)), Number(date.slice(8, 10))]
}

export function toEpochDay(date: LocalDate): number {
  const [year, month, day] = dateParts(date)
  return Math.round(utc(year, month - 1, day) / MILLIS_PER_DAY)
}

// Date.UTC reads years 0 to 99 as 1900 to 1999, setUTCFullYear doesn't
function utc(year: number, monthIndex: number, day: number): number {
  return new Date(0).setUTCFullYear(year, monthIndex, day)
}

export function ofEpochDay(epochDay: number): LocalDate {
  const date = new Date(epochDay * MILLIS_PER_DAY)
  return `${pad(date.getUTCFullYear(), 4)}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`
}

export function dateOf(year: number, month: number, day: number): LocalDate {
  return `${pad(year, 4)}-${pad(month)}-${pad(day)}`
}

export const plusDays = (date: LocalDate, days: number): LocalDate => ofEpochDay(toEpochDay(date) + days)
export const plusWeeks = (date: LocalDate, weeks: number): LocalDate => plusDays(date, weeks * 7)

/** The weekday of [date], Monday first like DayOfWeek. */
export function dayOfWeek(date: LocalDate): DayOfWeek {
  // 1970-01-01 was a Thursday
  return DAYS_OF_WEEK[(((toEpochDay(date) + 3) % 7) + 7) % 7]
}

/** The first [day] on or after [date], like TemporalAdjusters.nextOrSame. */
export function nextOrSame(date: LocalDate, day: DayOfWeek): LocalDate {
  const ahead = (DAYS_OF_WEEK.indexOf(day) - DAYS_OF_WEEK.indexOf(dayOfWeek(date)) + 7) % 7
  return plusDays(date, ahead)
}

/** [text] as a date, like LocalDate.parse, or null when it isn't a valid `YYYY-MM-DD` date. */
export function parseDate(text: string): LocalDate | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return null
  const [year, month, day] = dateParts(text)
  if (month < 1 || month > 12 || day < 1 || day > lengthOfMonth(year, month)) return null
  return text
}

function lengthOfMonth(year: number, month: number): number {
  return new Date(utc(year, month, 0)).getUTCDate()
}

export function yearMonthOf(year: number, month: number): YearMonth {
  return `${pad(year, 4)}-${pad(month)}`
}

/** [text] as a month, like YearMonth.parse, or null when it isn't a valid `YYYY-MM` month. */
export function parseYearMonth(text: string): YearMonth | null {
  if (!/^\d{4}-\d{2}$/.test(text)) return null
  const month = Number(text.slice(5, 7))
  return month >= 1 && month <= 12 ? text : null
}

export const yearOf = (month: YearMonth | LocalDate): number => Number(month.slice(0, 4))
export const monthOf = (month: YearMonth | LocalDate): number => Number(month.slice(5, 7))
export const yearMonthFrom = (date: LocalDate): YearMonth => date.slice(0, 7)

export function plusMonths(month: YearMonth, months: number): YearMonth {
  const index = yearOf(month) * 12 + monthOf(month) - 1 + months
  return yearMonthOf(Math.floor(index / 12), (index % 12) + 1)
}

export function atEndOfMonth(month: YearMonth): LocalDate {
  return dateOf(yearOf(month), monthOf(month), lengthOfMonth(yearOf(month), monthOf(month)))
}

export function timeOf(hour: number, minute: number): LocalTime {
  return `${pad(hour)}:${pad(minute)}`
}

/** [text] as a time, like LocalTime.parse, or null when it isn't one; seconds are dropped. */
export function parseTime(text: string): LocalTime | null {
  const match = /^(\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?$/.exec(text)
  if (!match) return null
  const [hour, minute, second] = [Number(match[1]), Number(match[2]), Number(match[3] ?? 0)]
  return hour < 24 && minute < 60 && second < 60 ? timeOf(hour, minute) : null
}

export const hourOf = (time: LocalTime): number => Number(time.slice(0, 2))
export const minuteOf = (time: LocalTime): number => Number(time.slice(3, 5))
const minuteOfDay = (time: LocalTime): number => hourOf(time) * 60 + minuteOf(time)

export function atTime(date: LocalDate, time: LocalTime): LocalDateTime {
  return toEpochDay(date) * MINUTES_PER_DAY + minuteOfDay(time)
}

export function toLocalDate(dateTime: LocalDateTime): LocalDate {
  return ofEpochDay(Math.floor(dateTime / MINUTES_PER_DAY))
}

export function toLocalTime(dateTime: LocalDateTime): LocalTime {
  const minutes = ((dateTime % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
  return timeOf(Math.floor(minutes / 60), minutes % 60)
}

export const plusDaysTime = (dateTime: LocalDateTime, days: number): LocalDateTime => dateTime + days * MINUTES_PER_DAY
export const plusWeeksTime = (dateTime: LocalDateTime, weeks: number): LocalDateTime =>
  plusDaysTime(dateTime, weeks * 7)

/** The date and time of [instant] on this device's clock, to the minute. */
export function localDateTimeOf(instant: Date): LocalDateTime {
  const date = dateOf(instant.getFullYear(), instant.getMonth() + 1, instant.getDate())
  return atTime(date, timeOf(instant.getHours(), instant.getMinutes()))
}

/** The instant [dateTime] is on this device's clock, in milliseconds since the epoch. */
export function toEpochMillis(dateTime: LocalDateTime): number {
  const date = toLocalDate(dateTime)
  const time = toLocalTime(dateTime)
  return new Date(yearOf(date), monthOf(date) - 1, Number(date.slice(8, 10)), hourOf(time), minuteOf(time)).getTime()
}

/** Days from [from] to [to], like ChronoUnit.DAYS.between for dates. */
export const daysBetween = (from: LocalDate, to: LocalDate): number => toEpochDay(to) - toEpochDay(from)
