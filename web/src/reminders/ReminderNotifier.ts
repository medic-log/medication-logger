import type { MedicationData } from '../data/MedicationData'
import { canBuy, boxName } from '../data/models'
import type { ReminderKind } from '../data/models'
import { boughtPillsRunOutDate, expiringBox } from '../data/ReminderCalculator'
import type { LocalDateTime } from '../data/time'
import type { Strings } from '../i18n/i18n'

const TITLES: Record<ReminderKind, string> = {
  REFILL: 'reminders_notification_refill_title',
  PICKUP: 'reminders_notification_pickup_title',
  RUN_OUT: 'reminders_notification_run_out_title',
  PILL: 'reminders_notification_pill_title',
  EXPIRY: 'reminders_notification_expiry_title',
}

/** The title and text of the notification for [kind], as the Android app's ReminderNotifier writes them. */
export function reminderNotification(kind: ReminderKind, data: MedicationData, strings: Strings, now: LocalDateTime) {
  const title = strings.t(TITLES[kind])
  switch (kind) {
    case 'REFILL':
      return { title, text: strings.t('reminders_notification_refill_text') }
    case 'PICKUP':
      return { title, text: strings.t('reminders_notification_pickup_text') }
    case 'PILL':
      return { title, text: strings.t('reminders_notification_pill_text') }
    case 'RUN_OUT':
      return {
        title,
        text: strings.t(
          data.prescriptions.some(canBuy)
            ? 'reminders_notification_run_out_repetition_text'
            : 'reminders_notification_run_out_text',
          String(boughtPillsRunOutDate(data, now)),
        ),
      }
    case 'EXPIRY': {
      const box = expiringBox(data)
      return { title, text: strings.t('reminders_notification_expiry_text', box ? boxName(box) : '', String(box?.expiryDate)) }
    }
  }
}

/**
 * Shows the notification for [kind], replacing any earlier one of the same kind. Through the service worker where
 * there is one, as mobile browsers only show notifications from there; the browser drops it when not allowed.
 */
export async function showReminder(kind: ReminderKind, data: MedicationData, strings: Strings, now: LocalDateTime) {
  if (!('Notification' in window) || Notification.permission !== 'granted') return
  const { title, text } = reminderNotification(kind, data, strings, now)
  const options: NotificationOptions = { body: text, tag: kind, icon: 'icon-192.png', badge: 'icon-192.png', lang: strings.language }
  const registration = await navigator.serviceWorker?.getRegistration()
  if (registration) await registration.showNotification(title, options)
  else new Notification(title, options)
}
