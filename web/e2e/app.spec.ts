// The user flows of app/src/test/.../MainActivityTest.kt, run in a browser against the production build.
import { expect, plural, savedData, string, test, today } from './app'

test('app opens on the boxes tab', async ({ app }) => {
  await expect(app.tab('main_tab_boxes')).toHaveAttribute('aria-selected', 'true')
  await expect(app.text(string('boxes_empty_active'))).toBeVisible()
})

test('tabs are in the order settings, prescriptions, boxes, refills', async ({ app }) => {
  const tabs = await app.page.getByRole('tab').allInnerTexts()
  expect(tabs).toEqual(['main_tab_settings', 'main_tab_prescriptions', 'main_tab_boxes', 'main_tab_refills'].map(it => string(it)))
})

test('a new prescription is dated today, shows its totals and buys boxes', async ({ app, page }) => {
  const day = await today(page)
  await app.openTab('main_tab_prescriptions')
  await expect(app.text(string('prescriptions_empty_active'))).toBeVisible()

  await app.addPrescription(30, 3, 2)
  for (const text of [
    string('prescriptions_prescribed_date', day),
    string('prescriptions_prescribed_by', 'Dr Test'),
    string('pharmacy_label', 'Test Pharmacy'),
    string('prescriptions_per_repetition', 3, 90),
    string('prescriptions_total', 6, 180),
    string('prescriptions_status_not_started'),
  ]) {
    await expect(app.text(text)).toBeVisible()
  }

  await app.tap(string('prescriptions_order'))
  await expect(app.text(string('prescriptions_status_ordered'))).toBeVisible()
  await expect(app.page.getByText(string('prescriptions_repetitions', 0, 2))).toBeVisible()
  await expect(app.button(string('prescriptions_order'))).toBeDisabled()

  await app.buyRepetition()
  await expect(app.text(string('prescriptions_status_in_progress'))).toBeVisible()
  await expect(app.page.getByText(string('prescriptions_repetitions', 1, 2))).toBeVisible()
  await expect(app.text(string('prescriptions_bought_first', day))).toBeVisible()
  await expect(app.text(string('prescriptions_bought_last', day))).toBeVisible()
  await expect(app.page.getByRole('button', { name: string('link_box_number', 1) })).toBeVisible()
  await expect(app.page.getByRole('button', { name: string('link_box_number', 3) })).toBeVisible()

  await app.openTab('main_tab_boxes')
  for (const box of [1, 2, 3]) await expect(app.text(`${day} #${box}`)).toBeVisible()
  await expect(app.page.getByText(string('boxes_pills_left', 30, 30)).first()).toBeVisible()

  await app.openTab('main_tab_prescriptions')
  await app.buyRepetition()
  // Now finished, so only under "All"
  await app.page.getByRole('radio', { name: string('filter_all') }).click()
  await expect(app.page.getByRole('button', { name: string('link_box_number', 4) })).toBeVisible()
  await expect(app.page.getByRole('button', { name: string('link_box_number', 6) })).toBeVisible()
  await expect(app.button(string('prescriptions_buy'))).toBeDisabled()
})

test("buying asks each box's expiry date, starting them all from the first one entered", async ({ app, page }) => {
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(30, 3, 1)

  await app.tap(string('prescriptions_buy'))
  await expect(app.button(string('prescriptions_save'))).toBeDisabled()
  expect((await savedData(page)).boxes).toHaveLength(0)

  const year = Number((await today(page)).slice(0, 4))
  const january = `${year}-01`
  const nextJune = `${year + 1}-06`
  const field = (box: number) => string('prescriptions_expiry_field', box)
  await app.tap(string('month_selector_empty', field(1)))
  await app.tap('Jan')
  for (const box of [1, 2, 3]) await expect(app.button(string('month_selector_value', field(box), january))).toBeVisible()

  await app.tap(string('month_selector_value', field(3), january))
  await app.page.getByRole('button', { name: string('month_selector_next_year') }).click()
  await app.tap('Jun')
  await expect(app.button(string('month_selector_value', field(1), january))).toBeVisible()
  await expect(app.button(string('month_selector_value', field(3), nextJune))).toBeVisible()
  await app.tap(string('prescriptions_save'))

  expect((await savedData(page)).boxes.map((it: { expiryDate: string }) => it.expiryDate)).toEqual([january, january, nextJune])
  await app.openTab('main_tab_boxes')
  await app.page.getByRole('radio', { name: string('filter_all') }).click()
  await expect(app.text(string('boxes_expires', nextJune))).toHaveCount(1)
})

