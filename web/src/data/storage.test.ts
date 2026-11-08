// Ported from JsonFileMedicationStoreTest, MedicationExportTest, MedicationRepositoryTest and FeedbackTest
import { beforeEach, describe, expect, test } from 'vitest'
import { feedbackJson, isValidEmail, sendResultOf } from './Feedback'
import { emptyData, type MedicationData } from './MedicationData'
import { readExport, writeExport } from './MedicationExport'
import { MedicationRepository } from './MedicationRepository'
import { DATA_KEY, LocalStorageMedicationStore, type MedicationStore } from './MedicationStore'
import { DEFAULT_REMINDER_SETTINGS } from './models'
import { D, draftOf } from './testing'
import { atTime } from './time'

/** An in-memory Storage, as localStorage is a browser object. */
class MemoryStorage implements Storage {
  private items = new Map<string, string>()
  isFull = false
  get length() { return this.items.size }
  clear() { this.items.clear() }
  getItem(key: string) { return this.items.get(key) ?? null }
  key(index: number) { return [...this.items.keys()][index] ?? null }
  removeItem(key: string) { this.items.delete(key) }
  setItem(key: string, value: string) {
    if (this.isFull) throw new DOMException('full', 'QuotaExceededError')
    this.items.set(key, value)
  }
}

const day = '2026-11-02'
let storage: MemoryStorage
const store = () => new LocalStorageMedicationStore(storage)
const rewrite = (change: (json: any) => void) => {
  const json = JSON.parse(storage.getItem(DATA_KEY)!)
  change(json)
  storage.setItem(DATA_KEY, JSON.stringify(json))
}

beforeEach(() => {
  storage = new MemoryStorage()
})

describe('LocalStorageMedicationStore', () => {
  test('no data means empty data', () => {
    expect(store().load()).toEqual(emptyData())
  })

  test('everything is read back as it was saved', () => {
    const data = D.of({
      reminderSettings: {
        isRefillEnabled: true, refillDay: 'FRIDAY', refillTime: '07:45', isPickupEnabled: true,
        pickupDaysAfterOrder: 5, isRunOutEnabled: true, runOutDaysBefore: 20, isPillEnabled: true,
        pillTime: '21:15', isExpiryEnabled: false, expiryDaysBefore: 30, utcOffset: '+05:30',
      },
    })
      .addPrescription(draftOf('Ibuprofen "600" mg', 'Dr "Quote"', 'Pharmacy, ünï', 7, 1, 2), day)
      .addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 2, 1), day)
      .buyRepetition(1, day)
      .orderRepetition(1, day)
      .buyRepetition(2, day)
      .refillWeekly(1, atTime(day, '21:00'))
      .saveAsExtra(3)
      .setBoxNotes(3, 'For travel, "spare"')
      .logMiss('MISSED', atTime(day, '21:00'))
      .finishTutorial().data
    store().save(data)
    expect(store().load()).toEqual(data)
  })

  test('a file saved before the expiry reminder existed starts it on', () => {
    store().save(emptyData())
    rewrite(json => {
      delete json.reminderSettings.isExpiryEnabled
      delete json.reminderSettings.expiryDaysBefore
    })
    expect(store().load()).toEqual(emptyData())
  })

  test('open and expiry dates load back', () => {
    const data = new D()
      .addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 2, 1), day)
      .buyRepetition(1, day, ['2030-01', '2031-06'])
      .refillWeekly(1, atTime(day, '21:00')).data
    store().save(data)
    expect(store().load()).toEqual(data)
  })

  test('an expiry saved as a full date loads as its year and month', () => {
    const data = new D()
      .addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 1, 1), day)
      .buyRepetition(1, day, ['2030-01']).data
    store().save(data)
    rewrite(json => { json.boxes[0].expiryDate = '2030-01-31' })
    expect(store().load()).toEqual(data)
  })

  test('boxes and refills that lost their links load back without them', () => {
    const data = new D()
      .addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 2, 1), day)
      .buyRepetition(1, day)
      .refillWeekly(1, atTime(day, '21:00'))
      .refillWeekly(2, atTime(day, '21:00'))
      .deleteBox(1, true)
      .deletePrescription(1, true).data
    store().save(data)
    expect(store().load()).toEqual(data)
  })

  test('a file saved before the pill reminder existed still loads', () => {
    const data = new D().addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 1, 1), day).data
    store().save(data)
    rewrite(json => {
      delete json.reminderSettings.isPillEnabled
      delete json.reminderSettings.pillTime
    })
    expect(store().load()).toEqual(data)
  })

  test('a prescription saved before it had a medication loads with a blank one', () => {
    store().save(new D().addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 1, 1), day).data)
    rewrite(json => { delete json.prescriptions[0].medication })
    expect(store().load().prescriptions[0].medication).toBe('')
  })

  test('an unreadable file is kept aside instead of being overwritten', () => {
    storage.setItem(DATA_KEY, 'not json')
    expect(store().load()).toEqual(emptyData())
    expect(storage.getItem(`${DATA_KEY}.unreadable`)).toBe('not json')
    expect(storage.getItem(DATA_KEY)).toBeNull()
  })

  test("the Android app's file loads, with absent links and dates as null", () => {
    // As JsonFileMedicationStore writes it: null values left out, a miss's time written as null
    const android = `{"prescriptions":[{"id":1,"medication":"Med","prescribedDate":"2026-11-02","prescribedBy":"Dr",
      "pharmacy":"Ph","pillsPerBox":30,"boxesPerRepetition":1,"repetitionsTotal":2,"status":"IN_PROGRESS",
      "boughtFirstDate":"2026-11-02","boughtLastDate":"2026-11-02","repetitionsBought":1}],
      "boxes":[{"id":1,"boxNumber":1,"boughtDate":"2026-11-02","pillsLeft":23,"pillsTotal":30,"isExtra":false,
      "openDate":"2026-11-02","expiryDate":"2028-06","notes":""}],
      "refills":[{"id":1,"boxId":1,"date":"2026-11-02","amount":7,"amountRemaining":23,"isPartial":false}],
      "misses":[{"id":1,"date":"2026-11-03","kind":"DELAYED","time":"09:30"},{"id":2,"date":"2026-11-04","kind":"MISSED","time":null}],
      "reminderSettings":{"isRefillEnabled":true,"refillDay":"SUNDAY","refillTime":"20:00","isPickupEnabled":true,
      "pickupDaysAfterOrder":4,"isRunOutEnabled":true,"runOutDaysBefore":14,"isPillEnabled":true,"pillTime":"08:00",
      "isExpiryEnabled":true,"expiryDaysBefore":60},"isTutorialDone":true}`
    storage.setItem(DATA_KEY, android)
    const data = store().load()
    expect(data.prescriptions[0].orderedDate).toBeNull()
    expect(data.boxes[0].prescriptionId).toBeNull()
    expect(data.refills[0].prescriptionId).toBeNull()
    expect(data.misses.map(it => it.time)).toEqual(['09:30', null])
    expect(data.reminderSettings).toEqual(DEFAULT_REMINDER_SETTINGS)
    expect(data.isTutorialDone).toBe(true)
  })
})

