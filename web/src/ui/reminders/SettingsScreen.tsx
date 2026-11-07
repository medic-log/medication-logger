import { type ReactNode, useRef, useState } from 'react'
import resources from 'virtual:android-resources'
import { readExport, writeExport } from '../../data/MedicationExport'
import { type ReminderSettings, hasAnyEnabled } from '../../data/models'
import { boxName } from '../../data/models'
import { dayOfWeek, toLocalDate, toLocalTime } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { useNow, useRepositoryState, useServices } from '../AppServices'
import { IconButton, OutlinedButton, TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import { Icon } from '../components/Icon'
import { RecordCard } from '../components/RecordCard'
import { squircleClip } from '../components/squircle'
import { Switch } from '../components/Switch'
import { TimeSelector } from '../components/TimeSelector'
import { useToast } from '../components/Toast'
import { tutorialTarget } from '../components/TutorialTarget'
import { UpdatePrompt } from '../components/UpdatePrompt'
import { WeekdaySelector } from '../components/WeekdaySelector'
import type { AvailableUpdate } from '../main/MainViewModel'
import { FeedbackDialog } from './FeedbackDialog'
import { LanguageDialog } from './LanguageDialog'
import { remindersUiState } from './remindersState'
import { SETTINGS_ACTIONS, SETTINGS_ROWS, type SettingsAction } from './SettingsAction'
import { ShareDialog } from './ShareDialog'

const MIN_DAYS = 1
const MAX_DAYS = 365
const ACTION_SIZE = 72

/** The settings tab: the configuration of the five reminders, then the app's settings. */
export function SettingsScreen({ onHelp }: { onHelp: () => void }) {
  const strings = useStrings()
  const services = useServices()
  const { repository, updates, notificationAccess, requestNotifications, language, setLanguage } = services
  const { shownData, data } = useRepositoryState()
  const state = remindersUiState(shownData, useNow())
  const settings = state.settings
  const showToast = useToast()
  const fileInput = useRef<HTMLInputElement>(null)
  const [isImportConfirmShown, setImportConfirmShown] = useState(false)
  const [importText, setImportText] = useState<string | null>(null)
  const [isLanguageShown, setLanguageShown] = useState(false)
  const [isFeedbackShown, setFeedbackShown] = useState(false)
  const [isShareShown, setShareShown] = useState(false)
  // A newer version found, waiting for the user to choose to update
  const [availableUpdate, setAvailableUpdate] = useState<AvailableUpdate | null>(null)

  const onSettingsChange = (newSettings: ReminderSettings) => {
    const isTurningOn = hasAnyEnabled(newSettings) && !hasAnyEnabled(settings)
    repository.updateReminderSettings(newSettings)
    if (isTurningOn) void requestNotifications()
  }

  const allowNotifications = async () => {
    if ((await requestNotifications()) === 'denied') showToast(strings.t('web_notifications_blocked'), true)
  }

  const exportRecords = () => {
    // Sample records shown by the tutorial are never exported
    const blob = new Blob([writeExport(data)], { type: 'application/json' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `medication-logger-${toLocalDate(repository.now())}.json`
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(link.href), 10_000)
    showToast(strings.t('settings_export_done'))
  }

  const onAction = async (action: SettingsAction) => {
    switch (action) {
      case 'HELP':
        return onHelp()
      case 'UPDATE': {
        const latest = await updates.findNewer()
        if (latest === 'failed') showToast(strings.t('settings_update_failed'))
        else if (latest === null) showToast(strings.t('settings_update_latest'))
        else setAvailableUpdate({ versionName: latest.versionName })
        return
      }
      case 'LANGUAGE':
        return setLanguageShown(true)
      case 'FEEDBACK':
        return setFeedbackShown(true)
      case 'EXPORT':
        return exportRecords()
      case 'IMPORT':
        return setImportConfirmShown(true)
      case 'SHARE':
        return setShareShown(true)
    }
  }

  const reminderNext = (text: string) => <span className="body-medium">{text}</span>
  return (
    <div className="tab-scroll">
      <div className="tab-list">
        {hasAnyEnabled(settings) && notificationAccess !== 'granted' && (
          <div className="notice error">
            <span className="body-medium">
              {strings.t(notificationAccess === 'unsupported' ? 'web_notifications_unsupported' : 'web_notifications_off')}
            </span>
            {notificationAccess !== 'unsupported' && (
              <OutlinedButton onClick={allowNotifications}>{strings.t('web_notifications_allow')}</OutlinedButton>
            )}
          </div>
        )}
        <div className="column">
          <h2 className="title-large">{strings.t('settings_section_reminders')}</h2>
          <span className="body-small on-surface-variant">{strings.t('web_reminders_note')}</span>
        </div>
        <ReminderCard
          title={strings.t('reminders_pill_title')}
          summary={strings.t('reminders_pill_summary')}
          isEnabled={settings.isPillEnabled}
          onEnabledChange={it => onSettingsChange({ ...settings, isPillEnabled: it })}
          attributes={tutorialTarget('PILL_REMINDER')}
        >
          <div>
            <TimeSelector
              time={settings.pillTime}
              dialogTitle={strings.t('reminders_pill_time_dialog_title')}
              onTimeChange={it => onSettingsChange({ ...settings, pillTime: it })}
            />
          </div>
          {settings.isPillEnabled &&
            reminderNext(strings.t('reminders_pill_next', toLocalDate(state.nextPill), toLocalTime(state.nextPill)))}
        </ReminderCard>
        <ReminderCard
          title={strings.t('reminders_refill_title')}
          summary={strings.t('reminders_refill_summary')}
          isEnabled={settings.isRefillEnabled}
          onEnabledChange={it => onSettingsChange({ ...settings, isRefillEnabled: it })}
          attributes={tutorialTarget('REFILL_REMINDER')}
        >
          <div className="flow">
            <WeekdaySelector day={settings.refillDay} onDayChange={it => onSettingsChange({ ...settings, refillDay: it })} />
            <TimeSelector
              time={settings.refillTime}
              dialogTitle={strings.t('reminders_time_dialog_title')}
              onTimeChange={it => onSettingsChange({ ...settings, refillTime: it })}
            />
          </div>
          {settings.isRefillEnabled &&
            reminderNext(
              strings.t(
                'reminders_refill_next',
                strings.dayName(dayOfWeek(toLocalDate(state.nextRefill))),
                toLocalDate(state.nextRefill),
                toLocalTime(state.nextRefill),
              ),
            )}
        </ReminderCard>
        <ReminderCard
          title={strings.t('reminders_run_out_title')}
          summary={strings.t('reminders_run_out_summary')}
          isEnabled={settings.isRunOutEnabled}
          onEnabledChange={it => onSettingsChange({ ...settings, isRunOutEnabled: it })}
        >
          <DaysStepper
            reminderName={strings.t('reminders_run_out_title')}
            days={settings.runOutDaysBefore}
            onDaysChange={it => onSettingsChange({ ...settings, runOutDaysBefore: it })}
          />
          {reminderNext(
            state.runOutDate ? strings.t('reminders_run_out_date', state.runOutDate) : strings.t('reminders_run_out_none'),
          )}
          {state.runOutDate && (
            <span className="body-small">
              {strings.plural('reminders_pills_remaining', state.pillsRemaining, state.pillsRemaining)}
            </span>
          )}
        </ReminderCard>
        <ReminderCard
          title={strings.t('reminders_pickup_title')}
          summary={strings.t('reminders_pickup_summary')}
          isEnabled={settings.isPickupEnabled}
          onEnabledChange={it => onSettingsChange({ ...settings, isPickupEnabled: it })}
        >
          <DaysStepper
            reminderName={strings.t('reminders_pickup_title')}
            days={settings.pickupDaysAfterOrder}
            onDaysChange={it => onSettingsChange({ ...settings, pickupDaysAfterOrder: it })}
          />
          {reminderNext(
            state.pickupDate ? strings.t('reminders_pickup_next', state.pickupDate) : strings.t('reminders_pickup_none'),
          )}
        </ReminderCard>
        <ReminderCard
          title={strings.t('reminders_expiry_title')}
          summary={strings.t('reminders_expiry_summary')}
          isEnabled={settings.isExpiryEnabled}
          onEnabledChange={it => onSettingsChange({ ...settings, isExpiryEnabled: it })}
        >
          <DaysStepper
            reminderName={strings.t('reminders_expiry_title')}
            days={settings.expiryDaysBefore}
            onDaysChange={it => onSettingsChange({ ...settings, expiryDaysBefore: it })}
          />
          {reminderNext(
            state.expiringBox
              ? strings.t('reminders_expiry_next', boxName(state.expiringBox), String(state.expiringBox.expiryDate))
              : strings.t('reminders_expiry_none'),
          )}
        </ReminderCard>
        <h2 className="title-large">{strings.t('settings_section_settings')}</h2>
        <div className="settings-actions" style={{ ['--squircle' as string]: squircleClip(ACTION_SIZE) }}>
          {SETTINGS_ROWS.map(row => (
            <div key={row.join()} className="flow">
              {row.map(action => (
                <button key={action} type="button" className="settings-action" onClick={() => void onAction(action)}>
                  {/* The label names the button, so the icon is decorative */}
                  <Icon name={SETTINGS_ACTIONS[action].icon} />
                  <span className="label-medium">{strings.t(SETTINGS_ACTIONS[action].label)}</span>
                </button>
              ))}
            </div>
          ))}
        </div>
        <span className="body-small on-surface-variant version">{strings.t('settings_version', resources.versionName)}</span>
      </div>
      <input
        ref={fileInput}
        type="file"
        // Some systems don't know .json as application/json, so any text file can be picked
        accept=".json,application/json,text/plain"
        hidden
        onChange={async event => {
          const file = event.target.files?.[0]
          event.target.value = ''
          if (!file) return
          try {
            setImportText(await file.text())
          } catch {
            showToast(strings.t('settings_import_failed'))
          }
        }}
      />
      {isFeedbackShown && <FeedbackDialog onDismiss={() => setFeedbackShown(false)} />}
      {isShareShown && <ShareDialog url={resources.siteUrl} onDismiss={() => setShareShown(false)} />}
      {isLanguageShown && (
        <LanguageDialog
          selected={language}
          onConfirm={chosen => {
            setLanguageShown(false)
            if (chosen !== language) setLanguage(chosen)
          }}
          onDismiss={() => setLanguageShown(false)}
        />
      )}
      <UpdatePrompt update={availableUpdate} onDismiss={() => setAvailableUpdate(null)} />
      {isImportConfirmShown && (
        <Dialog
          title={strings.t('settings_import_confirm_title')}
          onDismiss={() => setImportConfirmShown(false)}
          buttons={
            <>
              <TextButton onClick={() => setImportConfirmShown(false)}>{strings.t('edit_cancel')}</TextButton>
              <TextButton
                onClick={() => {
                  setImportConfirmShown(false)
                  fileInput.current?.click()
                }}
              >
                {strings.t('settings_import_confirm')}
              </TextButton>
            </>
          }
        >
          {strings.t('settings_import_confirm_text')}
        </Dialog>
      )}
      {importText !== null && (
        <Dialog
          title={strings.t('settings_import_replace_title')}
          onDismiss={() => setImportText(null)}
          buttons={
            <>
              <TextButton onClick={() => setImportText(null)}>{strings.t('edit_cancel')}</TextButton>
              <TextButton
                onClick={() => {
                  const records = readExport(importText)
                  setImportText(null)
                  if (records) repository.replaceRecords(records)
                  showToast(strings.t(records ? 'settings_import_done' : 'settings_import_failed'))
                }}
              >
                {strings.t('settings_import_replace')}
              </TextButton>
            </>
          }
        >
          {strings.t('settings_import_replace_text')}
        </Dialog>
      )}
    </div>
  )
}

function ReminderCard({
  title,
  summary,
  isEnabled,
  onEnabledChange,
  attributes,
  children,
}: {
  title: string
  summary: string
  isEnabled: boolean
  onEnabledChange: (isEnabled: boolean) => void
  attributes?: Record<string, string>
  children: ReactNode
}) {
  return (
    <RecordCard attributes={attributes}>
      <div className="row center">
        <div className="column grow" style={{ gap: 0, paddingRight: 16 }}>
          <span className="title-medium">{title}</span>
          <span className="body-small">{summary}</span>
        </div>
        <Switch checked={isEnabled} onChange={onEnabledChange} label={title} />
      </div>
      {children}
    </RecordCard>
  )
}

function DaysStepper({ reminderName, days, onDaysChange }: { reminderName: string; days: number; onDaysChange: (days: number) => void }) {
  const strings = useStrings()
  return (
    <div className="stepper">
      <IconButton
        onClick={() => onDaysChange(days - 1)}
        disabled={days <= MIN_DAYS}
        aria-label={strings.t('reminders_fewer_days', reminderName)}
      >
        <Icon name="ic_remove" />
      </IconButton>
      <span className="body-large">{strings.plural('reminders_days', days, days)}</span>
      <IconButton
        onClick={() => onDaysChange(days + 1)}
        disabled={days >= MAX_DAYS}
        aria-label={strings.t('reminders_more_days', reminderName)}
      >
        <Icon name="ic_add" />
      </IconButton>
    </div>
  )
}
