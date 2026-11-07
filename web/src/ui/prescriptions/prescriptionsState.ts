import type { MedicationData } from '../../data/MedicationData'
import { type Box, type Prescription, type PrescriptionStatus, isPrescriptionActive } from '../../data/models'
import type { ListFilter } from '../components/ListFilter'

/** A prescription with the boxes bought with it. */
export interface PrescriptionItem {
  prescription: Prescription
  boxes: Box[]
}

/** The prescriptions to show, and the one that a new prescription starts from. */
export interface PrescriptionsUiState {
  filter: ListFilter
  items: PrescriptionItem[]
  previousPrescription: Prescription | null
}

const SORT_RANK: Record<PrescriptionStatus, number> = { ORDERED: 0, IN_PROGRESS: 1, NOT_STARTED: 2, FINISHED: 3 }

/** The state of the prescriptions tab, as PrescriptionsViewModel works it out. */
export function prescriptionsUiState(data: MedicationData, filter: ListFilter): PrescriptionsUiState {
  return {
    filter,
    items: data.prescriptions
      .filter(it => filter === 'ALL' || isPrescriptionActive(it))
      // Newest first within a status, as a new prescription is the one most likely to need attention
      .sort(
        (a, b) =>
          SORT_RANK[a.status] - SORT_RANK[b.status] ||
          b.prescribedDate.localeCompare(a.prescribedDate) ||
          b.id - a.id,
      )
      .map(prescription => ({ prescription, boxes: data.boxes.filter(it => it.prescriptionId === prescription.id) })),
    previousPrescription: data.prescriptions.at(-1) ?? null,
  }
}
