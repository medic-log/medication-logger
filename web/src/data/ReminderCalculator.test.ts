// Ported from app/src/test/.../data/ReminderCalculatorTest.kt
import { describe, expect, test } from 'vitest'
import { DEFAULT_REMINDER_SETTINGS, REMINDER_KINDS } from './models'
import { emptyData } from './MedicationData'
import * as R from './ReminderCalculator'
import { ALL_REMINDERS_OFF, D, draftOf } from './testing'
import { atTime, plusDays, plusWeeks } from './time'

// A Monday
const today = '2026-11-02'
// Before the pill time, so today's dose is still to come
const morning = atTime(today, '07:00')
const noon = atTime(today, '12:00')
const draft = draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 1, 2)
const settings = (overrides = {}) => ({ ...DEFAULT_REMINDER_SETTINGS, ...overrides })

describe('ReminderCalculator', () => {
  test('refill reminder is next on the chosen weekday and time', () => {
    const s = settings({ isRefillEnabled: true, refillDay: 'WEDNESDAY', refillTime: '08:30' })
    expect(R.nextRefill(s, noon)).toBe(atTime('2026-11-04', '08:30'))
  })

  test("refill reminder moves to next week once today's time has passed", () => {
    const s = settings({ refillDay: 'MONDAY', refillTime: '09:00' })
    expect(R.nextRefill(s, atTime(today, '08:00'))).toBe(atTime(today, '09:00'))
    expect(R.nextRefill(s, atTime(today, '09:00'))).toBe(atTime(plusWeeks(today, 1), '09:00'))
  })

  test('pill reminder is next today until its time has passed, then tomorrow', () => {
    const data = emptyData({ reminderSettings: settings({ isPillEnabled: true, pillTime: '21:30' }) })
    expect(R.nextTrigger('PILL', data, noon)).toBe(atTime(today, '21:30'))
    expect(R.nextTrigger('PILL', data, atTime(today, '21:30'))).toBe(atTime(plusDays(today, 1), '21:30'))
    expect(R.isDue('PILL', data, atTime(today, '21:30'))).toBe(true)
  })

  test("disabled reminders aren't scheduled", () => {
    const data = D.of({ reminderSettings: ALL_REMINDERS_OFF }).addPrescription(draft, today).orderRepetition(1, today)
    for (const kind of REMINDER_KINDS) {
      expect(R.nextTrigger(kind, data.data, noon)).toBeNull()
      expect(kind === 'REFILL' && R.isDue(kind, data.data, noon)).toBe(false)
    }
  })

  const expiryOnly = D.of({ reminderSettings: { ...ALL_REMINDERS_OFF, isExpiryEnabled: true } })
    .addPrescription(draft, today)

  test('expiry reminder fires the chosen days before its month ends, then daily', () => {
    const data = expiryOnly.buyRepetition(1, today, ['2027-01']).data
    const dueDay = '2026-12-02'
    expect(R.nextTrigger('EXPIRY', data, noon)).toBe(atTime(dueDay, '09:00'))
    expect(R.isDue('EXPIRY', data, noon)).toBe(false)
    expect(R.isDue('EXPIRY', data, atTime(dueDay, '09:00'))).toBe(true)
    expect(R.nextTrigger('EXPIRY', data, atTime(dueDay, '09:00'))).toBe(atTime(plusDays(dueDay, 1), '09:00'))
  })

  test('expiry reminder follows the first box to expire, extra ones included', () => {
    const data = expiryOnly.buyRepetition(1, today, ['2028-01']).buyRepetition(1, today, ['2027-01']).saveAsExtra(2)
    expect(R.expiringBox(data.data)?.id).toBe(2)
  })

  test('expiry reminder stops for an empty box, and without any date', () => {
    const bought = expiryOnly.buyRepetition(1, today, ['2027-01'])
    const emptied = bought.editBox({ ...bought.boxes[0], pillsLeft: 0 })
    const undated = expiryOnly.buyRepetition(1, today)
    expect(R.nextTrigger('EXPIRY', emptied.data, noon)).toBeNull()
    expect(R.nextTrigger('EXPIRY', undated.data, noon)).toBeNull()
  })

  test('a missed dose after a refill makes the medication last a day longer', () => {
    const refilled = new D().addPrescription(draft, today).buyRepetition(1, today).refillWeekly(1, morning)
    const before = R.runOutDate(refilled.data, morning)!
    const missed = refilled.logMiss('MISSED', morning)
    expect(R.runOutDate(missed.data, morning)).toBe(plusDays(before, 1))
    expect(missed.boxes).toEqual(refilled.boxes)
  })

  test('a delayed dose, or one missed before any refill, changes no dates', () => {
    const bought = new D().addPrescription(draft, today).buyRepetition(1, today)
    const before = R.runOutDate(bought.data, morning)
    const refilled = bought.refillWeekly(1, morning)
    expect(R.runOutDate(bought.logMiss('MISSED', morning).data, morning)).toBe(before)
    expect(R.runOutDate(refilled.logMiss('DELAYED', morning).data, morning)).toBe(R.runOutDate(refilled.data, morning))
  })

  test('pickup reminder fires the chosen days after ordering, then daily until bought', () => {
    const ordered = D.of({ reminderSettings: settings({ isPickupEnabled: true, pickupDaysAfterOrder: 3 }) })
      .addPrescription(draft, today).orderRepetition(1, today)
    const dueDay = plusDays(today, 3)
    expect(R.nextTrigger('PICKUP', ordered.data, noon)).toBe(atTime(dueDay, '09:00'))
    expect(R.isDue('PICKUP', ordered.data, noon)).toBe(false)
    expect(R.isDue('PICKUP', ordered.data, atTime(dueDay, '09:00'))).toBe(true)
    expect(R.nextTrigger('PICKUP', ordered.data, atTime(dueDay, '09:00'))).toBe(atTime(plusDays(dueDay, 1), '09:00'))
    const bought = ordered.buyRepetition(1, dueDay)
    expect(R.nextTrigger('PICKUP', bought.data, atTime(dueDay, '09:00'))).toBeNull()
    expect(R.isDue('PICKUP', bought.data, atTime(dueDay, '09:00'))).toBe(false)
  })

  test('nothing logged has no run-out date', () => {
    expect(R.runOutDate(emptyData(), morning)).toBeNull()
  })

  test('run-out date counts one pill a day, including repetitions not bought', () => {
    const data = new D().addPrescription(draft, today).buyRepetition(1, today).data
    expect(R.pillsRemaining(data)).toBe(60)
    expect(R.runOutDate(data, morning)).toBe(plusDays(today, 60))
  })

  test('run-out date starts after the days the last refill covers', () => {
    const data = new D().addPrescription(draft, today).buyRepetition(1, today).refillWeekly(1, morning).data
    expect(R.pillsRemaining(data)).toBe(53)
    expect(R.runOutDate(data, morning)).toBe(plusDays(today, 7 + 53))
  })

  test('an old refill no longer covers any days', () => {
    const data = new D().addPrescription(draft, today).buyRepetition(1, today).refillWeekly(1, morning).data
    const later = plusDays(today, 10)
    expect(R.runOutDate(data, atTime(later, '07:00'))).toBe(plusDays(later, 53))
  })

  test("extra boxes don't count towards the run-out date", () => {
    const data = new D().addPrescription(draft, today).buyRepetition(1, today).saveAsExtra(1).data
    expect(R.pillsRemaining(data)).toBe(30)
  })

  test('run-out reminder fires the chosen days before the bought pills run out', () => {
    const data = D.of({ reminderSettings: settings({ isRunOutEnabled: true, runOutDaysBefore: 14 }) })
      .addPrescription(draft, today).buyRepetition(1, today).data
    // Noon is after today's pill time, so the pills start tomorrow
    const dueDay = plusDays(today, 1 + 30 - 14)
    expect(R.nextTrigger('RUN_OUT', data, noon)).toBe(atTime(dueDay, '09:00'))
    expect(R.isDue('RUN_OUT', data, noon)).toBe(false)
  })

  test('run-out reminder is due daily once the date is close', () => {
    const data = D.of({ reminderSettings: settings({ isRunOutEnabled: true, runOutDaysBefore: 14 }) })
      .addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 7, 1, 1), today).data
    const now = atTime(today, '10:00')
    expect(R.isDue('RUN_OUT', data, now)).toBe(true)
    expect(R.nextTrigger('RUN_OUT', data, now)).toBe(atTime(plusDays(today, 1), '09:00'))
  })
})