test("cancelling the expiry dates doesn't buy", async ({ app, page }) => {
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(30, 1, 1)
  await app.tap(string('prescriptions_buy'))
  await app.tap(string('prescriptions_cancel'))
  expect((await savedData(page)).boxes).toHaveLength(0)
})

test('the first refill shows when the box was opened', async ({ app, page }) => {
  await app.addBox()
  const opened = string('boxes_opened', await today(page))
  await expect(app.text(opened)).toHaveCount(0)
  await app.refillFirstBox()
  await expect(app.text(opened)).toBeVisible()
})

test('a new prescription starts from the previous values', async ({ app }) => {
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(28, 2, 5)
  await app.page.locator('.fab').click()
  await expect(app.field('prescriptions_field_doctor')).toHaveValue('Dr Test')
  await expect(app.field('prescriptions_field_pharmacy')).toHaveValue('Test Pharmacy')
  await expect(app.field('prescriptions_field_pills_per_box')).toHaveValue('28')
  await expect(app.field('prescriptions_field_boxes_per_repetition')).toHaveValue('2')
  await expect(app.field('prescriptions_field_repetitions_total')).toHaveValue('5')
  await expect(app.button(string('prescriptions_save'))).toBeEnabled()
})

test('save stays disabled until the prescription is complete', async ({ app }) => {
  await app.openTab('main_tab_prescriptions')
  await app.page.locator('.fab').click()
  await app.fill('prescriptions_field_medication', 'Test medication')
  await app.fill('prescriptions_field_doctor', 'Dr Test')
  await expect(app.button(string('prescriptions_save'))).toBeDisabled()
})

test("partial refill is highlighted while the weekly lot won't last until the next refill", async ({ app, page }) => {
  // Seven pill times until the next refill, unless the test runs in the day's last minute
  await page.evaluate(() => {
    const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
    const data = JSON.parse(localStorage.getItem('medication-data')!)
    data.reminderSettings.refillDay = days[(new Date().getDay() + 6) % 7]
    data.reminderSettings.refillTime = '23:59'
    data.reminderSettings.pillTime = '23:59'
    localStorage.setItem('medication-data', JSON.stringify(data))
  })
  await page.reload()
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(4, 1, 1)
  await app.buyRepetition()
  // The filter hides the finished prescription
  await page.locator('.fab').click()
  await app.fill('prescriptions_field_pills_per_box', '30')
  await app.tap(string('prescriptions_save'))
  await app.buyRepetition()
  await app.openTab('main_tab_boxes')

  const pending = plural('boxes_refill_partial_pending', 3, 3)
  await expect(app.button(pending)).toHaveCount(0)
  await app.refillFirstBox()

  await expect(app.text(plural('boxes_pending_notice', 3, 3))).toBeVisible()
  await app.tap(pending)
  await expect(app.button(pending)).toHaveCount(0)
  await expect(app.text(plural('boxes_pending_notice', 3, 3))).toHaveCount(0)
  await expect(app.page.getByText(string('boxes_pills_left', 27, 30))).toBeVisible()

  await app.openTab('main_tab_refills')
  const day = await today(page)
  // Each prescription numbers its own boxes
  for (const left of [0, 27]) await expect(app.text(string('refills_box', `${day} #1`, left))).toBeVisible()
  await expect(app.text(plural('refills_lot_pills', 7, 7))).toBeVisible()
  await expect(app.text(string('refills_partial'))).toHaveCount(2)
  await app.hold(string('refills_box', `${day} #1`, 0))
  await expect(app.text(string('edit_refill_title'))).toBeVisible()
})

