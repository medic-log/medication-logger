// What the web app does in place of the Android platform: saving in the browser, files, language, installing,
// updating, sharing and working offline.
import { readFileSync } from 'node:fs'
import { expect, savedData, string, test, today, versionName } from './app'

test('records survive a reload', async ({ app, page }) => {
  await app.addBox()
  await page.reload()
  await expect(app.text(`${await today(page)} #1`)).toBeVisible()
  await expect(app.text(string('tutorial_language_title'))).toHaveCount(0)
})

test('export writes the records, and import replaces them after asking twice', async ({ app, page }, testInfo) => {
  await app.addBox()
  await app.openTab('main_tab_settings')
  const download = page.waitForEvent('download')
  await app.tap(string('settings_export'))
  const file = await download
  expect(file.suggestedFilename()).toBe(`medication-logger-${await today(page)}.json`)
  const path = testInfo.outputPath('export.json')
  await file.saveAs(path)
  const exported = JSON.parse(readFileSync(path, 'utf8'))
  expect(Object.keys(exported)).toEqual(['prescriptions', 'boxes', 'refills', 'misses'])
  await expect(page.getByRole('status')).toHaveText(string('settings_export_done'))

  // Something to replace
  await app.openTab('main_tab_refills')
  await app.tap(string('refills_log_missed'))
  await app.openTab('main_tab_settings')
  await app.tap(string('settings_import'))
  await expect(app.text(string('settings_import_confirm_text'))).toBeVisible()
  const chooser = page.waitForEvent('filechooser')
  await app.tap(string('settings_import_confirm'))
  await (await chooser).setFiles(path)
  await expect(app.text(string('settings_import_replace_text'))).toBeVisible()
  await app.tap(string('settings_import_replace'))
  await expect(page.getByRole('status')).toHaveText(string('settings_import_done'))
  const data = await savedData(page)
  expect(data.misses).toHaveLength(0)
  expect(data.boxes).toHaveLength(1)
})

test("an Android app export imports, and a file that isn't one is refused", async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  const android = JSON.stringify({
    prescriptions: [{ id: 1, medication: 'Med', prescribedDate: '2026-11-02', prescribedBy: 'Dr Android', pharmacy: 'Ph',
      pillsPerBox: 30, boxesPerRepetition: 1, repetitionsTotal: 2, status: 'IN_PROGRESS', boughtFirstDate: '2026-11-02',
      boughtLastDate: '2026-11-02', repetitionsBought: 1 }],
    boxes: [{ id: 1, prescriptionId: 1, boxNumber: 1, boughtDate: '2026-11-02', pillsLeft: 23, pillsTotal: 30,
      isExtra: false, openDate: '2026-11-02', expiryDate: '2028-06', notes: '' }],
    refills: [{ id: 1, boxId: 1, prescriptionId: 1, date: '2026-11-02', amount: 7, amountRemaining: 23, isPartial: false }],
    misses: [{ id: 1, date: '2026-11-03', kind: 'DELAYED', time: '09:30' }],
  }, null, 2)
  for (const [text, message] of [['not an export', 'settings_import_failed'], [android, 'settings_import_done']]) {
    await app.tap(string('settings_import'))
    const chooser = page.waitForEvent('filechooser')
    await app.tap(string('settings_import_confirm'))
    await (await chooser).setFiles({ name: 'records.json', mimeType: 'application/json', buffer: Buffer.from(text) })
    await app.tap(string('settings_import_replace'))
    await expect(page.getByRole('status')).toHaveText(string(message))
  }
  await app.openTab('main_tab_prescriptions')
  await expect(page.getByText('Dr Android')).toBeVisible()
})

test('the language is English until another is chosen, and stays chosen', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await app.text(string('settings_language')).click()
  await page.getByRole('radio', { name: 'Nederlands' }).click()
  // The dialog previews it before it's confirmed
  await expect(page.getByRole('button', { name: 'Annuleren' })).toBeVisible()
  await expect(app.tab('main_tab_settings')).toHaveText(string('main_tab_settings'))
  await page.getByRole('button', { name: 'Bevestigen' }).click()
  await expect(page.getByRole('tab', { name: 'Instellingen' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('tab', { name: 'Instellingen' })).toBeVisible()
  await expect(page.locator('html')).toHaveAttribute('lang', 'nl')
})

test('the browser language is never used', async ({ browser }) => {
  const context = await browser.newContext({ locale: 'es-ES' })
  const page = await context.newPage()
  await page.goto('./')
  await expect(page.getByText(string('tutorial_language_title'))).toBeVisible()
  await context.close()
})

test('the tutorial language step switches the app on Next', async ({ page }) => {
  await page.goto('./')
  await page.getByRole('radio', { name: 'Português' }).click()
  await page.getByRole('button', { name: 'Próximo' }).click()
  await expect(page.getByRole('tab', { name: 'Caixas' })).toBeVisible()
})

test('settings show the version, and update says when it is the latest', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await expect(app.text(string('settings_version', versionName))).toBeVisible()
  await app.tap(string('settings_update'))
  await expect(page.getByRole('status')).toHaveText(string('settings_update_latest'))
})

test('a newer deployed version is offered after the tutorial', async ({ page }) => {
  await page.route('**/version.json', route => route.fulfill({ json: { versionCode: 99999, versionName: '99.0.0' } }))
  await page.goto('./')
  await expect(page.getByText(string('settings_update_confirm_title'))).toHaveCount(0)
  await page.getByRole('button', { name: string('tutorial_skip') }).click()
  await expect(page.getByText(string('web_update_confirm_text', '99.0.0', versionName))).toBeVisible()
  await page.getByRole('button', { name: string('settings_update_later') }).click()
  await expect(page.getByText(string('settings_update_confirm_title'))).toHaveCount(0)
})

test('share shows the download site as a QR code', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await app.tap(string('settings_share'))
  await expect(page.getByRole('img', { name: string('settings_share_qr_description') })).toBeVisible()
  await app.tap(string('settings_share_close'))
})

test('the download page link opens the install dialog', async ({ page }) => {
  await page.goto('./?install')
  await expect(page.getByText(string('web_install_title'))).toBeVisible()
  await expect(page.getByText(string('web_install_other'))).toBeVisible()
  await page.getByRole('button', { name: string('settings_share_close') }).click()
  await expect(page).not.toHaveURL(/install/)
})

test('the notice asks for notifications, and blocked ones say where to allow them', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await expect(app.text(string('web_notifications_off'))).toBeVisible()
  await page.evaluate(() => {
    Notification.requestPermission = async () => 'denied'
  })
  await app.tap(string('web_notifications_allow'))
  await expect(page.getByRole('status')).toHaveText(string('web_notifications_blocked'))
})

test('the app works offline once loaded', async ({ app, page, context }) => {
  await app.addBox()
  await page.evaluate(() => navigator.serviceWorker.ready)
  // The service worker controls the page from the next load
  await page.reload()
  await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true)
  await context.setOffline(true)
  await page.reload()
  await expect(app.text(`${await today(page)} #1`)).toBeVisible()
  await context.setOffline(false)
})

test('the manifest makes it installable', async ({ page }) => {
  const response = await page.request.get('manifest.webmanifest')
  const manifest = await response.json()
  expect(manifest).toMatchObject({ name: 'Medication logger', display: 'standalone', start_url: '.', scope: '.' })
  expect(manifest.icons.map((it: { sizes: string }) => it.sizes)).toEqual(expect.arrayContaining(['192x192', '512x512']))
})
