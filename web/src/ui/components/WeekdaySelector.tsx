import { useCallback, useRef, useState } from 'react'
import { DAYS_OF_WEEK, type DayOfWeek } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { OutlinedButton } from './Buttons'
import { Menu, MenuItem } from './Menu'

/** A button showing [day] that opens a menu of weekdays. */
export function WeekdaySelector({ day, onDayChange }: { day: DayOfWeek; onDayChange: (day: DayOfWeek) => void }) {
  const strings = useStrings()
  const button = useRef<HTMLButtonElement>(null)
  const [isExpanded, setExpanded] = useState(false)
  const close = useCallback(() => setExpanded(false), [])
  return (
    <>
      <OutlinedButton ref={button} onClick={() => setExpanded(true)} aria-haspopup="menu" aria-expanded={isExpanded}>
        {strings.dayName(day)}
      </OutlinedButton>
      {isExpanded && button.current && (
        <Menu anchor={button.current} onDismiss={close}>
          {DAYS_OF_WEEK.map(option => (
            <MenuItem
              key={option}
              onClick={() => {
                onDayChange(option)
                setExpanded(false)
              }}
            >
              {strings.dayName(option)}
            </MenuItem>
          ))}
        </Menu>
      )}
    </>
  )
}