test('links open the linked prescription and box, highlighted', async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.refillFirstBox()

  await app.openTab('main_tab_refills')
  await app.page.getByRole('button', { name: string('link_open_box') }).click()
  await expect(app.tab('main_tab_boxes')).toHaveAttribute('aria-selected', 'true')
  await expect(page.locator('.card[aria-current="true"]').filter({ hasText: `${day} #1` })).toBeVisible()

  await app.openTab('main_tab_refills')
  await app.page.getByRole('button', { name: string('link_open_prescription') }).click()
  await expect(app.tab('main_tab_prescriptions')).toHaveAttribute('aria-selected', 'true')
  // The prescription is finished, so the link has to reach past the active filter
  await expect(
    page.locator('.card[aria-current="true"]').filter({ hasText: string('prescriptions_prescribed_date', day) }),
  ).toBeVisible()

  await app.page.getByRole('button', { name: string('link_box_number', 1) }).click()
  await expect(app.tab('main_tab_boxes')).toHaveAttribute('aria-selected', 'true')
  await app.page.getByRole('button', { name: string('link_open_prescription') }).click()
  await expect(app.tab('main_tab_prescriptions')).toHaveAttribute('aria-selected', 'true')
})

test("a box saved as extra leaves the active list, can't be refilled and can go back", async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.tap(string('boxes_save_as_extra'))
  await expect(app.text(`${day} #1`)).toHaveCount(0)
  await expect(app.text(string('boxes_empty_active'))).toBeVisible()

  await app.page.getByRole('radio', { name: string('filter_all') }).click()
  // The filter and the status
  await expect(app.page.getByText(string('boxes_status_extra'), { exact: true })).toHaveCount(2)
  await expect(app.button(string('boxes_refill_weekly'))).toHaveCount(0)

  await app.tap(string('boxes_back_to_inventory'))
  await expect(app.text(string('boxes_status_not_started'))).toBeVisible()
  await expect(app.button(string('boxes_refill_weekly'))).toBeEnabled()
})

test('the filter hides finished prescriptions until all is selected', async ({ app, page }) => {
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(30, 1, 1)
  await app.buyRepetition()
  const prescribedOn = string('prescriptions_prescribed_date', await today(page))
  await expect(app.text(prescribedOn)).toHaveCount(0)
  await app.page.getByRole('radio', { name: string('filter_all') }).click()
  await expect(app.text(prescribedOn)).toBeVisible()
  await expect(app.text(string('prescriptions_status_finished'))).toBeVisible()
  await app.page.getByRole('radio', { name: string('filter_active') }).click()
  await expect(app.text(prescribedOn)).toHaveCount(0)
})

test('misses and delays are registered in the refills tab', async ({ app, page }) => {
  await app.openTab('main_tab_refills')
  await expect(app.text(string('refills_empty'))).toBeVisible()
  await app.tap(string('refills_log_missed'))
  await app.tap(string('refills_log_delayed'))
  await app.tap(string('reminders_time_confirm'))

  await expect(app.text(string('refills_missed'))).toBeVisible()
  await expect(app.text(string('refills_delayed'))).toBeVisible()
  await expect(app.text(await today(page)).first()).toBeVisible()
  const { misses } = await savedData(page)
  expect(misses).toHaveLength(2)
  expect(misses.find((it: { kind: string }) => it.kind === 'DELAYED').time).toMatch(/^\d\d:\d\d$/)
})

test("a short press on a card doesn't open it for editing", async ({ app, page }) => {
  await app.addBox()
  // Longer than the system's long press, shorter than the hold that opens a card
  await app.press(app.text(`${await today(page)} #1`), 700)
  await page.waitForTimeout(500)
  await expect(app.text(string('edit_confirm_title'))).toHaveCount(0)
})

test("holding a card asks first, and cancelling doesn't open it for editing", async ({ app, page }) => {
  await app.addBox()
  await app.press(app.text(`${await today(page)} #1`), 1200)
  await expect(app.text(string('edit_confirm_title'))).toBeVisible()
  await expect(app.text(string('edit_box_title'))).toHaveCount(0)
  await app.tap(string('edit_cancel'))
  await expect(app.text(string('edit_confirm_title'))).toHaveCount(0)
  await expect(app.text(string('edit_box_title'))).toHaveCount(0)
})

test('holding a box card edits its fields', async ({ app, page }) => {
  await app.addBox()
  await app.hold(`${await today(page)} #1`)
  await expect(app.text(string('edit_box_title'))).toBeVisible()
  await app.fill('edit_field_pills_left', '12')
  await app.tap(string('edit_save'))
  expect((await savedData(page)).boxes[0].pillsLeft).toBe(12)
  await expect(app.text(string('edit_box_title'))).toHaveCount(0)
  await expect(page.getByText(string('boxes_pills_left', 12, 30))).toBeVisible()
})

