import { type PointerEvent, type ReactNode, useRef, useState } from 'react'
import { useStrings } from '../../i18n/i18n'
import { TextButton } from './Buttons'
import { Dialog } from './Dialog'
import type { StatusTone } from './StatusBadge'

// Longer than the system's long press, so that a card isn't opened for editing by accident
const EDIT_HOLD_MILLIS = 1000
// How far a finger can move before a hold counts as a scroll
const HOLD_SLOP_PX = 10

/**
 * A card for one record; highlighted when the user arrived by following a link to it.
 * With [tone], the card and everything in it take on the colors of the record's status.
 * With [onEdit], holding the card down for a second asks whether to edit its record, and calls [onEdit] if so.
 * A right-click or the context menu key asks too, for mice and keyboards.
 */
export function RecordCard({
  children,
  isHighlighted = false,
  tone,
  onEdit,
  className,
  attributes,
}: {
  children: ReactNode
  isHighlighted?: boolean
  tone?: StatusTone
  onEdit?: () => void
  className?: string
  attributes?: Record<string, string>
}) {
  const strings = useStrings()
  const [isConfirmingEdit, setConfirmingEdit] = useState(false)
  const [isHolding, setHolding] = useState(false)
  const hold = useRef<{ timer: number; x: number; y: number } | null>(null)

  const cancelHold = () => {
    if (hold.current) window.clearTimeout(hold.current.timer)
    hold.current = null
    setHolding(false)
  }
  const askToEdit = () => {
    cancelHold()
    navigator.vibrate?.(10)
    setConfirmingEdit(true)
  }
  const onPointerDown = (event: PointerEvent) => {
    // Buttons and fields on the card handle their own presses
    if (!onEdit || event.button !== 0 || (event.target as Element).closest('button, input, textarea, a, select')) return
    cancelHold()
    hold.current = { timer: window.setTimeout(askToEdit, EDIT_HOLD_MILLIS), x: event.clientX, y: event.clientY }
    setHolding(true)
  }
  const onPointerMove = (event: PointerEvent) => {
    const start = hold.current
    if (start && Math.hypot(event.clientX - start.x, event.clientY - start.y) > HOLD_SLOP_PX) cancelHold()
  }

  const classes = ['card', tone && `status tone-${tone}`, isHighlighted && 'highlighted', isHolding && 'holding', className]
  return (
    <>
      <div
        {...attributes}
        className={classes.filter(Boolean).join(' ')}
        aria-current={isHighlighted || undefined}
        tabIndex={onEdit ? 0 : undefined}
        aria-description={onEdit ? strings.t('tutorial_edit_text') : undefined}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={cancelHold}
        onPointerCancel={cancelHold}
        onPointerLeave={cancelHold}
        onContextMenu={event => {
          if (!onEdit) return
          event.preventDefault()
          askToEdit()
        }}
      >
        {children}
      </div>
      {isConfirmingEdit && onEdit && (
        <Dialog
          title={strings.t('edit_confirm_title')}
          onDismiss={() => setConfirmingEdit(false)}
          buttons={
            <>
              <TextButton onClick={() => setConfirmingEdit(false)}>{strings.t('edit_cancel')}</TextButton>
              <TextButton
                onClick={() => {
                  setConfirmingEdit(false)
                  onEdit()
                }}
              >
                {strings.t('edit_action')}
              </TextButton>
            </>
          }
        />
      )}
    </>
  )
}
