import { useState } from 'react'
import { DATE_STYLES, type DateStyle } from '../../data/models'
import type { LocalDate } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'

/** Picks how dates are shown, applied on Confirm; the language option shows [today] in it, as DateFormatDialog. */
export function DateFormatDialog({
  selected,
  today,
  onConfirm,
  onDismiss,
}: {
  selected: DateStyle
  today: LocalDate
  onConfirm: (style: DateStyle) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [chosen, setChosen] = useState(selected)
  return (
    <Dialog
      title={strings.t('settings_date_format_title')}
      onDismiss={onDismiss}
      buttons={
        <>
          <TextButton onClick={onDismiss}>{strings.t('edit_cancel')}</TextButton>
          <TextButton onClick={() => onConfirm(chosen)}>{strings.t('settings_language_confirm')}</TextButton>
        </>
      }
    >
      <div role="radiogroup">
        {DATE_STYLES.map(style => {
          const label =
            style === 'LANGUAGE'
              ? strings.t('settings_date_format_language', strings.date(today, style))
              : strings.t(`settings_date_format_${style.toLowerCase()}`)
          return (
            <button
              key={style}
              type="button"
              role="radio"
              aria-checked={style === chosen}
              className="radio-row"
              onClick={() => setChosen(style)}
            >
              <span className="radio" />
              <span className="label body-large">{label}</span>
            </button>
          )
        })}
      </div>
    </Dialog>
  )
}
