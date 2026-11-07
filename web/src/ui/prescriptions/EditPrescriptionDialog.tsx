import { useState } from 'react'
import type { Prescription } from '../../data/models'
import { useStrings } from '../../i18n/i18n'
import { countOrNull, dateOrNull, isOptionalDate, positiveCountOrNull } from '../components/fieldParsing'
import { RecordEditDialog } from '../components/RecordEditDialog'
import { TextField } from '../components/TextField'

/** Edits every field of a [prescription] by hand, or deletes it. */
export function EditPrescriptionDialog({
  prescription,
  hasBoxes,
  onSave,
  onDelete,
  onDismiss,
}: {
  prescription: Prescription
  hasBoxes: boolean
  onSave: (updated: Prescription) => void
  onDelete: (keepBoxes: boolean) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [prescribedDate, setPrescribedDate] = useState(prescription.prescribedDate)
  const [medication, setMedication] = useState(prescription.medication)
  const [doctor, setDoctor] = useState(prescription.prescribedBy)
  const [pharmacy, setPharmacy] = useState(prescription.pharmacy)
  const [pillsPerBox, setPillsPerBox] = useState(String(prescription.pillsPerBox))
  const [boxesPerRepetition, setBoxesPerRepetition] = useState(String(prescription.boxesPerRepetition))
  const [repetitionsTotal, setRepetitionsTotal] = useState(String(prescription.repetitionsTotal))
  const [repetitionsBought, setRepetitionsBought] = useState(String(prescription.repetitionsBought))
  const [orderedDate, setOrderedDate] = useState(prescription.orderedDate ?? '')
  const [boughtFirstDate, setBoughtFirstDate] = useState(prescription.boughtFirstDate ?? '')
  const [boughtLastDate, setBoughtLastDate] = useState(prescription.boughtLastDate ?? '')

  const total = positiveCountOrNull(repetitionsTotal)
  const boughtCount = countOrNull(repetitionsBought)
  const bought = boughtCount !== null && total !== null && boughtCount <= total ? boughtCount : null
  const date = dateOrNull(prescribedDate)
  const pills = positiveCountOrNull(pillsPerBox)
  const boxes = positiveCountOrNull(boxesPerRepetition)
  const areOptionalDatesValid = [orderedDate, boughtFirstDate, boughtLastDate].every(isOptionalDate)
  const updated: Prescription | null =
    date !== null && pills !== null && boxes !== null && total !== null && bought !== null &&
    medication.trim() && doctor.trim() && pharmacy.trim() && areOptionalDatesValid
      ? {
          ...prescription,
          prescribedDate: date,
          medication: medication.trim(),
          prescribedBy: doctor.trim(),
          pharmacy: pharmacy.trim(),
          pillsPerBox: pills,
          boxesPerRepetition: boxes,
          repetitionsTotal: total,
          repetitionsBought: bought,
          orderedDate: dateOrNull(orderedDate),
          boughtFirstDate: dateOrNull(boughtFirstDate),
          boughtLastDate: dateOrNull(boughtLastDate),
        }
      : null

  return (
    <RecordEditDialog
      title={strings.t('edit_prescription_title')}
      canSave={updated !== null}
      deleteMessage={strings.t(hasBoxes ? 'edit_prescription_delete_message' : 'edit_delete_message')}
      onSave={() => updated && onSave(updated)}
      onDelete={() => onDelete(false)}
      onDismiss={onDismiss}
      keepChildrenLabel={hasBoxes ? strings.t('edit_delete_keep_boxes') : undefined}
      onDeleteKeepingChildren={() => onDelete(true)}
    >
      <TextField value={prescribedDate} onChange={setPrescribedDate} label={strings.t('edit_field_prescribed_date')} isError={date === null} />
      <TextField value={medication} onChange={setMedication} label={strings.t('prescriptions_field_medication')} isError={!medication.trim()} />
      <TextField value={doctor} onChange={setDoctor} label={strings.t('prescriptions_field_doctor')} isError={!doctor.trim()} />
      <TextField value={pharmacy} onChange={setPharmacy} label={strings.t('prescriptions_field_pharmacy')} isError={!pharmacy.trim()} />
      <TextField value={pillsPerBox} onChange={setPillsPerBox} label={strings.t('prescriptions_field_pills_per_box')} isNumber isError={pills === null} />
      <TextField
        value={boxesPerRepetition}
        onChange={setBoxesPerRepetition}
        label={strings.t('prescriptions_field_boxes_per_repetition')}
        isNumber
        isError={boxes === null}
      />
      <TextField value={repetitionsTotal} onChange={setRepetitionsTotal} label={strings.t('prescriptions_field_repetitions_total')} isNumber isError={total === null} />
      <TextField value={repetitionsBought} onChange={setRepetitionsBought} label={strings.t('edit_field_repetitions_bought')} isNumber isError={bought === null} />
      <TextField value={orderedDate} onChange={setOrderedDate} label={strings.t('edit_field_ordered_date')} isError={!isOptionalDate(orderedDate)} />
      <TextField value={boughtFirstDate} onChange={setBoughtFirstDate} label={strings.t('edit_field_bought_first_date')} isError={!isOptionalDate(boughtFirstDate)} />
      <TextField value={boughtLastDate} onChange={setBoughtLastDate} label={strings.t('edit_field_bought_last_date')} isError={!isOptionalDate(boughtLastDate)} />
    </RecordEditDialog>
  )
}
