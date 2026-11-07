import { useState } from 'react'
import type { Refill } from '../../data/models'
import { useStrings } from '../../i18n/i18n'
import { countOrNull, dateOrNull, positiveCountOrNull } from '../components/fieldParsing'
import { RecordEditDialog } from '../components/RecordEditDialog'
import { Switch } from '../components/Switch'
import { TextField } from '../components/TextField'

/**
 * Edits a [refill] by hand, up to [maxAmount] pills and with pills left in [leftRange], or deletes it.
 * Changing the amount also changes the pills left, which can then be corrected.
 */
export function EditRefillDialog({
  refill,
  maxAmount,
  leftRange,
  onSave,
  onDelete,
  onDismiss,
}: {
  refill: Refill
  maxAmount: number
  leftRange: [number, number] | null
  onSave: (updated: Refill) => void
  onDelete: () => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [date, setDate] = useState(refill.date)
  const [amount, setAmount] = useState(String(refill.amount))
  const [left, setLeft] = useState(String(refill.amountRemaining))
  const [isPartial, setPartial] = useState(refill.isPartial)

  const parsedDate = dateOrNull(date)
  const amountCount = positiveCountOrNull(amount)
  const parsedAmount = amountCount !== null && amountCount <= maxAmount ? amountCount : null
  const leftCount = countOrNull(left)
  const parsedLeft =
    leftCount !== null && (leftRange === null || (leftCount >= leftRange[0] && leftCount <= leftRange[1])) ? leftCount : null
  const updated =
    parsedDate !== null && parsedAmount !== null && parsedLeft !== null
      ? { ...refill, date: parsedDate, amount: parsedAmount, amountRemaining: parsedLeft, isPartial }
      : null

  return (
    <RecordEditDialog
      title={strings.t('edit_refill_title')}
      canSave={updated !== null}
      deleteMessage={strings.t(refill.boxId !== null ? 'edit_refill_delete_message' : 'edit_delete_message')}
      onSave={() => updated && onSave(updated)}
      onDelete={onDelete}
      onDismiss={onDismiss}
    >
      <TextField value={date} onChange={setDate} label={strings.t('edit_field_date')} isError={parsedDate === null} />
      <TextField
        value={amount}
        onChange={newAmount => {
          setAmount(newAmount)
          const pills = positiveCountOrNull(newAmount)
          if (pills !== null) setLeft(String(Math.max(refill.amountRemaining + refill.amount - pills, 0)))
        }}
        label={strings.t('edit_field_refill_amount', maxAmount)}
        isNumber
        isError={parsedAmount === null}
      />
      <TextField
        value={left}
        onChange={setLeft}
        label={
          leftRange ? strings.t('edit_field_refill_left_range', leftRange[0], leftRange[1]) : strings.t('edit_field_refill_left')
        }
        isNumber
        isError={parsedLeft === null}
      />
      <div className="row center">
        <span className="body-large grow" style={{ color: 'var(--on-surface)' }}>
          {strings.t('edit_field_partial')}
        </span>
        <Switch checked={isPartial} onChange={setPartial} label={strings.t('edit_field_partial')} />
      </div>
    </RecordEditDialog>
  )
}
