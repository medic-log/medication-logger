// The app's texts in its languages, from the Android app's strings.xml plus the web's own few, formatted like
// Android's getString and getQuantityString.
import { createContext, useContext } from 'react'
import resources from 'virtual:android-resources'
import { WEB_STRINGS } from './webStrings'
import { type DayOfWeek, type LocalDate, DAYS_OF_WEEK, dateOf, toEpochDay } from '../data/time'

/** The languages the app is translated into, each named in itself. */
export const APP_LANGUAGES = [
  { tag: 'en', nativeName: 'English' },
  { tag: 'es', nativeName: 'Español' },
  { tag: 'pt', nativeName: 'Português' },
  { tag: 'nl', nativeName: 'Nederlands' },
] as const

export type AppLanguage = (typeof APP_LANGUAGES)[number]['tag']

const LANGUAGE_KEY = 'app-language'

/** The language chosen in the app, English if none is: it never follows the browser's language. */
export function currentLanguage(): AppLanguage {
  try {
    const tag = localStorage.getItem(LANGUAGE_KEY)
    return APP_LANGUAGES.find(it => it.tag === tag)?.tag ?? 'en'
  } catch {
    return 'en'
  }
}

export function saveLanguage(language: AppLanguage) {
  try {
    localStorage.setItem(LANGUAGE_KEY, language)
  } catch {
    // Without storage the choice lasts until the page closes
  }
}

/** Fills Android format arguments: %1$s, %1$d, %s, %d and %%. */
export function format(template: string, args: (string | number)[]): string {
  let next = 0
  return template.replace(/%(?:(\d+)\$)?([sd%])/g, (match, position: string | undefined, type: string) => {
    if (type === '%') return '%'
    const value = args[position ? Number(position) - 1 : next++]
    return value === undefined ? match : String(value)
  })
}

/** Looks up and formats the texts of one language, falling back to English as Android does. */
export class Strings {
  private readonly plurals: Intl.PluralRules

  constructor(readonly language: AppLanguage) {
    this.plurals = new Intl.PluralRules(language)
  }

  private lookup(key: string) {
    return (
      WEB_STRINGS[this.language]?.[key] ??
      resources.strings[this.language]?.[key] ??
      WEB_STRINGS.en[key] ??
      resources.strings.en[key]
    )
  }

  /** The string [key], formatted with [args]. */
  t = (key: string, ...args: (string | number)[]): string => {
    const value = this.lookup(key)
    if (typeof value !== 'string') return key
    return format(value, args)
  }

  /** The plural [key] for [count], formatted with [args]. */
  plural = (key: string, count: number, ...args: (string | number)[]): string => {
    const forms = this.lookup(key)
    if (typeof forms !== 'object') return key
    return format(forms[this.plurals.select(count)] ?? forms.other ?? '', args)
  }

  /** The weekday's full name, as DayOfWeek.getDisplayName(FULL). */
  dayName = (day: DayOfWeek): string => {
    // 2024-01-01 was a Monday
    const date = dateOf(2024, 1, 1 + DAYS_OF_WEEK.indexOf(day))
    return new Intl.DateTimeFormat(this.language, { weekday: 'long', timeZone: 'UTC' }).format(utcDate(date))
  }

  /** The month's short name, as Month.getDisplayName(SHORT), without the period some languages add. */
  monthName = (month: number): string =>
    new Intl.DateTimeFormat(this.language, { month: 'short', timeZone: 'UTC' })
      .format(utcDate(dateOf(2024, month, 1)))
      .replace(/\.$/, '')
}

const utcDate = (date: LocalDate) => new Date(toEpochDay(date) * 86_400_000)

export const StringsContext = createContext(new Strings('en'))

/** The texts in the language of where it's used. */
export const useStrings = () => useContext(StringsContext)
