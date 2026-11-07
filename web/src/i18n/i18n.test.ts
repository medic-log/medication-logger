import { describe, expect, test } from 'vitest'
import resources from 'virtual:android-resources'
import { Strings, format } from './i18n'
import { WEB_STRINGS } from './webStrings'

describe('strings', () => {
  test('come from the Android app, in all its languages', () => {
    expect(Object.keys(resources.strings).sort()).toEqual(['en', 'es', 'nl', 'pt'])
    expect(new Strings('en').t('main_tab_boxes')).toBe('Boxes')
    expect(new Strings('en').t('edit_delete_message')).toBe("It can't be undone.")
  })

  test('are formatted like Android', () => {
    expect(format('%1$s: %2$s', ['Expires', '2028-06'])).toBe('Expires: 2028-06')
    expect(new Strings('en').t('boxes_pills_left', 23, 30)).toBe('23/30 pills')
    expect(new Strings('en').t('link_box_number', 4)).toBe('#4')
  })

  test('pick the plural form of their language', () => {
    const en = new Strings('en')
    expect(en.plural('refills_pills', 1, 1)).toBe('1 pill')
    expect(en.plural('refills_pills', 7, 7)).toBe('7 pills')
    expect(new Strings('es').plural('reminders_days', 1, 1)).toMatch(/^1 /)
  })

  test('the web strings are translated in every language', () => {
    const keys = Object.keys(WEB_STRINGS.en).sort()
    for (const language of ['es', 'pt', 'nl']) expect(Object.keys(WEB_STRINGS[language]).sort()).toEqual(keys)
  })

  test('name weekdays and months in the language', () => {
    expect(new Strings('en').dayName('SUNDAY')).toBe('Sunday')
    expect(new Strings('es').dayName('MONDAY')).toBe('lunes')
    expect(new Strings('en').monthName(1)).toBe('Jan')
  })

  test('every Android string has a value in English', () => {
    for (const value of Object.values(resources.strings.en)) expect(value).toBeTruthy()
  })
})
