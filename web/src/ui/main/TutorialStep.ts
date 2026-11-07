import type { TutorialTarget } from '../components/TutorialTarget'
import type { AppTab } from './AppTab'

/**
 * The tutorial's steps in order, each shown over the tab it explains and spotlighting its target along with that
 * tab. Steps without a target blur the app instead.
 */
export const TUTORIAL_STEPS = [
  { step: 'LANGUAGE', tab: 'BOXES', target: null, title: 'tutorial_language_title', text: 'tutorial_language_text' },
  { step: 'WELCOME', tab: 'BOXES', target: null, title: 'tutorial_welcome_title', text: 'tutorial_welcome_text' },
  { step: 'TABS', tab: 'BOXES', target: 'TAB_BAR', title: 'tutorial_tabs_title', text: 'tutorial_tabs_text' },
  {
    step: 'PRESCRIPTION', tab: 'PRESCRIPTIONS', target: 'FIRST_PRESCRIPTION',
    title: 'tutorial_prescription_title', text: 'tutorial_prescription_text',
  },
  { step: 'ADD', tab: 'PRESCRIPTIONS', target: 'NEW_PRESCRIPTION', title: 'tutorial_add_title', text: 'tutorial_add_text' },
  { step: 'BOX', tab: 'BOXES', target: 'FIRST_BOX', title: 'tutorial_box_title', text: 'tutorial_box_text' },
  { step: 'EDIT', tab: 'BOXES', target: 'FIRST_BOX', title: 'tutorial_edit_title', text: 'tutorial_edit_text' },
  { step: 'REFILLS', tab: 'REFILLS', target: 'FIRST_REFILL', title: 'tutorial_refills_title', text: 'tutorial_refills_text' },
  { step: 'DOSES', tab: 'REFILLS', target: 'DOSE_LOG', title: 'tutorial_doses_title', text: 'tutorial_doses_text' },
  {
    step: 'PILL_TIME', tab: 'SETTINGS', target: 'PILL_REMINDER',
    title: 'tutorial_pill_time_title', text: 'tutorial_pill_time_text',
  },
  {
    step: 'REFILL_TIME', tab: 'SETTINGS', target: 'REFILL_REMINDER',
    title: 'tutorial_refill_time_title', text: 'tutorial_refill_time_text',
  },
  { step: 'DONE', tab: 'SETTINGS', target: null, title: 'tutorial_done_title', text: 'tutorial_done_text' },
] as const satisfies readonly { step: string; tab: AppTab; target: TutorialTarget | null; title: string; text: string }[]

export type TutorialStep = (typeof TUTORIAL_STEPS)[number]
