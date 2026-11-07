import { useState } from 'react'
import type { YearMonth } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import { MonthSelector } from '../components/MonthSelector'

/** Asks the expiry date of each of a repetition's [boxCount] boxes, numbered from [firstBoxNumber], before buying. */
export function BuyRepetitionDialog({
  boxCount,
  firstBoxNumber,
  onSave,
  onDismiss,
}: {
  boxCount: number
  firstBoxNumber: number
  onSave: (expiryDates: YearMonth[]) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [dates, setDates] = useState<(YearMonth | null)[]>(Array(boxCount).fill(null))
  return (
    <Dialog
      title={strings.t('prescriptions_expiry_title')}
      onDismiss={onDismiss}
      buttons={
        <>
          <TextButton onClick={onDismiss}>{strings.t('prescriptions_cancel')}</TextButton>
          <TextButton
            onClick={() => onSave(dates.filter((it): it is YearMonth => it !== null))}
            disabled={dates.some(it => it === null)}
          >
            {strings.t('prescriptions_save')}
          </TextButton>
        </>
      }
    >
      {dates.map((date, index) => (
        <MonthSelector
          key={index}
          month={date}
          label={strings.t('prescriptions_expiry_field', firstBoxNumber + index)}
          onMonthChange={month => setDates(withDate(dates, index, month))}
        />
      ))}
    </Dialog>
  )
}

// Boxes bought together mostly expire together, so the first date chosen starts them all
function withDate(dates: (YearMonth | null)[], index: number, date: YearMonth | null): (YearMonth | null)[] {
  const isFirstDate = dates.every(it => it === null)
  return dates.map((current, other) => (isFirstDate || other === index ? date : current))
}
