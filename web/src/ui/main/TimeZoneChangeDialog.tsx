import type { UtcOffset } from '../../data/time'
import { useStrings } from '../../i18n/i18n'
import { TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import type { TimeZoneChange } from './MainViewModel'

/** [offset] as `UTC+01:00`. */
const label = (offset: UtcOffset) => `UTC${offset === 'Z' ? '+00:00' : offset}`

/** Tells that the device's time zone changed, and the times the pill and refill reminders moved to. */
export function TimeZoneChangeDialog({ change, onDismiss }: { change: TimeZoneChange; onDismiss: () => void }) {
  const strings = useStrings()
  const { settings } = change
  return (
    <Dialog
      title={strings.t('time_zone_changed_title')}
      onDismiss={onDismiss}
      buttons={<TextButton onClick={onDismiss}>{strings.t('reminders_time_confirm')}</TextButton>}
    >
      <span>
        {strings.t(
          'time_zone_changed_text',
          label(change.from),
          settings.utcOffset ? `${change.zone} (${label(settings.utcOffset)})` : '',
        )}
      </span>
      {settings.isPillEnabled && <span>{strings.t('time_zone_changed_pill', settings.pillTime)}</span>}
      {settings.isRefillEnabled && (
        <span>{strings.t('time_zone_changed_refill', strings.dayName(settings.refillDay), settings.refillTime)}</span>
      )}
      <span>{strings.t('time_zone_changed_hint')}</span>
    </Dialog>
  )
}
