import { createContext, useContext, useEffect, useState, useSyncExternalStore } from 'react'
import type { MedicationRepository, RepositoryState } from '../data/MedicationRepository'
import type { LocalDateTime } from '../data/time'
import type { AppLanguage } from '../i18n/i18n'
import type { Updates } from '../update/updates'
import type { MainUiState, MainViewModel } from './main/MainViewModel'

/** Whether reminders can reach the user: the browser's notification permission, or that it has none. */
export type NotificationAccess = NotificationPermission | 'unsupported'

/** What the screens share, as the Android app's Application holds it. */
export interface AppServices {
  repository: MedicationRepository
  viewModel: MainViewModel
  updates: Updates
  language: AppLanguage
  setLanguage: (language: AppLanguage) => void
  notificationAccess: NotificationAccess
  /** Asks for the notification permission, which browsers only allow after a tap. */
  requestNotifications: () => Promise<NotificationAccess>
}

export const AppServicesContext = createContext<AppServices | null>(null)

export function useServices(): AppServices {
  const services = useContext(AppServicesContext)
  if (!services) throw new Error('No AppServices')
  return services
}

export function useRepositoryState(): RepositoryState {
  const { repository } = useServices()
  return useSyncExternalStore(repository.subscribe, repository.getState)
}

export function useMainState(): MainUiState {
  const { viewModel } = useServices()
  return useSyncExternalStore(viewModel.subscribe, viewModel.getState)
}

// Often enough that "now" in a screen is never more than a few seconds behind a minute change
const NOW_TICK_MILLIS = 15_000

/** The time now, to the minute, kept current while the screen shows. */
export function useNow(): LocalDateTime {
  const { repository } = useServices()
  const [now, setNow] = useState(repository.now)
  useEffect(() => {
    const tick = () => setNow(repository.now())
    const interval = window.setInterval(tick, NOW_TICK_MILLIS)
    document.addEventListener('visibilitychange', tick)
    return () => {
      window.clearInterval(interval)
      document.removeEventListener('visibilitychange', tick)
    }
  }, [repository])
  return now
}
