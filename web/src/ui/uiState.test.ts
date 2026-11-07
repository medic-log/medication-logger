// Ported from app/src/test/.../ui/ViewModelsTest.kt
import { beforeEach, describe, expect, test } from 'vitest'
import { emptyData, type MedicationData } from '../data/MedicationData'
import { MedicationRepository } from '../data/MedicationRepository'
import type { MedicationStore } from '../data/MedicationStore'
import { DEFAULT_REMINDER_SETTINGS, type MedicationMiss, type Refill, boxStatus } from '../data/models'
import { D, draftOf } from '../data/testing'
import { atTime, plusDays } from '../data/time'
import { boxesUiState } from './boxes/boxesState'
import type { ListFilter } from './components/ListFilter'
import { MainViewModel } from './main/MainViewModel'
import { TUTORIAL_STEPS } from './main/TutorialStep'
import { prescriptionsUiState } from './prescriptions/prescriptionsState'
import { type RefillLogEntry, refillsUiState } from './refills/refillsState'
import { remindersUiState } from './reminders/remindersState'

class FakeStore implements MedicationStore {
  isFailing = false
  constructor(public saved: MedicationData = emptyData()) {}
  load() { return this.saved }
  save(data: MedicationData) {
    if (this.isFailing) throw new Error('disk full')
    this.saved = data
  }
}

const today = '2026-11-02'
const clock = () => atTime(today, '12:00')
const draft = draftOf('Test medication', 'Dr Test', 'Test Pharmacy', 30, 1, 1)
let store: FakeStore
let repository: MedicationRepository
const shown = () => repository.getState().shownData
const prescriptions = (filter: ListFilter) => prescriptionsUiState(shown(), filter)
const boxes = (filter: ListFilter) => boxesUiState(shown(), filter, clock())
const mainViewModel = () => new MainViewModel(repository, () => draft)

beforeEach(() => {
  store = new FakeStore()
  repository = new MedicationRepository(store, clock)
})

describe('MainViewModel', () => {
  test('a newer version found at startup is offered after the tutorial until dismissed', () => {
    const viewModel = mainViewModel()
    viewModel.offerUpdate({ versionName: '9.9.9' })
    expect(viewModel.getState().availableUpdate).toBeNull()
    viewModel.finishTutorial()
    expect(viewModel.getState().availableUpdate).toEqual({ versionName: '9.9.9' })
    viewModel.dismissUpdate()
    expect(viewModel.getState().availableUpdate).toBeNull()
  })

  test("app opens on boxes, and links open the record's tab", () => {
    const viewModel = mainViewModel()
    expect(viewModel.getState().selectedTab).toBe('BOXES')
    viewModel.openPrescription(4)
    expect(viewModel.getState().selectedTab).toBe('PRESCRIPTIONS')
    expect(viewModel.getState().focusedPrescriptionId).toBe(4)
    viewModel.openBox(7)
    expect(viewModel.getState().selectedTab).toBe('BOXES')
    expect(viewModel.getState().focusedBoxId).toBe(7)
    expect(viewModel.getState().focusedPrescriptionId).toBeNull()
    viewModel.selectTab('REFILLS')
    expect(viewModel.getState().focusedBoxId).toBeNull()
  })

  test('the tutorial opens on first launch, leads through the tabs and stays closed once done', () => {
    const viewModel = mainViewModel()
    expect(viewModel.getState().tutorialStep?.step).toBe('LANGUAGE')
    viewModel.nextTutorialStep()
    viewModel.nextTutorialStep()
    expect(viewModel.getState().tutorialStep?.step).toBe('TABS')
    viewModel.nextTutorialStep()
    expect(viewModel.getState().selectedTab).toBe('PRESCRIPTIONS')
    viewModel.previousTutorialStep()
    viewModel.previousTutorialStep()
    expect(viewModel.getState().tutorialStep?.step).toBe('WELCOME')
    TUTORIAL_STEPS.forEach(() => viewModel.nextTutorialStep())
    expect(viewModel.getState().tutorialStep).toBeNull()
    expect(shown().prescriptions).toEqual([])
    expect(mainViewModel().getState().tutorialStep).toBeNull()
  })

  test('the tutorial shows sample records without saving them', () => {
    const viewModel = mainViewModel()
    const sample = shown()
    expect(sample.prescriptions.map(it => it.prescribedBy)).toEqual(['Dr Test'])
    expect(sample.refills.length).toBeGreaterThan(0)
    expect(sample.misses.length).toBeGreaterThan(0)
    expect(repository.getState().data.prescriptions).toEqual([])
    viewModel.updateReminderSettings({ ...DEFAULT_REMINDER_SETTINGS, pillTime: '09:30' })
    expect(shown().reminderSettings.pillTime).toBe('09:30')
    viewModel.finishTutorial()
    expect(shown()).toEqual(repository.getState().data)
  })

  test('skipping closes the tutorial for good, and help opens it again', () => {
    const viewModel = mainViewModel()
    viewModel.finishTutorial()
    expect(viewModel.getState().tutorialStep).toBeNull()
    expect(repository.getState().data.isTutorialDone).toBe(true)
    viewModel.startTutorial()
    expect(viewModel.getState().tutorialStep?.step).toBe('LANGUAGE')
  })

  test("the tutorial's settings steps change the reminder settings", () => {
    const viewModel = mainViewModel()
    const settings = { ...viewModel.getState().reminderSettings, pillTime: '09:30' }
    viewModel.updateReminderSettings(settings)
    expect(viewModel.getState().reminderSettings).toEqual(settings)
  })

  test('save failures reach the main state', () => {
    const viewModel = mainViewModel()
    store.isFailing = true
    repository.addPrescription(draft)
    expect(viewModel.getState().isSaveFailed).toBe(true)
  })
})