test("a box's expiry date can be cleared when editing", async ({ app, page }) => {
  await app.addBox()
  const label = string('edit_field_expiry_date')
  await app.hold(`${await today(page)} #1`)
  const year = (await today(page)).slice(0, 4)
  await app.tap(string('month_selector_value', label, `${year}-01`))
  await app.tap(string('month_selector_clear'))
  await expect(app.button(string('month_selector_empty', label))).toBeVisible()
  await app.tap(string('edit_save'))
  expect((await savedData(page)).boxes[0]).not.toHaveProperty('expiryDate')
})

test("an edit that isn't valid can't be saved", async ({ app, page }) => {
  await app.addBox()
  await app.hold(`${await today(page)} #1`)
  await app.fill('edit_field_pills_left', '31')
  await expect(app.button(string('edit_save'))).toBeDisabled()
})

test('deleting from the edit dialog asks first', async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.hold(`${day} #1`)
  await app.tap(string('edit_delete'))
  await expect(app.text(string('edit_delete_message'))).toBeVisible()
  await app.tap(string('edit_cancel'))
  expect((await savedData(page)).boxes).toHaveLength(1)
  await expect(app.text(string('edit_box_title'))).toBeVisible()

  await app.tap(string('edit_delete'))
  await app.tap(string('edit_delete'))
  expect((await savedData(page)).boxes).toHaveLength(0)
  await expect(app.text(`${day} #1`)).toHaveCount(0)
})

test('deleting a box can keep its refills', async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.refillFirstBox()
  await app.hold(`${day} #1`)
  await app.tap(string('edit_delete'))
  await expect(app.text(string('edit_box_delete_message'))).toBeVisible()
  await app.tap(string('edit_delete_keep_refills'))

  const data = await savedData(page)
  expect(data.boxes).toHaveLength(0)
  expect(data.refills[0]).not.toHaveProperty('boxId')
  await app.openTab('main_tab_refills')
  await expect(app.text(plural('refills_pills', 7, 7))).toBeVisible()
  await expect(app.text(string('refills_box', `${day} #1`, 23))).toHaveCount(0)
  await expect(page.getByRole('button', { name: string('link_box_deleted') })).toBeDisabled()
  await expect(page.getByRole('button', { name: string('link_open_prescription') })).toBeEnabled()
})

test('deleting a box can delete its refills too', async ({ app, page }) => {
  await app.addBox()
  await app.refillFirstBox()
  await app.hold(`${await today(page)} #1`)
  await app.tap(string('edit_delete'))
  await app.tap(string('edit_delete_all'))
  const data = await savedData(page)
  expect(data.boxes).toHaveLength(0)
  expect(data.refills).toHaveLength(0)
})

test('deleting a prescription can keep its boxes', async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.openTab('main_tab_prescriptions')
  await page.getByRole('radio', { name: string('filter_all') }).click()
  await app.hold(string('prescriptions_prescribed_date', day))
  await app.tap(string('edit_delete'))
  await expect(app.text(string('edit_prescription_delete_message'))).toBeVisible()
  await app.tap(string('edit_delete_keep_boxes'))

  const data = await savedData(page)
  expect(data.prescriptions).toHaveLength(0)
  expect(data.boxes[0]).not.toHaveProperty('prescriptionId')
  await app.openTab('main_tab_boxes')
  await expect(app.text(`${day} #1`)).toBeVisible()
  await expect(page.getByRole('button', { name: string('link_prescription_deleted') })).toBeDisabled()
})

test('holding a prescription card edits it', async ({ app, page }) => {
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(30, 1, 2)
  await app.hold(string('prescriptions_prescribed_date', await today(page)))
  await app.fill('prescriptions_field_pharmacy', 'Other Pharmacy')
  await app.tap(string('edit_save'))
  expect((await savedData(page)).prescriptions[0].pharmacy).toBe('Other Pharmacy')
})

