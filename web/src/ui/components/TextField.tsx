import { type ReactNode, useId } from 'react'

/** Material's outlined text field, one line unless [minLines]; marked as an error when [isError]. */
export function TextField({
  value,
  onChange,
  label,
  isNumber = false,
  isError = false,
  isDisabled = false,
  type = 'text',
  minLines,
  supportingText,
  className,
  onBlur,
  onEnter,
  autoCapitalize,
  trailing,
}: {
  value: string
  onChange: (value: string) => void
  label: string
  isNumber?: boolean
  isError?: boolean
  isDisabled?: boolean
  type?: string
  minLines?: number
  supportingText?: string
  className?: string
  onBlur?: () => void
  onEnter?: () => void
  autoCapitalize?: string
  trailing?: ReactNode
}) {
  const id = useId()
  const classes = ['field', value !== '' && 'filled', isError && 'error', isDisabled && 'disabled', className]
  const common = {
    id,
    value,
    disabled: isDisabled,
    'aria-invalid': isError || undefined,
    onChange: (event: { target: { value: string } }) => onChange(event.target.value),
    onBlur,
  }
  return (
    <div className={classes.filter(Boolean).join(' ')}>
      {minLines ? (
        <textarea {...common} rows={minLines} />
      ) : (
        <input
          {...common}
          type={type}
          inputMode={isNumber ? 'numeric' : undefined}
          autoCapitalize={autoCapitalize}
          onKeyDown={event => {
            if (event.key === 'Enter' && onEnter) onEnter()
          }}
        />
      )}
      <label htmlFor={id}>{label}</label>
      {trailing && <span className="trailing">{trailing}</span>}
      {supportingText && <div className="supporting">{supportingText}</div>}
    </div>
  )
}
