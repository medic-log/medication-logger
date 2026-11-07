// Helpers for the ported tests: a fluent wrapper over the data actions, so tests read like the Kotlin ones.
import * as a from './MedicationData'
import type { MedicationData } from './MedicationData'
import type { MedicationMiss, MissKind, Prescription, PrescriptionDraft, Box, Refill, ReminderSettings } from './models'
import { DEFAULT_REMINDER_SETTINGS } from './models'
import type { LocalDate, LocalDateTime, LocalTime, YearMonth } from './time'

/** Reminder settings with nothing on, as every reminder starts on. */
export const ALL_REMINDERS_OFF: ReminderSettings = {
  ...DEFAULT_REMINDER_SETTINGS,
  isRefillEnabled: false,
  isPickupEnabled: false,
  isRunOutEnabled: false,
  isPillEnabled: false,
  isExpiryEnabled: false,
}

export function draftOf(
  medication: string,
  prescribedBy: string,
  pharmacy: string,
  pillsPerBox: number,
  boxesPerRepetition: number,
  repetitionsTotal: number,
): PrescriptionDraft {
  return { medication, prescribedBy, pharmacy, pillsPerBox, boxesPerRepetition, repetitionsTotal }
}

export class D {
  constructor(readonly data: MedicationData = a.emptyData()) {}
  static of(overrides: Partial<MedicationData> = {}) {
    return new D(a.emptyData(overrides))
  }
  addPrescription = (draft: PrescriptionDraft, today: LocalDate) => new D(a.addPrescription(this.data, draft, today))
  orderRepetition = (id: number, today: LocalDate) => new D(a.orderRepetition(this.data, id, today))
  buyRepetition = (id: number, today: LocalDate, dates: YearMonth[] = []) =>
    new D(a.buyRepetition(this.data, id, today, dates))
  refillWeekly = (boxId: number, now: LocalDateTime) => new D(a.refillWeekly(this.data, boxId, now))
  refillPartial = (boxId: number, now: LocalDateTime) => new D(a.refillPartial(this.data, boxId, now))
  saveAsExtra = (boxId: number) => new D(a.saveAsExtra(this.data, boxId))
  setBoxNotes = (boxId: number, notes: string) => new D(a.setBoxNotes(this.data, boxId, notes))
  backToInventory = (boxId: number) => new D(a.backToInventory(this.data, boxId))
  logMiss = (kind: MissKind, now: LocalDateTime, time: LocalTime | null = null) =>
    new D(a.logMiss(this.data, kind, now, time))
  editPrescription = (updated: Prescription) => new D(a.editPrescription(this.data, updated))
  deletePrescription = (id: number, keepBoxes = false) => new D(a.deletePrescription(this.data, id, keepBoxes))
  editBox = (updated: Box) => new D(a.editBox(this.data, updated))
  deleteBox = (id: number, keepRefills = false) => new D(a.deleteBox(this.data, id, keepRefills))
  editRefill = (updated: Refill) => new D(a.editRefill(this.data, updated))
  deleteRefill = (id: number) => new D(a.deleteRefill(this.data, id))
  editMiss = (updated: MedicationMiss) => new D(a.editMiss(this.data, updated))
  deleteMiss = (id: number) => new D(a.deleteMiss(this.data, id))
  finishTutorial = () => new D(a.finishTutorial(this.data))
  lotPills = (now: LocalDateTime) => a.lotPills(this.data, now)
  pendingRefillAmount = (now: LocalDateTime) => a.pendingRefillAmount(this.data, now)
  get prescriptions() { return this.data.prescriptions }
  get boxes() { return this.data.boxes }
  get refills() { return this.data.refills }
  get misses() { return this.data.misses }
}
