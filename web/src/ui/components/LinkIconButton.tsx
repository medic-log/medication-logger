import { useStrings } from '../../i18n/i18n'
import { CardButton } from './Buttons'
import { Icon } from './Icon'

/**
 * An icon that opens a linked record, with a short [label] when the icon alone doesn't say which one.
 * Without [onClick], the record no longer exists: the button is disabled and its icon crossed out.
 */
export function LinkIconButton({
  icon,
  description,
  onClick,
  label,
}: {
  icon: string
  description: string
  onClick: (() => void) | null
  label?: string
}) {
  const strings = useStrings()
  return (
    <CardButton
      onClick={onClick ?? undefined}
      disabled={onClick === null}
      isSmallShape
      isIconOnly={label === undefined}
      aria-label={label ? `${strings.t(description)} ${label}` : strings.t(description)}
      title={strings.t(description)}
    >
      <span className={onClick === null ? 'crossed' : undefined} style={{ display: 'inline-flex' }}>
        <Icon name={icon} size={label === undefined ? 16 : 12} />
      </span>
      {label !== undefined && <span style={{ marginLeft: -4 }}>{label}</span>}
    </CardButton>
  )
}