test("a refill's pills left can be edited, and its box follows", async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.refillFirstBox()
  await app.openTab('main_tab_refills')
  await app.hold(string('refills_box', `${day} #1`, 23))
  await app.field('edit_field_refill_left_range', 0, 30).fill('20')
  await app.tap(string('edit_save'))
  const data = await savedData(page)
  expect(data.refills[0].amountRemaining).toBe(20)
  expect(data.refills[0].amount).toBe(7)
  expect(data.boxes[0].pillsLeft).toBe(20)
})

test("changing a refill's amount updates its pills left in the dialog", async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.refillFirstBox()
  await app.openTab('main_tab_refills')
  await app.hold(string('refills_box', `${day} #1`, 23))
  await app.field('edit_field_refill_amount', 30).fill('5')
  await expect(app.field('edit_field_refill_left_range', 0, 30)).toHaveValue('25')
  await app.tap(string('edit_save'))
  const data = await savedData(page)
  expect(data.refills[0].amount).toBe(5)
  expect(data.boxes[0].pillsLeft).toBe(25)
})

test('holding a refill card deletes it, returning its pills', async ({ app, page }) => {
  await app.addBox()
  await app.refillFirstBox()
  await app.openTab('main_tab_refills')
  await app.hold(string('refills_box', `${await today(page)} #1`, 23))
  await app.tap(string('edit_delete'))
  await app.tap(string('edit_delete'))
  const data = await savedData(page)
  expect(data.refills).toHaveLength(0)
  expect(data.boxes[0].pillsLeft).toBe(30)
})

test('a missed dose the day after a refill joins it as one pill less, and is still edited on its own', async ({ app, page }) => {
  const day = await today(page)
  await app.addBox()
  await app.refillFirstBox()
  await app.openTab('main_tab_refills')
  await app.tap(string('refills_log_missed'))
  // Logged on the refill's day, it's that day's pill, taken from before the refill
  await expect(app.text(string('refills_missed_pill'))).toHaveCount(0)

  await app.hold(string('refills_missed'))
  const tomorrow = await page.evaluate(() => {
    const date = new Date()
    date.setDate(date.getDate() + 1)
    const pad = (value: number) => String(value).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  })
  await app.fill('edit_field_date', tomorrow)
  await app.tap(string('edit_save'))

  await expect(app.text(string('refills_missed_pill'))).toBeVisible()
  await expect(app.text(string('refills_box', `${day} #1`, 23))).toBeVisible()
  expect((await savedData(page)).refills[0].amount).toBe(7)
})

test('holding a missed dose card edits it', async ({ app, page }) => {
  await app.openTab('main_tab_refills')
  await app.tap(string('refills_log_missed'))
  await app.hold(string('refills_missed'))
  await page.getByRole('radio', { name: string('refills_delayed') }).click()
  await app.tap(string('edit_save'))
  expect((await savedData(page)).misses[0].kind).toBe('DELAYED')
})

test('every reminder starts on and can be turned off', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  const switches = page.getByRole('switch')
  await expect(switches).toHaveCount(5)
  for (let index = 0; index < 5; index++) await expect(switches.nth(index)).toHaveAttribute('aria-checked', 'true')
  for (let index = 0; index < 5; index++) await switches.nth(index).click()
  const settings = (await savedData(page)).reminderSettings
  expect([settings.isRefillEnabled, settings.isPickupEnabled, settings.isRunOutEnabled, settings.isPillEnabled, settings.isExpiryEnabled])
    .toEqual([false, false, false, false, false])
})

test('notifications are asked for once the tutorial closes, as reminders start on', async ({ page }) => {
  await page.addInitScript(() => {
    ;(window as unknown as { asked: number }).asked = 0
    Notification.requestPermission = async () => {
      ;(window as unknown as { asked: number }).asked++
      return 'granted'
    }
  })
  await page.goto('./')
  await page.getByRole('button', { name: string('tutorial_skip') }).click()
  expect(await page.evaluate(() => (window as unknown as { asked: number }).asked)).toBe(1)
})

test('refill reminder is configured with a weekday and time', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await app.tap('Sunday')
  await page.getByRole('menuitem', { name: 'Monday' }).click()
  expect((await savedData(page)).reminderSettings.refillDay).toBe('MONDAY')

  await app.tap(string('reminders_choose_time', '20:00'))
  await page.getByLabel('Hour').fill('19')
  await app.tap(string('reminders_time_confirm'))
  expect((await savedData(page)).reminderSettings.refillTime).toBe('19:00')
  await expect(app.button(string('reminders_choose_time', '19:00'))).toBeVisible()
})