describe('prescriptions and boxes', () => {
  test('prescriptions filter hides finished ones, and the newest comes first', () => {
    repository.addPrescription(draft)
    repository.addPrescription({ ...draft, prescribedBy: 'Dr Second' })
    repository.buyRepetition(1, [])
    expect(prescriptions('ACTIVE').items.map(it => it.prescription.id)).toEqual([2])
    const all = prescriptions('ALL')
    expect(all.items.map(it => it.prescription.id)).toEqual([2, 1])
    expect(all.previousPrescription?.prescribedBy).toBe('Dr Second')
    expect(all.items.at(-1)!.boxes).toHaveLength(1)
  })

  test('boxes filter shows only those in use, and all lists finished ones last', () => {
    repository.addPrescription({ ...draft, pillsPerBox: 7, boxesPerRepetition: 3 })
    repository.buyRepetition(1)
    repository.refillWeekly(1)
    repository.saveAsExtra(2)
    expect(boxes('ACTIVE').items.map(it => it.box.id)).toEqual([3])
    expect(boxes('ALL').items.map(it => it.box.id)).toEqual([2, 3, 1])
    repository.backToInventory(2)
    expect(boxes('ACTIVE').items.map(it => it.box.id)).toEqual([2, 3])
  })

  test('prescriptions sort by status, then newest first', () => {
    repository.addPrescription(draft)
    repository.addPrescription({ ...draft, repetitionsTotal: 2 })
    repository.addPrescription(draft)
    repository.addPrescription(draft)
    repository.addPrescription(draft)
    repository.buyRepetition(1, [])
    repository.buyRepetition(2, [])
    repository.orderRepetition(3)
    expect(prescriptions('ALL').items.map(it => it.prescription.id)).toEqual([3, 2, 5, 4, 1])
  })

  test('boxes sort by status, then oldest date, then number', () => {
    const twoBoxes = { ...draft, boxesPerRepetition: 2 }
    const data = new D()
      .addPrescription(twoBoxes, today)
      .addPrescription({ ...twoBoxes, pillsPerBox: 7 }, today)
      .addPrescription(twoBoxes, today)
      .buyRepetition(1, today)
      .buyRepetition(2, today)
      .buyRepetition(3, plusDays(today, -1))
      .refillWeekly(1, atTime(today, '21:00'))
      .saveAsExtra(2)
      .refillWeekly(3, atTime(today, '21:00')).data
    const items = boxesUiState(data, 'ALL', clock()).items.map(it => it.box)
    expect(items.map(boxStatus)).toEqual(['IN_PROGRESS', 'EXTRA', 'NOT_STARTED', 'NOT_STARTED', 'NOT_STARTED', 'FINISHED'])
    expect(items.slice(2, 5).map(it => it.id)).toEqual([5, 6, 4])
  })

  test('boxes extra filter shows only extra boxes', () => {
    repository.addPrescription({ ...draft, pillsPerBox: 7, boxesPerRepetition: 3 })
    repository.buyRepetition(1)
    repository.saveAsExtra(2)
    expect(boxes('EXTRA').items.map(it => it.box.id)).toEqual([2])
  })

  test('box items hold their refills, and the pending amount lasts the lot until the next refill', () => {
    repository.addPrescription({ ...draft, pillsPerBox: 4, boxesPerRepetition: 2 })
    repository.buyRepetition(1)
    // Monday noon, so six doses are left before Sunday's refill
    repository.refillWeekly(1)
    expect(boxes('ACTIVE').pendingRefillAmount).toBe(2)
    repository.refillPartial(2)
    const state = boxes('ACTIVE')
    expect(state.pendingRefillAmount).toBe(0)
    expect(state.items.map(it => it.refills.map(r => r.amountRemaining))).toEqual([[2]])
  })
})

