import type { MedicationRepository } from '../../data/MedicationRepository'
import type { PrescriptionDraft, ReminderSettings } from '../../data/models'
import type { AppTab } from './AppTab'
import { TUTORIAL_STEPS, type TutorialStep } from './TutorialStep'
import { tutorialSample } from './tutorialSample'

/** The current tab, the record a link pointed to and where Settings opens scrolled to. */
export interface Navigation {
  selectedTab: AppTab
  focusedPrescriptionId: number | null
  focusedBoxId: number | null
  settingsScroll: number
}

/** A newer version, ready to switch to. */
export interface AvailableUpdate {
  versionName: string
}

/**
 * What the app's frame shows: the current tab, the record a link pointed to, any save problem,
 * the tutorial step on screen, if any, with the settings it asks for, and a newer version to offer.
 */
export interface MainUiState extends Navigation {
  isSaveFailed: boolean
  tutorialStep: TutorialStep | null
  reminderSettings: ReminderSettings
  availableUpdate: AvailableUpdate | null
}

const navigationTo = (selectedTab: AppTab): Navigation => ({
  selectedTab,
  focusedPrescriptionId: null,
  focusedBoxId: null,
  settingsScroll: 0,
})

/**
 * Tracks the selected tab, follows links between records, and leads the tutorial, which opens until it's done:
 * a port of MainViewModel.kt. The tutorial shows sample records made from [sampleDraft] in place of the saved data.
 * A newer version found is offered once the tutorial is out of the way.
 */
export class MainViewModel {
  private navigation: Navigation
  private tutorialStep: TutorialStep | null
  private returnScroll = 0
  private availableUpdate: AvailableUpdate | null = null
  private uiState: MainUiState
  private readonly listeners = new Set<() => void>()

  constructor(
    private readonly repository: MedicationRepository,
    private readonly sampleDraft: () => PrescriptionDraft,
  ) {
    this.tutorialStep = repository.getState().data.isTutorialDone ? null : TUTORIAL_STEPS[0]
    this.navigation = navigationTo(this.tutorialStep?.tab ?? 'BOXES')
    if (this.tutorialStep) this.showSample()
    this.uiState = this.computeState()
    repository.subscribe(this.refresh)
  }

  getState = (): MainUiState => this.uiState

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  selectTab = (tab: AppTab) => this.setNavigation(navigationTo(tab))

  openPrescription = (prescriptionId: number) =>
    this.setNavigation({ ...navigationTo('PRESCRIPTIONS'), focusedPrescriptionId: prescriptionId })

  openBox = (boxId: number) => this.setNavigation({ ...navigationTo('BOXES'), focusedBoxId: boxId })

  /** Opens the tutorial from Settings' Help, scrolled to [settingsScroll], where Settings returns when it closes. */
  startTutorial = (settingsScroll = 0) => {
    this.returnScroll = settingsScroll
    this.showSample()
    this.showTutorialStep(TUTORIAL_STEPS[0])
  }

  nextTutorialStep = () => {
    const next = this.tutorialStep && TUTORIAL_STEPS[TUTORIAL_STEPS.indexOf(this.tutorialStep) + 1]
    if (next) this.showTutorialStep(next)
    else this.finishTutorial()
  }

  previousTutorialStep = () => {
    const previous = this.tutorialStep && TUTORIAL_STEPS[TUTORIAL_STEPS.indexOf(this.tutorialStep) - 1]
    if (previous) this.showTutorialStep(previous)
  }

  /**
   * Closes the tutorial, from its last step or by skipping it, so it no longer opens on its own, and goes to Settings:
   * to its top after the tour at launch, or back to where Help was tapped.
   */
  finishTutorial = () => {
    this.tutorialStep = null
    this.navigation = { ...navigationTo('SETTINGS'), settingsScroll: this.returnScroll }
    this.repository.showSample(null)
    this.repository.finishTutorial()
    this.refresh()
  }

  /** Offers [update] once the tutorial is out of the way. */
  offerUpdate = (update: AvailableUpdate | null) => {
    this.availableUpdate = update
    this.refresh()
  }

  /** Stops offering the update until the next start. */
  dismissUpdate = () => this.offerUpdate(null)

  updateReminderSettings = (settings: ReminderSettings) => this.repository.updateReminderSettings(settings)

  private showSample() {
    const settings = this.repository.getState().data.reminderSettings
    this.repository.showSample(tutorialSample(this.sampleDraft(), settings, this.repository.now()))
  }

  private showTutorialStep(step: TutorialStep) {
    this.tutorialStep = step
    this.setNavigation(navigationTo(step.tab))
  }

  private setNavigation(navigation: Navigation) {
    this.navigation = navigation
    this.refresh()
  }

  private computeState(): MainUiState {
    const { data, isSaveFailed } = this.repository.getState()
    return {
      ...this.navigation,
      isSaveFailed,
      tutorialStep: this.tutorialStep,
      reminderSettings: data.reminderSettings,
      availableUpdate: this.tutorialStep ? null : this.availableUpdate,
    }
  }

  private refresh = () => {
    this.uiState = this.computeState()
    this.listeners.forEach(listener => listener())
  }
}
