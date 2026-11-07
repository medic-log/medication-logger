import { useMemo, useState } from 'react'
import { type AppLanguage, Strings, StringsContext } from '../../i18n/i18n'
import { TextButton } from '../components/Buttons'
import { Dialog } from '../components/Dialog'
import { LanguageOptions } from '../components/LanguageOptions'

/** Picks the app's language, applied on Confirm. The dialog's own words preview the marked language. */
export function LanguageDialog({
  selected,
  onConfirm,
  onDismiss,
}: {
  selected: AppLanguage
  onConfirm: (language: AppLanguage) => void
  onDismiss: () => void
}) {
  // Switching redraws the app, so a tap only marks the language until it's confirmed
  const [chosen, setChosen] = useState(selected)
  const strings = useMemo(() => new Strings(chosen), [chosen])
  return (
    <StringsContext.Provider value={strings}>
      <Dialog
        title={strings.t('settings_language')}
        onDismiss={onDismiss}
        buttons={
          <>
            <TextButton onClick={onDismiss}>{strings.t('edit_cancel')}</TextButton>
            <TextButton onClick={() => onConfirm(chosen)}>{strings.t('settings_language_confirm')}</TextButton>
          </>
        }
      >
        <LanguageOptions selected={chosen} onSelect={setChosen} />
      </Dialog>
    </StringsContext.Provider>
  )
}
