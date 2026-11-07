import { useState } from 'react'
import resources from 'virtual:android-resources'
import { useStrings } from '../../i18n/i18n'
import { useServices } from '../AppServices'
import type { AvailableUpdate } from '../main/MainViewModel'
import { TextButton } from './Buttons'
import { Dialog } from './Dialog'

/**
 * Offers [update], a newer version, and switches to it if the user chooses to update; [onDismiss] runs either way,
 * when the offer closes. While the new version loads, a dialog says so.
 */
export function UpdatePrompt({ update, onDismiss }: { update: AvailableUpdate | null; onDismiss: () => void }) {
  const strings = useStrings()
  const { updates } = useServices()
  const [loadingVersion, setLoadingVersion] = useState<string | null>(null)
  return (
    <>
      {loadingVersion && (
        <Dialog
          title={strings.t('settings_update_downloading', loadingVersion)}
          onDismiss={() => {}}
          isDismissible={false}
        >
          <div className="progress" role="progressbar" aria-label={strings.t('settings_update_downloading', loadingVersion)} />
        </Dialog>
      )}
      {update && (
        <Dialog
          title={strings.t('settings_update_confirm_title')}
          onDismiss={onDismiss}
          buttons={
            <>
              <TextButton onClick={onDismiss}>{strings.t('settings_update_later')}</TextButton>
              <TextButton
                onClick={() => {
                  onDismiss()
                  setLoadingVersion(update.versionName)
                  void updates.apply()
                }}
              >
                {strings.t('settings_update')}
              </TextButton>
            </>
          }
        >
          {strings.t('web_update_confirm_text', update.versionName, resources.versionName)}
        </Dialog>
      )}
    </>
  )
}
