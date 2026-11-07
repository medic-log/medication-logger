import { type MedicationData, pendingRefillAmount } from '../../data/MedicationData'
import { type Box, type BoxStatus, type Refill, boxStatus, isBoxActive } from '../../data/models'
import type { LocalDateTime } from '../../data/time'
import type { ListFilter } from '../components/ListFilter'

/** A box with its refills, newest first. */
export interface BoxItem {
  box: Box
  refills: Refill[]
}

/** The boxes to show, and how many pills the weekly lot still needs. */
export interface BoxesUiState {
  filter: ListFilter
  items: BoxItem[]
  pendingRefillAmount: number
}

const SORT_RANK: Record<BoxStatus, number> = { IN_PROGRESS: 0, EXTRA: 1, NOT_STARTED: 2, FINISHED: 3 }

/** The state of the boxes tab, as BoxesViewModel works it out. */
export function boxesUiState(data: MedicationData, filter: ListFilter, now: LocalDateTime): BoxesUiState {
  return {
    filter,
    items: data.boxes
      .filter(box => (filter === 'ACTIVE' ? isBoxActive(box) : filter === 'EXTRA' ? box.isExtra : true))
      // Oldest first within a status, as that's the order boxes get used in
      .sort(
        (a, b) =>
          SORT_RANK[boxStatus(a)] - SORT_RANK[boxStatus(b)] ||
          a.boughtDate.localeCompare(b.boughtDate) ||
          a.boxNumber - b.boxNumber ||
          a.id - b.id,
      )
      .map(box => ({ box, refills: data.refills.filter(it => it.boxId === box.id).reverse() })),
    pendingRefillAmount: pendingRefillAmount(data, now),
  }
}