describe('MedicationExport', () => {
  const records = new D()
    .addPrescription(draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 7, 1, 2), day)
    .buyRepetition(1, day)
    .refillWeekly(1, atTime(day, '21:00'))
    .logMiss('MISSED', atTime(day, '21:00'))
    .logMiss('DELAYED', atTime(day, '22:00'), '23:30').data

  test('records are read back as they were exported', () => {
    expect(readExport(writeExport(records))).toEqual(records)
  })

  test('settings and tutorial progress are left out', () => {
    const data = { ...records, reminderSettings: { ...DEFAULT_REMINDER_SETTINGS, isPillEnabled: false }, isTutorialDone: true }
    const json = JSON.parse(writeExport(data))
    expect(json).not.toHaveProperty('reminderSettings')
    expect(json).not.toHaveProperty('isTutorialDone')
  })

  test("text that isn't an export is rejected", () => {
    expect(readExport('not json')).toBeNull()
    expect(readExport('{}')).toBeNull()
  })
})

class FakeStore implements MedicationStore {
  saved: MedicationData | null = null
  isFailing = false
  load() { return this.saved ?? emptyData() }
  save(data: MedicationData) {
    if (this.isFailing) throw new Error('disk full')
    this.saved = data
  }
}

describe('MedicationRepository', () => {
  const clock = () => atTime(day, '00:00')
  const draft = draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 1, 1)
  let fake: FakeStore
  const repository = () => new MedicationRepository(fake, clock)
  beforeEach(() => { fake = new FakeStore() })

  test('actions are dated by the clock', () => {
    const r = repository()
    r.addPrescription(draft)
    expect(r.getState().data.prescriptions[0].prescribedDate).toBe(day)
  })

  test('changes survive a restart', () => {
    const first = repository()
    first.addPrescription(draft)
    first.buyRepetition(1)
    first.refillWeekly(1)
    first.logMiss('DELAYED')
    expect(repository().getState().data).toEqual(first.getState().data)
    expect(repository().getState().data.boxes[0].pillsLeft).toBe(23)
  })

  test('replacing records keeps the settings and tutorial progress', () => {
    const settings = { ...DEFAULT_REMINDER_SETTINGS, isPillEnabled: false }
    fake.saved = emptyData({ reminderSettings: settings, isTutorialDone: true })
    const r = repository()
    r.addPrescription(draft)
    const records = new D().addPrescription(draftOf('Test medication', 'Dr Other', 'Other', 7, 1, 1), day).data
    r.replaceRecords(records)
    expect(r.getState().data.prescriptions).toEqual(records.prescriptions)
    expect(r.getState().data.reminderSettings).toEqual(settings)
    expect(r.getState().data.isTutorialDone).toBe(true)
    expect(fake.saved).toEqual(r.getState().data)
  })

  test('a failed save is reported and cleared by the next good one', () => {
    const r = repository()
    fake.isFailing = true
    r.addPrescription(draft)
    expect(r.getState().isSaveFailed).toBe(true)
    expect(r.getState().data.prescriptions).toHaveLength(1)
    fake.isFailing = false
    r.orderRepetition(1)
    expect(r.getState().isSaveFailed).toBe(false)
    expect(fake.saved).toEqual(r.getState().data)
  })

  test('localStorage running out of room counts as a failed save', () => {
    const r = new MedicationRepository(new LocalStorageMedicationStore(storage), clock)
    storage.isFull = true
    r.addPrescription(draft)
    expect(r.getState().isSaveFailed).toBe(true)
  })

  const at = (offset: string) => new MedicationRepository(fake, clock, () => offset)

  test("settings without an offset take the device's without shifting", () => {
    const r = at('Z')
    expect(r.followDeviceTimeZone()).toBeNull()
    expect(r.getState().data.reminderSettings).toEqual({ ...DEFAULT_REMINDER_SETTINGS, utcOffset: 'Z' })
  })

  test('a new time zone shifts the reminders to the same moments', () => {
    const before = { ...DEFAULT_REMINDER_SETTINGS, refillDay: 'SUNDAY', refillTime: '20:00', pillTime: '07:30', utcOffset: '+09:00' } as const
    fake.saved = emptyData({ reminderSettings: before })
    const r = at('Z')
    expect(r.followDeviceTimeZone()).toEqual(before)
    const after = r.getState().data.reminderSettings
    expect(after.pillTime).toBe('22:30')
    expect(after.refillDay).toBe('SUNDAY')
    expect(after.refillTime).toBe('11:00')
    expect(after.utcOffset).toBe('Z')
    expect(fake.saved).toEqual(r.getState().data)
    expect(r.followDeviceTimeZone()).toBeNull()
  })

  test('a shift past midnight moves the refill to the day before', () => {
    fake.saved = emptyData({
      reminderSettings: { ...DEFAULT_REMINDER_SETTINGS, refillDay: 'MONDAY', refillTime: '02:00', utcOffset: '+05:00' },
    })
    const r = at('Z')
    r.followDeviceTimeZone()
    const after = r.getState().data.reminderSettings
    expect(after.refillDay).toBe('SUNDAY')
    expect(after.refillTime).toBe('21:00')
  })

  test('a new time zone at the same offset changes nothing', () => {
    const settings = { ...DEFAULT_REMINDER_SETTINGS, utcOffset: 'Z' }
    fake.saved = emptyData({ reminderSettings: settings })
    const r = at('Z')
    expect(r.followDeviceTimeZone()).toBeNull()
    expect(r.getState().data.reminderSettings).toEqual(settings)
  })

  test('a change to summer time shifts the reminders an hour later', () => {
    fake.saved = emptyData({ reminderSettings: { ...DEFAULT_REMINDER_SETTINGS, pillTime: '07:30', utcOffset: '+01:00' } })
    const r = at('+02:00')
    expect(r.followDeviceTimeZone()?.pillTime).toBe('07:30')
    expect(r.getState().data.reminderSettings.pillTime).toBe('08:30')
    expect(r.getState().data.reminderSettings.utcOffset).toBe('+02:00')
  })
})

