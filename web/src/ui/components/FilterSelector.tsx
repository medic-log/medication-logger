import { useStrings } from '../../i18n/i18n'
import type { ListFilter } from './ListFilter'
import { SegmentedButtons } from './SegmentedButtons'

const LABELS: Record<ListFilter, string> = { ACTIVE: 'filter_active', ALL: 'filter_all', EXTRA: 'filter_extra' }

/** Selects one of the [options]. */
export function FilterSelector({
  filter,
  options,
  onFilterChange,
}: {
  filter: ListFilter
  options: ListFilter[]
  onFilterChange: (filter: ListFilter) => void
}) {
  const strings = useStrings()
  return (
    <SegmentedButtons
      options={options.map(value => ({ value, label: strings.t(LABELS[value]) }))}
      selected={filter}
      onSelect={onFilterChange}
    />
  )
}
