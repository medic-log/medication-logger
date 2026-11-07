import { useState } from 'react'
import { type LocalTime, hourOf, minuteOf, timeOf } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { OutlinedButton, TextButton } from './Buttons'
import { Dialog } from './Dialog'

/** A button showing [time] that opens a dialog to change it. */
export function TimeSelector({
  time,
  dialogTitle,
  onTimeChange,
}: {
  time: LocalTime
  dialogTitle: string
  onTimeChange: (time: LocalTime) => void
}) {
  const strings = useStrings()
  const [isDialogOpen, setDialogOpen] = useState(false)
  return (
    <>
      <OutlinedButton onClick={() => setDialogOpen(true)}>{strings.t('reminders_choose_time', time)}</OutlinedButton>
      {isDialogOpen && (
        <TimePickerDialog
          time={time}
          title={dialogTitle}
          onConfirm={it => {
            onTimeChange(it)
            setDialogOpen(false)
          }}
          onDismiss={() => setDialogOpen(false)}
        />
      )}
    </>
  )
}

const fieldName = (language: string, field: 'hour' | 'minute') => {
  const name = new Intl.DisplayNames(language, { type: 'dateTimeField' }).of(field) ?? field
  return name.charAt(0).toLocaleUpperCase(language) + name.slice(1)
}

/** A dialog to pick a time, starting at [time]: an hour and a minute field, like Material's TimeInput. */
export function TimePickerDialog({
  time,
  title,
  onConfirm,
  onDismiss,
}: {
  time: LocalTime
  title: string
  onConfirm: (time: LocalTime) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [hour, setHour] = useState(String(hourOf(time)).padStart(2, '0'))
  const [minute, setMinute] = useState(String(minuteOf(time)).padStart(2, '0'))
  // A field left empty or out of range keeps the time it started with
  const parsed = (text: string, max: number, fallback: number) => {
    const value = Number(text)
    return text.trim() !== '' && Number.isInteger(value) && value >= 0 && value <= max ? value : fallback
  }
  const confirm = () => onConfirm(timeOf(parsed(hour, 23, hourOf(time)), parsed(minute, 59, minuteOf(time))))
  const digits = (text: string) => text.replace(/\D/g, '').slice(0, 2)
  return (
    <Dialog
      title={title}
      onDismiss={onDismiss}
      isScrolling={false}
      buttons={
        <>
          <TextButton onClick={onDismiss}>{strings.t('reminders_time_cancel')}</TextButton>
          <TextButton onClick={confirm}>{strings.t('reminders_time_confirm')}</TextButton>
        </>
      }
    >
      <div className="time-input">
        <label>
          <input
            value={hour}
            inputMode="numeric"
            aria-label={fieldName(strings.language, 'hour')}
            onFocus={event => event.target.select()}
            onChange={event => setHour(digits(event.target.value))}
            onKeyDown={event => event.key === 'Enter' && confirm()}
          />
          <span className="body-small">{fieldName(strings.language, 'hour')}</span>
        </label>
        <span className="separator">:</span>
        <label>
          <input
            value={minute}
            inputMode="numeric"
            aria-label={fieldName(strings.language, 'minute')}
            onFocus={event => event.target.select()}
            onChange={event => setMinute(digits(event.target.value))}
            onKeyDown={event => event.key === 'Enter' && confirm()}
          />
          <span className="body-small">{fieldName(strings.language, 'minute')}</span>
        </label>
      </div>
    </Dialog>
  )
}
