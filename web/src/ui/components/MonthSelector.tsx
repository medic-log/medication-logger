import { useState } from 'react'
import { type YearMonth, localDateTimeOf, monthOf, toLocalDate, yearMonthOf, yearOf } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { FilledButton, IconButton, OutlinedButton, TextButton } from './Buttons'
import { Dialog } from './Dialog'
import { Icon } from './Icon'

/** A button showing [month] that opens a picker for it; with [canClear], the month can also be removed. */
export function MonthSelector({
  month,
  label,
  onMonthChange,
  canClear = false,
}: {
  month: YearMonth | null
  label: string
  onMonthChange: (month: YearMonth | null) => void
  canClear?: boolean
}) {
  const strings = useStrings()
  const [isDialogOpen, setDialogOpen] = useState(false)
  return (
    <>
      <OutlinedButton onClick={() => setDialogOpen(true)} style={{ width: '100%' }}>
        {month !== null ? strings.t('month_selector_value', label, month) : strings.t('month_selector_empty', label)}
      </OutlinedButton>
      {isDialogOpen && (
        <MonthPickerDialog
          month={month}
          title={label}
          canClear={canClear && month !== null}
          onMonthChange={it => {
            onMonthChange(it)
            setDialogOpen(false)
          }}
          onDismiss={() => setDialogOpen(false)}
        />
      )}
    </>
  )
}

// Material 3's date picker always asks for a day, which an expiry date doesn't have
function MonthPickerDialog({
  month,
  title,
  canClear,
  onMonthChange,
  onDismiss,
}: {
  month: YearMonth | null
  title: string
  canClear: boolean
  onMonthChange: (month: YearMonth | null) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [year, setYear] = useState(yearOf(month ?? toLocalDate(localDateTimeOf(new Date()))))
  return (
    <Dialog
      title={title}
      onDismiss={onDismiss}
      buttons={
        <>
          {canClear && <TextButton onClick={() => onMonthChange(null)}>{strings.t('month_selector_clear')}</TextButton>}
          <TextButton onClick={onDismiss}>{strings.t('edit_cancel')}</TextButton>
        </>
      }
    >
      <div className="month-year">
        <IconButton onClick={() => setYear(year - 1)} aria-label={strings.t('month_selector_previous_year')}>
          <Icon name="ic_remove" />
        </IconButton>
        <span className="title-medium">{year}</span>
        <IconButton onClick={() => setYear(year + 1)} aria-label={strings.t('month_selector_next_year')}>
          <Icon name="ic_add" />
        </IconButton>
      </div>
      <div className="month-grid">
        {Array.from({ length: 12 }, (_, index) => {
          const candidate = yearMonthOf(year, index + 1)
          const name = strings.monthName(monthOf(candidate))
          const Button = candidate === month ? FilledButton : TextButton
          return (
            <Button key={candidate} onClick={() => onMonthChange(candidate)}>
              {name}
            </Button>
          )
        })}
      </div>
    </Dialog>
  )
}
