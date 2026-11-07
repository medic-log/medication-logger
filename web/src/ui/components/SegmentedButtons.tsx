import { Icon } from './Icon'

/** Material's single-choice segmented buttons, the selected one checked. */
export function SegmentedButtons<T extends string>({
  options,
  selected,
  onSelect,
  label,
}: {
  options: { value: T; label: string }[]
  selected: T
  onSelect: (value: T) => void
  label?: string
}) {
  return (
    <div className="segmented" role="radiogroup" aria-label={label}>
      {options.map(option => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={option.value === selected}
          className="segment"
          onClick={() => onSelect(option.value)}
        >
          {option.value === selected && <Icon name="check" size={18} />}
          {option.label}
        </button>
      ))}
    </div>
  )
}
