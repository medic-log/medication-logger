import type { MedicationData } from '../../data/MedicationData'
import type { Box, ReminderSettings } from '../../data/models'
import * as R from '../../data/ReminderCalculator'
import type { LocalDate, LocalDateTime } from '../../data/time'

/** The reminder configuration, and when each reminder applies. */
export interface RemindersUiState {
  settings: ReminderSettings
  nextRefill: LocalDateTime
  nextPill: LocalDateTime
  pickupDate: LocalDate | null
  runOutDate: LocalDate | null
  pillsRemaining: number
  expiringBox: Box | null
}

/** The state of the settings tab, as RemindersViewModel works it out. */
export function remindersUiState(data: MedicationData, now: LocalDateTime): RemindersUiState {
  return {
    settings: data.reminderSettings,
    nextRefill: R.nextRefill(data.reminderSettings, now),
    nextPill: R.nextPill(data.reminderSettings, now),
    pickupDate: R.pickupDate(data),
    runOutDate: R.runOutDate(data, now),
    pillsRemaining: R.pillsRemaining(data),
    expiringBox: R.expiringBox(data),
  }
}
