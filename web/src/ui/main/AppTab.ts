import type { TutorialTarget } from '../components/TutorialTarget'

/** The app's tabs, in display order, each with its label, icon and how the tutorial points at it in the tab bar. */
export const APP_TABS = [
  { tab: 'SETTINGS', label: 'main_tab_settings', icon: 'ic_settings', target: 'TAB_SETTINGS' },
  { tab: 'PRESCRIPTIONS', label: 'main_tab_prescriptions', icon: 'ic_prescription', target: 'TAB_PRESCRIPTIONS' },
  { tab: 'BOXES', label: 'main_tab_boxes', icon: 'ic_box', target: 'TAB_BOXES' },
  { tab: 'REFILLS', label: 'main_tab_refills', icon: 'ic_refill', target: 'TAB_REFILLS' },
] as const satisfies readonly { tab: string; label: string; icon: string; target: TutorialTarget }[]

export type AppTab = (typeof APP_TABS)[number]['tab']