describe('refills log', () => {
  test('refills log lists entries newest first, with box names, misses and the weekly lot', () => {
    repository.addPrescription(draft)
    repository.buyRepetition(1)
    repository.refillWeekly(1)
    repository.refillWeekly(1)
    repository.logMiss('DELAYED')
    const state = refillsUiState(shown(), clock())
    expect(state.entries[0].type).toBe('miss')
    const refills = state.entries.filter(it => it.type === 'refill')
    expect(refills.map(it => it.refill.amountRemaining)).toEqual([16, 23])
    expect(refills[0].boxName).toBe(`${today} #1`)
    expect(state.lotPills).toBe(14)
  })

  const partial = (id: number, amount: number, date = today): Refill =>
    ({ id, boxId: 1, prescriptionId: 1, date, amount, amountRemaining: 0, isPartial: true })
  const logOf = (refills: Refill[], misses: MedicationMiss[] = []) =>
    refillsUiState(emptyData({ refills, misses }), clock()).entries
  const ids = (entries: RefillLogEntry[]) => entries.map(entry =>
    entry.type === 'refill' ? [entry.refill.id] : entry.type === 'group' ? entry.refills.map(it => it.refill.id) : [])
  const keys = (entries: RefillLogEntry[]) =>
    entries.map(entry => (entry.type === 'group' ? entry.members.map(it => it.key) : [entry.key]))
  const missed = (id: number, date = today): MedicationMiss => ({ id, date, kind: 'MISSED', time: null })

  test('partial refills on one day that add up to a week are grouped, newest first', () => {
    expect(ids(logOf([partial(1, 2), partial(2, 2), partial(3, 3)]))).toEqual([[3, 2, 1]])
  })

  test("partial refills that don't add up to a week aren't grouped", () => {
    expect(ids(logOf([partial(1, 3), partial(2, 3)]))).toEqual([[2], [1]])
  })

  test("partial refills on different days aren't grouped", () => {
    expect(ids(logOf([partial(1, 4, plusDays(today, -1)), partial(2, 3)]))).toEqual([[2], [1]])
  })

  test('a full refill between partial ones keeps them apart', () => {
    const full: Refill = { id: 2, boxId: 1, prescriptionId: 1, date: today, amount: 7, amountRemaining: 0, isPartial: false }
    expect(ids(logOf([partial(1, 4), full, partial(3, 3)]))).toEqual([[3], [2], [1]])
  })

  test("a week is found after a partial refill that doesn't fit in it", () => {
    expect(ids(logOf([partial(1, 5), partial(2, 3), partial(3, 4)]))).toEqual([[3, 2], [1]])
  })

  test('a missed dose joins the refill before it, on top', () => {
    expect(keys(logOf([partial(1, 7)], [missed(1, plusDays(today, 2))]))).toEqual([['miss-1', 'refill-1']])
  })

  test('a missed dose joins a group of refills', () => {
    expect(keys(logOf([partial(1, 4), partial(2, 3)], [missed(1, plusDays(today, 1))])))
      .toEqual([['miss-1', 'refill-2', 'refill-1']])
  })

  test('a dose joins the last refill made before its day, not one made that day or later', () => {
    const log = logOf([partial(1, 7, plusDays(today, -7)), partial(2, 7)], [missed(1, plusDays(today, -3)), missed(2)])
    expect(keys(log)).toEqual([['refill-2'], ['miss-2', 'miss-1', 'refill-1']])
  })

  test('a delayed dose joins the refill before it too', () => {
    const delayed: MedicationMiss = { id: 1, date: plusDays(today, 1), kind: 'DELAYED', time: null }
    expect(keys(logOf([partial(1, 7)], [delayed]))).toEqual([['miss-1', 'refill-1']])
  })

  test("a dose until the first refill's day stands alone", () => {
    const log = logOf([partial(1, 7)], [missed(1, plusDays(today, -1)), missed(2)])
    expect(keys(log)).toEqual([['miss-2'], ['refill-1'], ['miss-1']])
  })
})

test('reminders state follows settings and data', () => {
  expect(remindersUiState(shown(), clock()).runOutDate).toBeNull()
  repository.addPrescription(draft)
  repository.updateReminderSettings({ ...DEFAULT_REMINDER_SETTINGS, isPickupEnabled: true, pickupDaysAfterOrder: 2 })
  repository.orderRepetition(1)
  const state = remindersUiState(shown(), clock())
  expect(state.settings.isPickupEnabled).toBe(true)
  expect(state.pickupDate).toBe(plusDays(today, 2))
  // Noon is after today's pill time, so the pills start tomorrow
  expect(state.runOutDate).toBe(plusDays(today, 31))
  expect(state.pillsRemaining).toBe(30)
})
