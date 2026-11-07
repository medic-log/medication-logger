import { useCallback, useState } from 'react'
import { useStrings } from '../../i18n/i18n'
import { useMainState, useServices } from '../AppServices'
import { BoxesScreen } from '../boxes/BoxesScreen'
import { Icon } from '../components/Icon'
import type { ListFilter } from '../components/ListFilter'
import { tutorialTarget } from '../components/TutorialTarget'
import { UpdatePrompt } from '../components/UpdatePrompt'
import { PrescriptionsScreen } from '../prescriptions/PrescriptionsScreen'
import { RefillsScreen } from '../refills/RefillsScreen'
import { SettingsScreen } from '../reminders/SettingsScreen'
import { APP_TABS, type AppTab } from './AppTab'
import { InstallDialog } from './InstallDialog'
import { TutorialCard } from './TutorialCard'
import { TutorialOverlay } from './TutorialOverlay'

/**
 * The app's frame: the tab bar, with the selected tab's content above it and the tutorial card between the two,
 * and the tutorial's overlay over it all. A tutorial step that points at nothing blurs the app behind it, and
 * during the tutorial only its card responds. Boxes stands out as the main tab.
 */
export function MainScreen({
  isInstallShown,
  onInstallDismiss,
  onTutorialFinished,
}: {
  isInstallShown: boolean
  onInstallDismiss: () => void
  onTutorialFinished: () => void
}) {
  const strings = useStrings()
  const { viewModel, language, setLanguage } = useServices()
  const state = useMainState()
  // Kept across tabs, as the Android app's tab ViewModels are
  const [prescriptionsFilter, setPrescriptionsFilter] = useState<ListFilter>('ACTIVE')
  const [boxesFilter, setBoxesFilter] = useState<ListFilter>('ACTIVE')
  const step = state.tutorialStep
  const isBlurred = step !== null && step.target === null
  const background = { inert: step !== null, className: isBlurred ? 'blurred' : undefined }
  const finishTutorial = useCallback(() => {
    viewModel.finishTutorial()
    onTutorialFinished()
  }, [viewModel, onTutorialFinished])
  const tabTarget = (tab: AppTab) => APP_TABS.find(it => it.tab === tab)!.target

  return (
    <>
      <div className="app">
        {state.isSaveFailed && <div className="save-failed body-medium" role="alert">{strings.t('main_save_failed')}</div>}
        <main className={`app-content ${background.className ?? ''}`} inert={background.inert}>
          <TabContent
            tab={state.selectedTab}
            focusedPrescriptionId={state.focusedPrescriptionId}
            focusedBoxId={state.focusedBoxId}
            settingsScroll={state.settingsScroll}
            prescriptionsFilter={prescriptionsFilter}
            onPrescriptionsFilterChange={setPrescriptionsFilter}
            boxesFilter={boxesFilter}
            onBoxesFilterChange={setBoxesFilter}
          />
        </main>
        {step && (
          <TutorialCard
            key={step.step}
            step={step}
            settings={state.reminderSettings}
            onSettingsChange={viewModel.updateReminderSettings}
            language={language}
            onLanguageChange={setLanguage}
            onBack={viewModel.previousTutorialStep}
            onNext={() => (step === null ? undefined : isLastStep(step) ? finishTutorial() : viewModel.nextTutorialStep())}
            onSkip={finishTutorial}
          />
        )}
        <nav
          className={`nav-bar ${background.className ?? ''}`}
          inert={background.inert}
          role="tablist"
        >
          {/* The tabs together are spotlit as wide as the content, each tab around its indicator and label */}
          <span className="nav-spotlight" aria-hidden {...tutorialTarget('TAB_BAR')} />
          {APP_TABS.map(tab => (
            <button
              key={tab.tab}
              type="button"
              role="tab"
              className={`nav-item ${tab.tab === 'BOXES' ? 'main' : ''}`}
              aria-selected={tab.tab === state.selectedTab}
              onClick={() => viewModel.selectTab(tab.tab)}
            >
              <span className="indicator" {...tutorialTarget(tab.target)}>
                <Icon name={tab.icon} />
              </span>
              <span className="nav-label label-medium" {...tutorialTarget(tab.target)}>
                {strings.t(tab.label)}
              </span>
            </button>
          ))}
        </nav>
      </div>
      {step && (
        <TutorialOverlay
          highlight={step.target}
          // The tab bar already holds the tab
          tab={step.target && step.target !== 'TAB_BAR' ? tabTarget(step.tab) : null}
        />
      )}
      <UpdatePrompt update={state.availableUpdate} onDismiss={viewModel.dismissUpdate} />
      {isInstallShown && <InstallDialog onDismiss={onInstallDismiss} />}
    </>
  )
}

const isLastStep = (step: { step: string }) => step.step === 'DONE'

function TabContent({
  tab,
  focusedPrescriptionId,
  focusedBoxId,
  settingsScroll,
  prescriptionsFilter,
  onPrescriptionsFilterChange,
  boxesFilter,
  onBoxesFilterChange,
}: {
  tab: AppTab
  focusedPrescriptionId: number | null
  focusedBoxId: number | null
  settingsScroll: number
  prescriptionsFilter: ListFilter
  onPrescriptionsFilterChange: (filter: ListFilter) => void
  boxesFilter: ListFilter
  onBoxesFilterChange: (filter: ListFilter) => void
}) {
  const { viewModel } = useServices()
  switch (tab) {
    case 'SETTINGS':
      return <SettingsScreen initialScroll={settingsScroll} onHelp={viewModel.startTutorial} />
    case 'PRESCRIPTIONS':
      return (
        <PrescriptionsScreen
          filter={prescriptionsFilter}
          onFilterChange={onPrescriptionsFilterChange}
          focusedPrescriptionId={focusedPrescriptionId}
          onOpenBox={viewModel.openBox}
        />
      )
    case 'BOXES':
      return (
        <BoxesScreen
          filter={boxesFilter}
          onFilterChange={onBoxesFilterChange}
          focusedBoxId={focusedBoxId}
          onOpenPrescription={viewModel.openPrescription}
        />
      )
    case 'REFILLS':
      return <RefillsScreen onOpenPrescription={viewModel.openPrescription} onOpenBox={viewModel.openBox} />
  }
}
