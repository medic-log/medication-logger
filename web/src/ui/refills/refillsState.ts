import { type MedicationData, PILLS_PER_WEEK, lotPills } from '../../data/MedicationData'
import type { Box, MedicationMiss, Refill } from '../../data/models'
import type { LocalDate, LocalDateTime } from '../../data/time'

/**
 * A refill, the box it came from, and the most it could be edited up to.
 * leftRange is what its pills left can be edited to while its box stays valid; null without a box.
 */
export interface RefillEntry {
  type: 'refill'
  key: string
  date: LocalDate
  refill: Refill
  box: Box | null
  maxAmount: number
  leftRange: [number, number] | null
}

/** A missed or delayed dose. */
export interface MissEntry {
  type: 'miss'
  key: string
  date: LocalDate
  miss: MedicationMiss
}

/**
 * Cards pasted together, each newest first: refills that belong together, either partial ones made one after
 * another on one day that add up to a full week or a single one, and the missed or delayed doses taken from them.
 */
export interface RefillGroupEntry {
  type: 'group'
  key: string
  date: LocalDate
  refills: RefillEntry[]
  doses: MissEntry[]
  members: (RefillEntry | MissEntry)[]
}

/** One line of the medication log. */
export type RefillLogEntry = RefillEntry | MissEntry | RefillGroupEntry

/** The medication log, newest first, and the pills in the weekly lot. */
export interface RefillsUiState {
  entries: RefillLogEntry[]
  lotPills: number
}

const maxDate = (dates: LocalDate[]) => dates.reduce((a, b) => (b > a ? b : a))

export function groupEntry(refills: RefillEntry[], doses: MissEntry[] = []): RefillGroupEntry {
  const members = [...doses, ...refills]
  return {
    type: 'group',
    key: `group-${refills.at(-1)!.refill.id}`,
    date: maxDate(members.map(it => it.date)),
    refills,
    doses,
    members,
  }
}

function missEntry(miss: MedicationMiss): MissEntry {
  return { type: 'miss', key: `miss-${miss.id}`, date: miss.date, miss }
}

/** The state of the refills tab, as RefillsViewModel works it out. */
export function refillsUiState(data: MedicationData, now: LocalDateTime): RefillsUiState {
  const boxes = new Map(data.boxes.map(it => [it.id, it]))
  const refills: RefillEntry[] = data.refills.map(refill => {
    const box = refill.boxId === null ? undefined : boxes.get(refill.boxId)
    return {
      type: 'refill',
      key: `refill-${refill.id}`,
      date: refill.date,
      refill,
      box: box ?? null,
      maxAmount: refill.amount + (box?.pillsLeft ?? 0),
      leftRange: box
        ? [Math.max(refill.amountRemaining - box.pillsLeft, 0), refill.amountRemaining + box.pillsTotal - box.pillsLeft]
        : null,
    }
  })
  const refillEntries = groupingFullWeeks(refills)
  // A dose is its day's pill, which comes before that day's refills as in lotPills, so it belongs to the last
  // refill made before its day; until there's one, it stands alone
  const dosesByRefill = new Map<number, MissEntry[]>()
  for (const dose of data.misses.map(missEntry)) {
    let index = -1
    refillEntries.forEach((entry, i) => {
      if (entry.date < dose.date) index = i
    })
    dosesByRefill.set(index, [...(dosesByRefill.get(index) ?? []), dose])
  }
  const stacked = refillEntries.map((entry, index): RefillLogEntry => {
    const doses = [...(dosesByRefill.get(index) ?? [])].reverse()
    if (doses.length === 0) return entry
    return entry.type === 'group' ? groupEntry(entry.refills, doses) : groupEntry([entry], doses)
  })
  const alone = [...(dosesByRefill.get(-1) ?? [])].sort((a, b) => b.miss.id - a.miss.id)
  return {
    // A stable sort, newest first, as Kotlin's sortedByDescending
    entries: [...alone, ...stacked.reverse()].sort((a, b) => b.date.localeCompare(a.date)),
    lotPills: lotPills(data, now),
  }
}

function groupingFullWeeks(refills: RefillEntry[]): (RefillEntry | RefillGroupEntry)[] {
  const entries: (RefillEntry | RefillGroupEntry)[] = []
  let start = 0
  while (start < refills.length) {
    const week = fullWeekFrom(refills, start)
    if (week === null) {
      entries.push(refills[start])
      start++
    } else {
      entries.push(groupEntry([...week].reverse()))
      start += week.length
    }
  }
  return entries
}

function fullWeekFrom(refills: RefillEntry[], start: number): RefillEntry[] | null {
  let pills = 0
  for (let end = start; end < refills.length; end++) {
    const refill = refills[end].refill
    if (!refill.isPartial || refill.date !== refills[start].refill.date) return null
    pills += refill.amount
    if (pills === PILLS_PER_WEEK) return end > start ? refills.slice(start, end + 1) : null
    if (pills > PILLS_PER_WEEK) return null
  }
  return null
}
