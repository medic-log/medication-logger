/** The buttons in the settings section, with their label and icon. */
export const SETTINGS_ACTIONS = {
  HELP: { label: 'settings_help', icon: 'ic_help' },
  UPDATE: { label: 'settings_update', icon: 'ic_update' },
  EXPORT: { label: 'settings_export', icon: 'ic_export' },
  IMPORT: { label: 'settings_import', icon: 'ic_import' },
  SHARE: { label: 'settings_share', icon: 'ic_share' },
  LANGUAGE: { label: 'settings_language', icon: 'ic_translation' },
  DATE_FORMAT: { label: 'settings_date_format', icon: 'ic_calendar' },
  FEEDBACK: { label: 'settings_feedback', icon: 'ic_feedback' },
} as const

export type SettingsAction = keyof typeof SETTINGS_ACTIONS

/** The buttons as laid out, row by row. */
export const SETTINGS_ROWS: SettingsAction[][] = [
  ['HELP', 'LANGUAGE', 'DATE_FORMAT', 'UPDATE'],
  ['SHARE', 'IMPORT', 'EXPORT', 'FEEDBACK'],
]
