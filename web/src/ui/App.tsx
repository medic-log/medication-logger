import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react'
import type { MedicationRepository } from '../data/MedicationRepository'
import { hasAnyEnabled } from '../data/models'
import { type AppLanguage, Strings, StringsContext, currentLanguage, saveLanguage } from '../i18n/i18n'
import { showReminder } from '../reminders/ReminderNotifier'
import { ReminderScheduler } from '../reminders/ReminderScheduler'
import type { Updates } from '../update/updates'
import { type AppServices, AppServicesContext, type NotificationAccess } from './AppServices'
import { ToastHost } from './components/Toast'
import { MainScreen } from './main/MainScreen'
import { MainViewModel } from './main/MainViewModel'

const INSTALL_PARAMETER = 'install'

const notificationAccess = (): NotificationAccess =>
  'Notification' in window ? Notification.permission : 'unsupported'

/** The app: its language, shared objects, reminders and update check, around the main screen. */
export function App({ repository, updates }: { repository: MedicationRepository; updates: Updates }) {
  const [language, setLanguageState] = useState<AppLanguage>(currentLanguage)
  const { dateStyle } = useSyncExternalStore(repository.subscribe, repository.getState).data
  const strings = useMemo(() => new Strings(language, dateStyle), [language, dateStyle])
  const stringsRef = useRef(strings)
  stringsRef.current = strings
  const [viewModel] = useState(
    () =>
      new MainViewModel(repository, () => ({
        medication: stringsRef.current.t('tutorial_sample_medication'),
        prescribedBy: stringsRef.current.t('tutorial_sample_doctor'),
        pharmacy: stringsRef.current.t('tutorial_sample_pharmacy'),
        pillsPerBox: 30,
        boxesPerRepetition: 2,
        repetitionsTotal: 3,
      })),
  )
  const [access, setAccess] = useState(notificationAccess)
  const [isInstallShown, setInstallShown] = useState(
    () => new URLSearchParams(window.location.search).has(INSTALL_PARAMETER),
  )

  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  // Keeps reminders in step with the data, in the language chosen at the time they show
  useEffect(() => {
    const scheduler = new ReminderScheduler(
      repository,
      kind => void showReminder(kind, repository.getState().data, stringsRef.current, repository.now()),
      localStorage,
    )
    return scheduler.start()
  }, [repository])

  // The user may allow or block notifications in the browser's settings and come back
  useEffect(() => {
    const refresh = () => setAccess(notificationAccess())
    document.addEventListener('visibilitychange', refresh)
    let status: PermissionStatus | undefined
    navigator.permissions?.query({ name: 'notifications' }).then(it => {
      status = it
      it.addEventListener('change', refresh)
    }, () => undefined)
    return () => {
      document.removeEventListener('visibilitychange', refresh)
      status?.removeEventListener('change', refresh)
    }
  }, [])

  // Moves the reminder times into a new time zone at start and whenever the app comes back to the front
  useEffect(() => {
    viewModel.followDeviceTimeZone()
    const onVisible = () => document.visibilityState === 'visible' && viewModel.followDeviceTimeZone()
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [viewModel])

  // Checks for a newer version at each start, offered once the tutorial is out of the way; not in development
  useEffect(() => {
    if (import.meta.env.DEV) return
    void updates.findNewer().then(latest => {
      if (latest && latest !== 'failed') viewModel.offerUpdate({ versionName: latest.versionName })
    })
  }, [updates, viewModel])

  const requestNotifications = useCallback(async (): Promise<NotificationAccess> => {
    if (!('Notification' in window)) return 'unsupported'
    const result = Notification.permission === 'default' ? await Notification.requestPermission() : Notification.permission
    setAccess(result)
    return result
  }, [])

  const setLanguage = useCallback((chosen: AppLanguage) => {
    saveLanguage(chosen)
    setLanguageState(chosen)
  }, [])

  // Reminders start on, so the first finished or skipped tour asks for notifications; browsers only allow asking after a tap
  const onTutorialFinished = useCallback(() => {
    if (hasAnyEnabled(repository.getState().data.reminderSettings)) void requestNotifications()
  }, [repository, requestNotifications])

  const dismissInstall = useCallback(() => {
    setInstallShown(false)
    const url = new URL(window.location.href)
    url.searchParams.delete(INSTALL_PARAMETER)
    window.history.replaceState(null, '', url)
  }, [])

  const services: AppServices = {
    repository,
    viewModel,
    updates,
    language,
    setLanguage,
    notificationAccess: access,
    requestNotifications,
  }
  return (
    <StringsContext.Provider value={strings}>
      <AppServicesContext.Provider value={services}>
        <ToastHost>
          <MainScreen isInstallShown={isInstallShown} onInstallDismiss={dismissInstall} onTutorialFinished={onTutorialFinished} />
        </ToastHost>
      </AppServicesContext.Provider>
    </StringsContext.Provider>
  )
}
