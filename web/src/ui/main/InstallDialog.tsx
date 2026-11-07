import { useEffect, useState } from 'react'
import { useStrings } from '../../i18n/i18n'
import { canPromptInstall, isInstalled, isIos, onInstallPromptChange, promptInstall } from '../../update/install'
import { TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'

/**
 * Explains installing the app on this device, opened from the download page's Install button: the browser's own
 * install prompt where it has one, else the steps for Safari or for other browsers.
 */
export function InstallDialog({ onDismiss }: { onDismiss: () => void }) {
  const strings = useStrings()
  const [canPrompt, setCanPrompt] = useState(canPromptInstall)
  useEffect(() => onInstallPromptChange(() => setCanPrompt(canPromptInstall())), [])
  const installed = isInstalled()
  const steps = installed ? 'web_install_done' : isIos() ? 'web_install_ios' : canPrompt ? null : 'web_install_other'
  return (
    <Dialog
      title={strings.t('web_install_title')}
      onDismiss={onDismiss}
      buttons={
        <>
          <TextButton onClick={onDismiss}>{strings.t(canPrompt && !installed ? 'edit_cancel' : 'settings_share_close')}</TextButton>
          {canPrompt && !installed && (
            <TextButton
              onClick={async () => {
                await promptInstall()
                onDismiss()
              }}
            >
              {strings.t('web_install')}
            </TextButton>
          )}
        </>
      }
    >
      {!installed && <span>{strings.t('web_install_text')}</span>}
      {steps && <span>{strings.t(steps)}</span>}
    </Dialog>
  )
}