describe('Feedback', () => {
  test("feedback json has the worker's fields and comes from the web", () => {
    const json = JSON.parse(feedbackJson({ title: 'Crash', topic: 'bug', version: '0.3.3', message: 'It "closes"\non start', email: '' }))
    expect(json).toEqual({ title: 'Crash', topic: 'bug', version: '0.3.3', message: 'It "closes"\non start', source: 'web' })
  })

  test('an email is sent when given', () => {
    const json = JSON.parse(feedbackJson({ title: 'How?', topic: 'question', version: '0.3.3', message: 'How?', email: 'me@example.org' }))
    expect(json.email).toBe('me@example.org')
  })

  test('only email-like addresses are valid', () => {
    expect(isValidEmail('me@example.org')).toBe(true)
    expect(isValidEmail('me@example')).toBe(false)
    expect(isValidEmail('me example@org.com')).toBe(false)
    expect(isValidEmail('@example.org')).toBe(false)
    expect(isValidEmail('a'.repeat(250) + '@b.org')).toBe(false)
  })

  test("the worker's answer tells whether to retry later", () => {
    expect(sendResultOf(201)).toBe('SENT')
    expect(sendResultOf(429)).toBe('UNAVAILABLE')
    expect(sendResultOf(503)).toBe('UNAVAILABLE')
    expect(sendResultOf(400)).toBe('REJECTED')
  })
})
