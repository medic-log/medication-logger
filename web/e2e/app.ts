// Helpers for the user flow tests, ported from MainActivityTest: texts from the Android strings.xml, the saved data,
// and the gestures the tests use.
import { type Locator, type Page, expect, test as base } from '@playwright/test'
import { fileURLToPath } from 'node:url'
import { loadAndroidResources } from '../android-resources.ts'
import { WEB_STRINGS } from '../src/i18n/webStrings.ts'

const resources = loadAndroidResources(fileURLToPath(new URL('../..', import.meta.url)))
export const versionName = resources.versionName

function format(template: string, args: (string | number)[]): string {
  let next = 0
  return template.replace(/%(?:(\d+)\$)?([sd%])/g, (_, position: string | undefined, type: string) =>
    type === '%' ? '%' : String(args[position ? Number(position) - 1 : next++]),
  )
}

/** The English string [key], as the app shows it. */
export function string(key: string, ...args: (string | number)[]): string {
  const value = WEB_STRINGS.en[key] ?? resources.strings.en[key]
  if (typeof value !== 'string') throw new Error(`No string ${key}`)
  return format(value, args)
}

/** The English plural [key] for [count]. */
export function plural(key: string, count: number, ...args: (string | number)[]): string {
  const forms = resources.strings.en[key]
  if (typeof forms !== 'object') throw new Error(`No plural ${key}`)
  return format(count === 1 ? forms.one : forms.other, args)
}

/** The saved data, as the app keeps it in localStorage. */
export async function savedData(page: Page) {
  return page.evaluate(() => JSON.parse(localStorage.getItem('medication-data') ?? 'null'))
}

/** The date in the browser, as the app dates records. */
export async function today(page: Page): Promise<string> {
  return page.evaluate(() => {
    const now = new Date()
    const pad = (value: number) => String(value).padStart(2, '0')
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  })
}

export class App {
  constructor(readonly page: Page) {}

  text = (text: string) => this.page.getByText(text, { exact: true })
  button = (name: string) => this.page.getByRole('button', { name, exact: true })
  tab = (key: string) => this.page.getByRole('tab', { name: string(key) })
  field = (key: string, ...args: (string | number)[]) => this.page.getByLabel(string(key, ...args), { exact: true })
  dialog = () => this.page.locator('dialog[open]').last()

  async tap(text: string) {
    await this.button(text).click()
  }

  async openTab(key: string) {
    await this.tab(key).click()
  }

  async fill(key: string, value: string) {
    await this.field(key).fill(value)
  }

  async addPrescription(pillsPerBox: number, boxesPerRepetition: number, repetitions: number) {
    await this.page.locator('.fab').click()
    await this.fill('prescriptions_field_medication', 'Test medication')
    await this.fill('prescriptions_field_doctor', 'Dr Test')
    await this.fill('prescriptions_field_pharmacy', 'Test Pharmacy')
    await this.fill('prescriptions_field_pills_per_box', String(pillsPerBox))
    await this.fill('prescriptions_field_boxes_per_repetition', String(boxesPerRepetition))
    await this.fill('prescriptions_field_repetitions_total', String(repetitions))
    await this.tap(string('prescriptions_save'))
  }

  async buyRepetition() {
    await this.button(string('prescriptions_buy')).first().click()
    // The first date chosen fills in the rest
    await this.dialog().getByRole('button', { name: string('month_selector_empty', '') }).first().click()
    await this.tap('Jan')
    await this.tap(string('prescriptions_save'))
  }

  async addBox() {
    await this.openTab('main_tab_prescriptions')
    await this.addPrescription(30, 1, 1)
    await this.buyRepetition()
    await this.openTab('main_tab_boxes')
  }

  async refillFirstBox() {
    await this.button(string('boxes_refill_weekly')).first().click()
  }

  /** Holds [target] down for [millis], as a finger or mouse would. */
  async press(target: Locator, millis: number) {
    const box = (await target.boundingBox())!
    await this.page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await this.page.mouse.down()
    await this.page.waitForTimeout(millis)
    await this.page.mouse.up()
  }

  /** Holds the card showing [text] for a second and confirms editing it. */
  async hold(text: string) {
    await this.press(this.text(text).first(), 1100)
    await this.tap(string('edit_action'))
  }
}

/** A test with the app open, its tutorial skipped, as every MainActivityTest starts. */
export const test = base.extend<{ app: App }>({
  app: async ({ page }, use) => {
    await page.goto('./')
    await page.getByRole('button', { name: string('tutorial_skip') }).click()
    await use(new App(page))
  },
})

export { expect }
