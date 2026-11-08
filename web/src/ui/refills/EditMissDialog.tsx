import { useState } from 'react'
import type { MedicationMiss, MissKind } from '../../data/models'
import type { LocalTime } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { dateOrNull } from '../components/fieldParsing'
import { RecordEditDialog } from '../components/RecordEditDialog'
import { SegmentedButtons } from '../components/SegmentedButtons'
import { TextField } from '../components/TextField'
import { TimeSelector } from '../components/TimeSelector'

/** Edits a missed or delayed dose by hand, or deletes it; [defaultTime] is the delay's time if it has none yet. */
export function EditMissDialog({
  miss,
  defaultTime,
  onSave,
  onDelete,
  onDismiss,
}: {
  miss: MedicationMiss
  defaultTime: LocalTime
  onSave: (updated: MedicationMiss) => void
  onDelete: () => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [date, setDate] = useState(miss.date)
  const [kind, setKind] = useState<MissKind>(miss.kind)
  const [time, setTime] = useState<LocalTime>(miss.time ?? defaultTime)
  const parsedDate = dateOrNull(date)
  return (
    <RecordEditDialog
      title={strings.t('edit_miss_title')}
      canSave={parsedDate !== null}
      deleteMessage={strings.t('edit_delete_message')}
      onSave={() => parsedDate && onSave({ ...miss, date: parsedDate, kind, time: kind === 'DELAYED' ? time : null })}
      onDelete={onDelete}
      onDismiss={onDismiss}
    >
      <TextField value={date} onChange={setDate} label={strings.t('edit_field_date')} isError={parsedDate === null} />
      <SegmentedButtons
        options={[
          { value: 'MISSED', label: strings.t('refills_missed') },
          { value: 'DELAYED', label: strings.t('refills_delayed') },
        ]}
        selected={kind}
        onSelect={setKind}
      />
      {kind === 'DELAYED' && (
        <div>
          <TimeSelector time={time} dialogTitle={strings.t('refills_delayed_time_dialog_title')} onTimeChange={setTime} />
        </div>
      )}
    </RecordEditDialog>
  )
}