test('daily pill reminder is configured with a time', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await app.tap(string('reminders_choose_time', '08:00'))
  await expect(app.text(string('reminders_pill_time_dialog_title'))).toBeVisible()
  await page.getByLabel('Hour').fill('07')
  await app.tap(string('reminders_time_confirm'))
  expect((await savedData(page)).reminderSettings.pillTime).toBe('07:00')
  await expect(app.button(string('reminders_choose_time', '07:00'))).toBeVisible()
})

test('the expiry reminder starts on at 60 days, and is configured in days', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  const settings = (await savedData(page)).reminderSettings
  expect(settings.isExpiryEnabled).toBe(true)
  expect(settings.expiryDaysBefore).toBe(60)
  await page.getByRole('button', { name: string('reminders_more_days', string('reminders_expiry_title')) }).click()
  expect((await savedData(page)).reminderSettings.expiryDaysBefore).toBe(61)
})

test('pickup and run-out reminders are configured in days', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await page.getByRole('button', { name: string('reminders_more_days', string('reminders_pickup_title')) }).click()
  await page.getByRole('button', { name: string('reminders_fewer_days', string('reminders_run_out_title')) }).click()
  await expect(app.text(plural('reminders_days', 5, 5))).toBeVisible()
  await expect(app.text(plural('reminders_days', 13, 13))).toBeVisible()
  const settings = (await savedData(page)).reminderSettings
  expect(settings.isPickupEnabled && settings.isRunOutEnabled).toBe(true)
  expect(settings.pickupDaysAfterOrder).toBe(5)
  expect(settings.runOutDaysBefore).toBe(13)
})

test('help opens the tutorial, which moves through the tabs and asks for the pill time', async ({ app, page }) => {
  await app.openTab('main_tab_settings')
  await app.tap(string('settings_help'))
  await expect(app.text(string('tutorial_language_title'))).toBeVisible()
  // Marking a language previews it on the card only, until Next
  await page.getByRole('radio', { name: 'Español' }).click()
  await expect(app.text(string('tutorial_language_title'))).toHaveCount(0)
  await expect(app.tab('main_tab_boxes')).toHaveAttribute('aria-selected', 'true')
  await page.getByRole('radio', { name: 'English' }).click()
  // The app behind the tutorial ignores taps
  await app.tab('main_tab_refills').click({ force: true })
  await expect(app.tab('main_tab_boxes')).toHaveAttribute('aria-selected', 'true')
  for (let i = 0; i < 3; i++) await app.tap(string('tutorial_next'))
  await expect(app.text(string('tutorial_prescription_text'))).toBeVisible()
  await expect(app.tab('main_tab_prescriptions')).toHaveAttribute('aria-selected', 'true')
  // Sample records fill the tabs during the tour
  await expect(page.getByText(string('tutorial_sample_doctor'))).toBeVisible()

  for (let i = 0; i < 6; i++) await app.tap(string('tutorial_next'))
  await expect(app.text(string('tutorial_pill_time_text'))).toBeVisible()
  // One on the tutorial card, one on the Settings tab behind it
  await expect(app.button(string('reminders_choose_time', '08:00'))).toHaveCount(2)

  await app.tap(string('tutorial_skip'))
  await expect(app.text(string('tutorial_pill_time_text'))).toHaveCount(0)
  await app.openTab('main_tab_prescriptions')
  await expect(page.getByText(string('tutorial_sample_doctor'))).toHaveCount(0)
})

test('reminders tab shows when medication runs out', async ({ app, page }) => {
  await app.openTab('main_tab_prescriptions')
  await app.addPrescription(30, 1, 1)
  await app.openTab('main_tab_settings')
  // Today's pill counts only if its time is still to come
  const runOutDate = await page.evaluate(() => {
    const date = new Date()
    if (date.getHours() >= 8) date.setDate(date.getDate() + 1)
    date.setDate(date.getDate() + 30)
    const pad = (value: number) => String(value).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  })
  await expect(app.text(string('reminders_run_out_date', runOutDate))).toBeVisible()
})
