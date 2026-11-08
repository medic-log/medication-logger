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
    expect(new Strings('en').t('refills_partial_suffix', '2028-06-01', 'partial')).toBe('2028-06-01, partial')
    expect(new Strings('en').t('link_box_number', 4)).toBe('#4')
  })

  test('show dates and months in each fixed style', () => {
    const date = '2026-11-04'
    expect(new Strings('en').date(date)).toBe('2026-11-04')
    expect(new Strings('en', 'DAY_MONTH_YEAR').date(date)).toBe('04/11/2026')
    expect(new Strings('en', 'MONTH_DAY_YEAR').date(date)).toBe('11/04/2026')
    expect(new Strings('en').month('2026-11')).toBe('2026-11')
    expect(new Strings('en', 'DAY_MONTH_YEAR').month('2026-11')).toBe('11/2026')
    expect(new Strings('en', 'MONTH_DAY_YEAR').month('2026-11')).toBe('11/2026')
  })

  test('the language style reads as the language writes dates', () => {
    expect(new Strings('en', 'LANGUAGE').date('2026-11-04')).toBe('Nov 4, 2026')
    expect(new Strings('en', 'LANGUAGE').month('2026-11')).toBe('Nov 2026')
    expect(new Strings('es', 'LANGUAGE').date('2026-11-04')).toBe('4 nov 2026')
    // Without the period Dutch puts after a short month, as on Android
    expect(new Strings('nl', 'LANGUAGE').month('2026-09')).toBe('sep 2026')
  })

  test('a box is named by its bought date and number', () => {
    const box = { id: 1, prescriptionId: 1, boxNumber: 2, boughtDate: '2026-11-04', pillsLeft: 30, pillsTotal: 30,
      isExtra: false, openDate: null, expiryDate: null, notes: '' }
    expect(new Strings('en').boxName(box)).toBe('2026-11-04 #2')
  })

  test('pick the plural form of their language', () => {
    const en = new Strings('en')
    expect(en.plural('refills_pills', 1, 1)).toBe('1 pill')
    expect(en.plural('refills_pills', 7, 7)).toBe('7 pills')
    expect(new Strings('es').plural('reminders_days', 1, 1)).toMatch(/^1 /)
    expect(en.plural('boxes_pills_left', 1, 0, 1)).toBe('0/1 pill')
    expect(en.plural('boxes_pills_left', 30, 23, 30)).toBe('23/30 pills')
    expect(new Strings('es').plural('refills_box', 1, '#1', 1)).toBe('Caja #1 (queda 1)')
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
