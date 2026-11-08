import * as actions from './MedicationData'
import type { MedicationData } from './MedicationData'
import type { MedicationStore } from './MedicationStore'
import type { Box, MedicationMiss, MissKind, Prescription, PrescriptionDraft, Refill, ReminderSettings } from './models'
import { settingsAtOffset } from './ReminderCalculator'
import {
  type LocalDateTime,
  type LocalTime,
  type UtcOffset,
  type YearMonth,
  changeOffset,
  deviceUtcOffset,
  toLocalDate,
} from './time'

/** What the screens read from the repository at once. */
export interface RepositoryState {
  /** The saved data, which reminders follow. */
  data: MedicationData
  /** What the screens show: the saved data, or sample records in its place, with the saved reminder settings. */
  shownData: MedicationData
  /** Whether the latest change couldn't be written to storage. */
  isSaveFailed: boolean
}

/** The single source of the app's data: a port of MedicationRepository.kt. Changes apply and save at once. */
export class MedicationRepository {
  private state: RepositoryState
  private sample: MedicationData | null = null
  private readonly listeners = new Set<() => void>()

  constructor(
    private readonly store: MedicationStore,
    private readonly clock: () => LocalDateTime,
    private readonly utcOffset: () => UtcOffset = deviceUtcOffset,
  ) {
    const data = store.load()
    this.state = { data, shownData: data, isSaveFailed: false }
  }

  getState = (): RepositoryState => this.state

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  now = (): LocalDateTime => this.clock()

  /** Now at the offset the reminder times are at, which they keep when the device's changes, until the app opens. */
  reminderNow = (): LocalDateTime =>
    changeOffset(this.clock(), this.utcOffset(), this.state.data.reminderSettings.utcOffset ?? this.utcOffset())

  addPrescription = (draft: PrescriptionDraft) => this.change(it => actions.addPrescription(it, draft, this.today()))
  orderRepetition = (id: number) => this.change(it => actions.orderRepetition(it, id, this.today()))
  buyRepetition = (id: number, expiryDates: YearMonth[] = []) =>
    this.change(it => actions.buyRepetition(it, id, this.today(), expiryDates))
  refillWeekly = (boxId: number) => this.change(it => actions.refillWeekly(it, boxId, this.clock()))
  refillPartial = (boxId: number) => this.change(it => actions.refillPartial(it, boxId, this.clock()))
  saveAsExtra = (boxId: number) => this.change(it => actions.saveAsExtra(it, boxId))
  setBoxNotes = (boxId: number, notes: string) => this.change(it => actions.setBoxNotes(it, boxId, notes))
  backToInventory = (boxId: number) => this.change(it => actions.backToInventory(it, boxId))
  logMiss = (kind: MissKind, time: LocalTime | null = null) =>
    this.change(it => actions.logMiss(it, kind, this.clock(), time))
  editPrescription = (updated: Prescription) => this.change(it => actions.editPrescription(it, updated))
  deletePrescription = (id: number, keepBoxes: boolean) =>
    this.change(it => actions.deletePrescription(it, id, keepBoxes))
  editBox = (updated: Box) => this.change(it => actions.editBox(it, updated))
  deleteBox = (boxId: number, keepRefills: boolean) => this.change(it => actions.deleteBox(it, boxId, keepRefills))
  editRefill = (updated: Refill) => this.change(it => actions.editRefill(it, updated))
  deleteRefill = (refillId: number) => this.change(it => actions.deleteRefill(it, refillId))
  editMiss = (updated: MedicationMiss) => this.change(it => actions.editMiss(it, updated))
  deleteMiss = (missId: number) => this.change(it => actions.deleteMiss(it, missId))

  /** Replaces the prescriptions, boxes, refills and misses with those of [records], keeping the settings. */
  replaceRecords = (records: MedicationData) =>
    this.change(it => ({
      ...it,
      prescriptions: records.prescriptions,
      boxes: records.boxes,
      refills: records.refills,
      misses: records.misses,
    }))

  updateReminderSettings = (settings: ReminderSettings) => this.change(it => ({ ...it, reminderSettings: settings }))

  /**
   * Moves the reminder settings to the device's offset from UTC when they're at another, after a move to another
   * time zone or a change to or from summer time, keeping each reminder at the same moment. A new zone at the same
   * offset changes nothing. Returns the settings from before when that shifted an enabled pill or refill reminder,
   * or null.
   */
  followDeviceTimeZone = (): ReminderSettings | null => {
    const before = this.state.data.reminderSettings
    const after = settingsAtOffset(before, this.utcOffset(), this.reminderNow())
    if (JSON.stringify(after) === JSON.stringify(before)) return null
    this.change(it => ({ ...it, reminderSettings: after }))
    const isPillShifted = after.isPillEnabled && after.pillTime !== before.pillTime
    const isRefillShifted =
      after.isRefillEnabled && (after.refillDay !== before.refillDay || after.refillTime !== before.refillTime)
    return isPillShifted || isRefillShifted ? before : null
  }

  finishTutorial = () => this.change(actions.finishTutorial)

  /** Shows [sample] on the screens instead of the saved data, which stays as it is, or the saved data again. */
  showSample = (sample: MedicationData | null) => {
    this.sample = sample
    this.publish({ ...this.state, shownData: this.shownDataOf(this.state.data) })
  }

  private today = () => toLocalDate(this.clock())

  private shownDataOf(data: MedicationData): MedicationData {
    return this.sample ? { ...this.sample, reminderSettings: data.reminderSettings } : data
  }

  private change(action: (data: MedicationData) => MedicationData) {
    const data = action(this.state.data)
    let isSaveFailed = false
    try {
      this.store.save(data)
    } catch {
      // Storage full or unavailable; the change stays in memory and the next one tries again
      isSaveFailed = true
    }
    this.publish({ data, shownData: this.shownDataOf(data), isSaveFailed })
  }

  private publish(state: RepositoryState) {
    this.state = state
    this.listeners.forEach(listener => listener())
  }
}
