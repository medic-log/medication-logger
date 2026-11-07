import { useState } from 'react'
import { useStrings } from '../../i18n/i18n'
import { IconButton, OutlinedButton, TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import { Icon } from '../components/Icon'
import { QrCode } from '../components/QrCode'
import { useToast } from '../components/Toast'

const QR_SIZE = 200

/**
 * The download site's link as a QR code to scan, with a button to send it through another app and one
 * to open the site, after asking.
 */
export function ShareDialog({ url, onDismiss }: { url: string; onDismiss: () => void }) {
  const strings = useStrings()
  const showToast = useToast()
  const [isOpenSiteConfirmShown, setOpenSiteConfirmShown] = useState(false)
  const share = async () => {
    const text = strings.t('settings_share_text', url)
    if (navigator.share) {
      // A cancelled share sheet rejects, which needs no message
      await navigator.share({ text }).catch(() => undefined)
    } else {
      // Without a secure context (plain http) there's no clipboard, and a denied permission rejects
      const isCopied = await (navigator.clipboard?.writeText(text).then(() => true) ?? Promise.resolve(false))
        .catch(() => false)
      showToast(strings.t(isCopied ? 'web_share_copied' : 'web_share_copy_failed'))
    }
  }
  return (
    <>
      <Dialog
        title={
          <div className="dialog-title-row">
            <h2 className="dialog-title">{strings.t('settings_share_title')}</h2>
            <IconButton onClick={() => setOpenSiteConfirmShown(true)} aria-label={strings.t('feedback_open_site')}>
              <Icon name="ic_open_in_new" />
            </IconButton>
          </div>
        }
        onDismiss={onDismiss}
        buttons={<TextButton onClick={onDismiss}>{strings.t('settings_share_close')}</TextButton>}
      >
        <div className="column share-body">
          <QrCode content={url} size={QR_SIZE} label={strings.t('settings_share_qr_description')} />
          <OutlinedButton onClick={() => void share()}>
            <Icon name="ic_share" />
            {strings.t('settings_share')}
          </OutlinedButton>
        </div>
      </Dialog>
      {isOpenSiteConfirmShown && (
        <Dialog
          title={strings.t('settings_share_open_site_title')}
          onDismiss={() => setOpenSiteConfirmShown(false)}
          buttons={
            <>
              <TextButton onClick={() => setOpenSiteConfirmShown(false)}>{strings.t('edit_cancel')}</TextButton>
              <TextButton
                onClick={() => {
                  setOpenSiteConfirmShown(false)
                  window.open(`${url}?lang=${strings.language}`, '_blank', 'noopener')
                }}
              >
                {strings.t('feedback_open')}
              </TextButton>
            </>
          }
        />
      )}
    </>
  )
}
