import { useState } from 'react'
import type { Prescription, PrescriptionDraft } from '../../data/models'
import { useStrings } from '../../i18n/i18n'
import { TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import { positiveCountOrNull } from '../components/fieldParsing'
import { TextField } from '../components/TextField'

/** Asks for a new prescription's details, starting from those of the [previous] one. */
export function NewPrescriptionDialog({
  previous,
  onSave,
  onDismiss,
}: {
  previous: Prescription | null
  onSave: (draft: PrescriptionDraft) => void
  onDismiss: () => void
}) {
  const strings = useStrings()
  const [medication, setMedication] = useState(previous?.medication ?? '')
  const [doctor, setDoctor] = useState(previous?.prescribedBy ?? '')
  const [pharmacy, setPharmacy] = useState(previous?.pharmacy ?? '')
  const [pillsPerBox, setPillsPerBox] = useState(previous ? String(previous.pillsPerBox) : '')
  const [boxesPerRepetition, setBoxesPerRepetition] = useState(previous ? String(previous.boxesPerRepetition) : '')
  const [repetitionsTotal, setRepetitionsTotal] = useState(previous ? String(previous.repetitionsTotal) : '')
  const draft = draftOrNull(medication, doctor, pharmacy, pillsPerBox, boxesPerRepetition, repetitionsTotal)
  return (
    <Dialog
      isForm
      title={strings.t('prescriptions_new')}
      onDismiss={onDismiss}
      buttons={
        <>
          <TextButton onClick={onDismiss}>{strings.t('prescriptions_cancel')}</TextButton>
          <TextButton onClick={() => draft && onSave(draft)} disabled={!draft}>
            {strings.t('prescriptions_save')}
          </TextButton>
        </>
      }
    >
      <TextField value={medication} onChange={setMedication} label={strings.t('prescriptions_field_medication')} />
      <TextField value={doctor} onChange={setDoctor} label={strings.t('prescriptions_field_doctor')} />
      <TextField value={pharmacy} onChange={setPharmacy} label={strings.t('prescriptions_field_pharmacy')} />
      <TextField value={pillsPerBox} onChange={setPillsPerBox} label={strings.t('prescriptions_field_pills_per_box')} isNumber />
      <TextField
        value={boxesPerRepetition}
        onChange={setBoxesPerRepetition}
        label={strings.t('prescriptions_field_boxes_per_repetition')}
        isNumber
      />
      <TextField
        value={repetitionsTotal}
        onChange={setRepetitionsTotal}
        label={strings.t('prescriptions_field_repetitions_total')}
        isNumber
      />
    </Dialog>
  )
}

function draftOrNull(
  medication: string,
  doctor: string,
  pharmacy: string,
  pillsPerBox: string,
  boxesPerRepetition: string,
  repetitionsTotal: string,
): PrescriptionDraft | null {
  const pills = positiveCountOrNull(pillsPerBox)
  const boxes = positiveCountOrNull(boxesPerRepetition)
  const repetitions = positiveCountOrNull(repetitionsTotal)
  if (pills === null || boxes === null || repetitions === null) return null
  if (!medication.trim() || !doctor.trim() || !pharmacy.trim()) return null
  return {
    medication: medication.trim(),
    prescribedBy: doctor.trim(),
    pharmacy: pharmacy.trim(),
    pillsPerBox: pills,
    boxesPerRepetition: boxes,
    repetitionsTotal: repetitions,
  }
}
