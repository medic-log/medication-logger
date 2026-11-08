import type { MedicationRepository } from '../data/MedicationRepository'
import { REMINDER_KINDS, type ReminderKind } from '../data/models'
import { isDue, nextTrigger } from '../data/ReminderCalculator'
import { type LocalDateTime, atTime, toLocalDate } from '../data/time'

// How often to look at the clock: timers stop while a device sleeps, so the time is checked rather than waited for
const CHECK_MILLIS = 30_000
const SHOWN_KEY = 'reminders-shown'
const DAILY_REMINDER_TIME = '09:00'
const DAILY_KINDS: ReminderKind[] = ['PICKUP', 'RUN_OUT', 'EXPIRY']

/**
 * Keeps one trigger per reminder kind, worked out from the saved data with ReminderCalculator as the Android app's
 * alarms are, and shows a reminder when its time comes while the app is open. Pickup, run-out and expiry reminders,
 * which repeat daily until resolved, also show once on a due day if the app wasn't open at their time.
 */
export class ReminderScheduler {
  private triggers = new Map<ReminderKind, LocalDateTime | null>()

  constructor(
    private readonly repository: MedicationRepository,
    private readonly show: (kind: ReminderKind) => void,
    private readonly storage: Storage,
  ) {}

  start(): () => void {
    this.reschedule()
    this.catchUp()
    const unsubscribe = this.repository.subscribe(() => this.reschedule())
    const interval = window.setInterval(() => this.check(), CHECK_MILLIS)
    const onVisible = () => document.visibilityState === 'visible' && this.check()
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      unsubscribe()
      window.clearInterval(interval)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }

  /** Replaces the triggers to match the data. */
  reschedule() {
    const { data } = this.repository.getState()
    const now = this.repository.reminderNow()
    REMINDER_KINDS.forEach(kind => this.triggers.set(kind, nextTrigger(kind, data, now)))
  }

  /** Shows the reminders whose time has come, if still due, as data may have changed since they were set. */
  check() {
    const { data } = this.repository.getState()
    const now = this.repository.reminderNow()
    for (const kind of REMINDER_KINDS) {
      const trigger = this.triggers.get(kind)
      if (trigger != null && trigger <= now && isDue(kind, data, now)) this.notify(kind, now)
    }
    this.reschedule()
    this.catchUp()
  }

  private catchUp() {
    const { data } = this.repository.getState()
    const now = this.repository.reminderNow()
    const today = toLocalDate(now)
    if (now < atTime(today, DAILY_REMINDER_TIME)) return
    const shown = this.shownDays()
    for (const kind of DAILY_KINDS) {
      if (shown[kind] !== today && isDue(kind, data, now)) this.notify(kind, now)
    }
  }

  private notify(kind: ReminderKind, now: LocalDateTime) {
    const shown = this.shownDays()
    shown[kind] = toLocalDate(now)
    try {
      this.storage.setItem(SHOWN_KEY, JSON.stringify(shown))
    } catch {
      // Without storage a daily reminder may show again after a reload, which does no harm
    }
    this.show(kind)
  }

  private shownDays(): Partial<Record<ReminderKind, string>> {
    try {
      return JSON.parse(this.storage.getItem(SHOWN_KEY) ?? '{}')
    } catch {
      return {}
    }
  }
}
