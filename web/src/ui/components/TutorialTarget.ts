/** What the tutorial can point at, across the tabs. */
export type TutorialTarget =
  | 'TAB_BAR'
  | 'FIRST_PRESCRIPTION'
  | 'NEW_PRESCRIPTION'
  | 'FIRST_BOX'
  | 'FIRST_REFILL'
  | 'DOSE_LOG'
  | 'PILL_REMINDER'
  | 'REFILL_REMINDER'
  | 'TAB_SETTINGS'
  | 'TAB_PRESCRIPTIONS'
  | 'TAB_BOXES'
  | 'TAB_REFILLS'
  | 'TUTORIAL_CARD'

/**
 * Lets the tutorial find this element as each of [targets]: spread onto the element. A target marked on several
 * elements is spotlit around all of them.
 */
export const tutorialTarget = (...targets: TutorialTarget[]) => ({ 'data-tutorial-target': targets.join(' ') })
