import { useState } from 'react'
import type { Box } from '../../data/models'
import type { YearMonth } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { countOrNull, dateOrNull, isOptionalDate, positiveCountOrNull } from '../components/fieldParsing'
import { MonthSelector } from '../components/MonthSelector'
import { RecordEditDialog } from '../components/RecordEditDialog'
import { TextField } from '../components/TextField'

/** Edits the fields of a [box] by hand, or deletes it. */
export function EditBoxDialog({
  box,
  hasRefills,
  onSave,
  onDelete,
  onDismiss,
}: {
  box: Box
  hasRefills: boolean
  onSave: (updated: Box) => void
  onDelete: (keepRefills: boolean) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [boxNumber, setBoxNumber] = useState(String(box.boxNumber))
  const [boughtDate, setBoughtDate] = useState(box.boughtDate)
  const [openDate, setOpenDate] = useState(box.openDate ?? '')
  const [expiryDate, setExpiryDate] = useState<YearMonth | null>(box.expiryDate)
  const [pillsLeft, setPillsLeft] = useState(String(box.pillsLeft))
  const [pillsTotal, setPillsTotal] = useState(String(box.pillsTotal))

  const number = positiveCountOrNull(boxNumber)
  const date = dateOrNull(boughtDate)
  const total = positiveCountOrNull(pillsTotal)
  const leftCount = countOrNull(pillsLeft)
  const left = leftCount !== null && total !== null && leftCount <= total ? leftCount : null
  const updated: Box | null =
    number !== null && date !== null && total !== null && left !== null && isOptionalDate(openDate)
      ? { ...box, boxNumber: number, boughtDate: date, openDate: dateOrNull(openDate), expiryDate, pillsLeft: left, pillsTotal: total }
      : null

  return (
    <RecordEditDialog
      title={strings.t('edit_box_title')}
      canSave={updated !== null}
      deleteMessage={strings.t(hasRefills ? 'edit_box_delete_message' : 'edit_delete_message')}
      onSave={() => updated && onSave(updated)}
      onDelete={() => onDelete(false)}
      onDismiss={onDismiss}
      keepChildrenLabel={hasRefills ? strings.t('edit_delete_keep_refills') : undefined}
      onDeleteKeepingChildren={() => onDelete(true)}
    >
      <TextField value={boxNumber} onChange={setBoxNumber} label={strings.t('edit_field_box_number')} isNumber isError={number === null} />
      <TextField value={boughtDate} onChange={setBoughtDate} label={strings.t('edit_field_bought_date')} isError={date === null} />
      <TextField value={openDate} onChange={setOpenDate} label={strings.t('edit_field_open_date')} isError={!isOptionalDate(openDate)} />
      <MonthSelector month={expiryDate} label={strings.t('edit_field_expiry_date')} onMonthChange={setExpiryDate} canClear />
      <TextField value={pillsLeft} onChange={setPillsLeft} label={strings.t('edit_field_pills_left')} isNumber isError={left === null} />
      <TextField value={pillsTotal} onChange={setPillsTotal} label={strings.t('edit_field_pills_total')} isNumber isError={total === null} />
    </RecordEditDialog>
  )
}
