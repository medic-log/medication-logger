import { useMemo, useState } from 'react'
import type { ReminderSettings } from '../../data/models'
import { type AppLanguage, Strings, StringsContext, useStrings } from '../../i18n/i18n'
import { FilledButton, TextButton } from '../components/Buttons'
import { LanguageOptions } from '../components/LanguageOptions'
import { TimeSelector } from '../components/TimeSelector'
import { tutorialTarget } from '../components/TutorialTarget'
import { WeekdaySelector } from '../components/WeekdaySelector'
import { TUTORIAL_STEPS, type TutorialStep } from './TutorialStep'

/** A tutorial step, docked above the tab bar, with the settings it asks for and the way to the next one. */
export function TutorialCard({
  step,
  settings,
  onSettingsChange,
  language,
  onLanguageChange,
  onBack,
  onNext,
  onSkip,
}: {
  step: TutorialStep
  settings: ReminderSettings
  onSettingsChange: (settings: ReminderSettings) => void
  language: AppLanguage
  onLanguageChange: (language: AppLanguage) => void
  onBack: () => void
  onNext: () => void
  onSkip: () => void
}) {
  // Switching redraws the app, so a tap only marks the language, previewed on this card, until Next
  const [chosenLanguage, setChosenLanguage] = useState(language)
  const appStrings = useStrings()
  const previewStrings = useMemo(() => new Strings(chosenLanguage), [chosenLanguage])
  const strings = step.step === 'LANGUAGE' ? previewStrings : appStrings
  const index = TUTORIAL_STEPS.indexOf(step)
  const hasNext = index < TUTORIAL_STEPS.length - 1
  return (
    <StringsContext.Provider value={strings}>
      <section className="tutorial-card" aria-live="polite" {...tutorialTarget('TUTORIAL_CARD')}>
        <span className="label-medium">{strings.t('tutorial_progress', index + 1, TUTORIAL_STEPS.length)}</span>
        <h2 className="title-medium" style={{ marginBlock: 0 }}>
          {strings.t(step.title)}
        </h2>
        <span className="body-medium">{strings.t(step.text)}</span>
        {step.step === 'LANGUAGE' && <LanguageOptions selected={chosenLanguage} onSelect={setChosenLanguage} />}
        {step.step === 'PILL_TIME' && (
          <div>
            <TimeSelector
              time={settings.pillTime}
              dialogTitle={strings.t('reminders_pill_time_dialog_title')}
              onTimeChange={it => onSettingsChange({ ...settings, pillTime: it })}
            />
          </div>
        )}
        {step.step === 'REFILL_TIME' && (
          <div className="flow">
            <WeekdaySelector day={settings.refillDay} onDayChange={it => onSettingsChange({ ...settings, refillDay: it })} />
            <TimeSelector
              time={settings.refillTime}
              dialogTitle={strings.t('reminders_time_dialog_title')}
              onTimeChange={it => onSettingsChange({ ...settings, refillTime: it })}
            />
          </div>
        )}
        <div className="row center">
          {hasNext && <TextButton onClick={onSkip}>{strings.t('tutorial_skip')}</TextButton>}
          <span className="grow" />
          {index > 0 && <TextButton onClick={onBack}>{strings.t('tutorial_back')}</TextButton>}
          <FilledButton
            onClick={() => {
              onNext()
              if (step.step === 'LANGUAGE' && chosenLanguage !== language) onLanguageChange(chosenLanguage)
            }}
          >
            {strings.t(hasNext ? 'tutorial_next' : 'tutorial_done')}
          </FilledButton>
        </div>
      </section>
    </StringsContext.Provider>
  )
}
