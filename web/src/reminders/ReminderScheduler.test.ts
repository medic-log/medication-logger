import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { emptyData } from '../data/MedicationData'
import { MedicationRepository } from '../data/MedicationRepository'
import { DEFAULT_REMINDER_SETTINGS, type ReminderKind } from '../data/models'
import { draftOf } from '../data/testing'
import { atTime, plusDays } from '../data/time'
import { ReminderScheduler } from './ReminderScheduler'

class MemoryStorage {
  items: Record<string, string> = {}
  getItem = (key: string) => this.items[key] ?? null
  setItem = (key: string, value: string) => { this.items[key] = value }
}

const today = '2026-11-02'
let now = atTime(today, '07:00')
let shown: ReminderKind[]
const settings = { ...DEFAULT_REMINDER_SETTINGS, isRefillEnabled: false, isRunOutEnabled: false, isExpiryEnabled: false }

function scheduler(data = emptyData({ reminderSettings: settings })) {
  const repository = new MedicationRepository({ load: () => data, save: () => {} }, () => now)
  const it = new ReminderScheduler(repository, kind => shown.push(kind), new MemoryStorage() as unknown as Storage)
  return { repository, scheduler: it }
}

beforeEach(() => {
  shown = []
  now = atTime(today, '07:00')
  vi.stubGlobal('window', { setInterval: () => 0, clearInterval: () => {} })
  vi.stubGlobal('document', { addEventListener: () => {}, removeEventListener: () => {}, visibilityState: 'visible' })
})
afterEach(() => vi.unstubAllGlobals())

describe('ReminderScheduler', () => {
  test('a reminder shows once its time comes, and then waits for the next', () => {
    const { scheduler: s } = scheduler()
    s.start()
    s.check()
    expect(shown).toEqual([])
    now = atTime(today, '08:00')
    s.check()
    expect(shown).toEqual(['PILL'])
    now = atTime(today, '08:01')
    s.check()
    expect(shown).toEqual(['PILL'])
  })

  test('a daily reminder due while the app was closed shows when it opens, once a day', () => {
    const data = emptyData({ reminderSettings: { ...settings, isPillEnabled: false, pickupDaysAfterOrder: 1 } })
    const { repository, scheduler: s } = scheduler(data)
    repository.addPrescription(draftOf('M', 'Dr', 'Ph', 30, 1, 1))
    repository.orderRepetition(1)
    now = atTime(plusDays(today, 3), '18:00')
    s.start()
    expect(shown).toEqual(['PICKUP'])
    s.check()
    expect(shown).toEqual(['PICKUP'])
    now = atTime(plusDays(today, 4), '09:00')
    s.check()
    expect(shown).toEqual(['PICKUP', 'PICKUP'])
  })

  test('changing the data moves the trigger', () => {
    const { repository, scheduler: s } = scheduler()
    s.start()
    repository.updateReminderSettings({ ...settings, pillTime: '07:30' })
    now = atTime(today, '07:30')
    s.check()
    expect(shown).toEqual(['PILL'])
  })
})
