package com.mediclog.medicationlogger

import android.Manifest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationMiss
import com.mediclog.medicationlogger.data.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// Tablet-sized, so whole lists are composed without scrolling. Not phone-sized: under Robolectric, a Material
// text field in a dialog never goes idle at some window sizes, including 412x915
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h1280dp")
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val today get() = LocalDate.now().toString()

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private fun plural(id: Int, count: Int, vararg args: Any) =
        composeRule.activity.resources.getQuantityString(id, count, *args)

    private val data: MedicationData
        get() = (composeRule.activity.application as MedicationLoggerApplication).repository.data.value

    private fun tap(text: String) = composeRule.onNodeWithText(text).performClick()

    private fun openTab(id: Int) = tap(string(id))

    private fun addPrescription(pillsPerBox: Int, boxesPerRepetition: Int, repetitions: Int) {
        composeRule.onNodeWithContentDescription(string(R.string.prescriptions_new)).performClick()
        fillField(R.string.prescriptions_field_doctor, "Dr Test")
        fillField(R.string.prescriptions_field_pharmacy, "Test Pharmacy")
        fillField(R.string.prescriptions_field_pills_per_box, pillsPerBox.toString())
        fillField(R.string.prescriptions_field_boxes_per_repetition, boxesPerRepetition.toString())
        fillField(R.string.prescriptions_field_repetitions_total, repetitions.toString())
        tap(string(R.string.prescriptions_save))
    }

    private fun fillField(labelId: Int, value: String) =
        composeRule.onNodeWithText(string(labelId)).performTextReplacement(value)

    private fun assertShown(text: String) {
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun assertNotShown(text: String) {
        composeRule.onNodeWithText(text).assertDoesNotExist()
    }

    private fun refillAmount(amount: Int, left: Int) = plural(R.plurals.refills_amount, amount, amount, left)

    @Test
    fun `app opens on the boxes tab`() {
        composeRule.onNodeWithText(string(R.string.main_tab_boxes)).assertIsSelected()
        assertShown(string(R.string.boxes_empty_active))
    }

    @Test
    fun `tabs are in the order reminders, prescriptions, boxes, refills`() {
        val tabs = composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        val expected = listOf(
            R.string.main_tab_reminders,
            R.string.main_tab_prescriptions,
            R.string.main_tab_boxes,
            R.string.main_tab_refills,
        ).map { string(it) }
        assertEquals(expected, tabs)
    }

    @Test
    fun `a new prescription is dated today, shows its totals and buys boxes`() {
        openTab(R.string.main_tab_prescriptions)
        assertShown(string(R.string.prescriptions_empty_active))

        addPrescription(pillsPerBox = 30, boxesPerRepetition = 3, repetitions = 2)

        assertShown(string(R.string.prescriptions_prescribed_date, today))
        assertShown(string(R.string.prescriptions_prescribed_by, "Dr Test"))
        assertShown(string(R.string.pharmacy_label, "Test Pharmacy"))
        assertShown(string(R.string.prescriptions_per_repetition, 3, 90))
        assertShown(string(R.string.prescriptions_total, 6, 180))
        val notStarted = string(R.string.prescriptions_status_not_started)
        assertShown(notStarted)

        tap(string(R.string.prescriptions_order))
        assertShown(string(R.string.prescriptions_status_ordered))
        assertShown(string(R.string.prescriptions_repetitions, 0, 2))
        composeRule.onNodeWithText(string(R.string.prescriptions_order)).assertIsNotEnabled()

        tap(string(R.string.prescriptions_buy))
        assertShown(string(R.string.prescriptions_status_in_progress))
        assertShown(string(R.string.prescriptions_repetitions, 1, 2))
        assertShown(string(R.string.prescriptions_bought_first, today))
        assertShown(string(R.string.prescriptions_bought_last, today))
        assertShown(string(R.string.link_box_number, 1))
        assertShown(string(R.string.link_box_number, 3))

        openTab(R.string.main_tab_boxes)
        assertShown("$today #1")
        assertShown("$today #2")
        assertShown("$today #3")
        composeRule.onAllNodesWithText(string(R.string.boxes_pills_left, 30, 30)).onFirst().assertIsDisplayed()

        openTab(R.string.main_tab_prescriptions)
        tap(string(R.string.prescriptions_buy))
        // Now finished, so only under "All"
        tap(string(R.string.filter_all))
        assertShown(string(R.string.link_box_number, 4))
        assertShown(string(R.string.link_box_number, 6))
        composeRule.onNodeWithText(string(R.string.prescriptions_buy)).assertIsNotEnabled()
    }

    @Test
    fun `a new prescription starts from the previous values`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 28, boxesPerRepetition = 2, repetitions = 5)

        composeRule.onNodeWithContentDescription(string(R.string.prescriptions_new)).performClick()

        composeRule.onNodeWithText("Dr Test").assertExists()
        composeRule.onNodeWithText("Test Pharmacy").assertExists()
        composeRule.onNodeWithText("28").assertExists()
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("5").assertExists()
        composeRule.onNodeWithText(string(R.string.prescriptions_save)).assertIsEnabled()
    }

    @Test
    fun `save stays disabled until the prescription is complete`() {
        openTab(R.string.main_tab_prescriptions)
        composeRule.onNodeWithContentDescription(string(R.string.prescriptions_new)).performClick()
        fillField(R.string.prescriptions_field_doctor, "Dr Test")

        composeRule.onNodeWithText(string(R.string.prescriptions_save)).assertIsNotEnabled()
    }

    @Test
    fun `refills follow the concept's example, with partial refill highlighted when incomplete`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 25, boxesPerRepetition = 1, repetitions = 1)
        tap(string(R.string.prescriptions_buy))
        // The filter hides the finished prescription
        composeRule.onNodeWithContentDescription(string(R.string.prescriptions_new)).performClick()
        fillSecondPrescriptionPillsPerBox(30)
        tap(string(R.string.prescriptions_save))
        tap(string(R.string.prescriptions_buy))
        openTab(R.string.main_tab_boxes)

        val weekly = string(R.string.boxes_refill_weekly)
        val pending = plural(R.plurals.boxes_refill_partial_pending, 3, 3)
        assertNotShown(pending)

        repeat(4) { composeRule.onAllNodesWithText(weekly).onFirst().performClick() }

        assertShown(plural(R.plurals.boxes_pending_notice, 3, 3))
        tap(pending)
        assertNotShown(pending)
        assertNotShown(plural(R.plurals.boxes_pending_notice, 3, 3))
        tap(weekly)
        assertShown(string(R.string.boxes_pills_left, 20, 30))

        openTab(R.string.main_tab_refills)
        assertShown(refillAmount(7, 18))
        assertShown(refillAmount(7, 11))
        assertShown(refillAmount(7, 4))
        assertShown(string(R.string.refills_partial_suffix, refillAmount(4, 0)))
        assertShown(string(R.string.refills_partial_suffix, refillAmount(3, 27)))
        assertShown(refillAmount(7, 20))
    }

    private fun fillSecondPrescriptionPillsPerBox(pills: Int) {
        composeRule.onNodeWithText("25").performTextReplacement(pills.toString())
    }

    @Test
    fun `links open the linked prescription and box, highlighted`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 30, boxesPerRepetition = 1, repetitions = 1)
        tap(string(R.string.prescriptions_buy))
        openTab(R.string.main_tab_boxes)
        tap(string(R.string.boxes_refill_weekly))

        openTab(R.string.main_tab_refills)
        composeRule.onNodeWithContentDescription(string(R.string.link_open_box)).performClick()
        composeRule.onNodeWithText(string(R.string.main_tab_boxes)).assertIsSelected()
        composeRule.onNode(isSelected() and hasAnyDescendant(hasText("$today #1"))).assertIsDisplayed()

        openTab(R.string.main_tab_refills)
        composeRule.onNodeWithContentDescription(string(R.string.link_open_prescription)).performClick()
        composeRule.onNodeWithText(string(R.string.main_tab_prescriptions)).assertIsSelected()
        // The prescription is finished, so the link has to reach past the active filter
        composeRule.onNode(
            isSelected() and hasAnyDescendant(hasText(string(R.string.prescriptions_prescribed_date, today))),
        ).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.link_box_number, 1)).performClick()
        composeRule.onNodeWithText(string(R.string.main_tab_boxes)).assertIsSelected()

        composeRule.onNodeWithContentDescription(string(R.string.link_open_prescription)).performClick()
        composeRule.onNodeWithText(string(R.string.main_tab_prescriptions)).assertIsSelected()
    }

    @Test
    fun `a box saved as extra leaves the active list, can't be refilled and can go back`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 30, boxesPerRepetition = 1, repetitions = 1)
        tap(string(R.string.prescriptions_buy))
        openTab(R.string.main_tab_boxes)

        tap(string(R.string.boxes_save_as_extra))
        assertNotShown("$today #1")
        assertShown(string(R.string.boxes_empty_active))

        tap(string(R.string.filter_all))
        // The filter and the status
        composeRule.onAllNodesWithText(string(R.string.boxes_status_extra)).assertCountEquals(2)
        assertNotShown(string(R.string.boxes_refill_weekly))

        tap(string(R.string.boxes_back_to_inventory))
        assertShown(string(R.string.boxes_status_not_started))
        composeRule.onNodeWithText(string(R.string.boxes_refill_weekly)).assertIsEnabled()
    }

    @Test
    fun `the filter hides finished prescriptions until all is selected`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 30, boxesPerRepetition = 1, repetitions = 1)
        tap(string(R.string.prescriptions_buy))
        val prescribedOn = string(R.string.prescriptions_prescribed_date, today)
        assertNotShown(prescribedOn)

        tap(string(R.string.filter_all))
        assertShown(prescribedOn)
        assertShown(string(R.string.prescriptions_status_finished))

        tap(string(R.string.filter_active))
        assertNotShown(prescribedOn)
    }

    @Test
    fun `misses and delays are registered in the refills tab`() {
        openTab(R.string.main_tab_refills)
        assertShown(string(R.string.refills_empty))

        tap(string(R.string.refills_log_missed))
        tap(string(R.string.refills_log_delayed))

        assertShown(string(R.string.refills_missed))
        assertShown(string(R.string.refills_delayed))
        composeRule.onAllNodesWithText(today).onFirst().assertIsDisplayed()
        assertEquals(2, data.misses.size)
    }

    private fun hold(text: String) {
        composeRule.onNodeWithText(text).performTouchInput { longClick(durationMillis = 1000) }
        tap(string(R.string.edit_action))
    }

    private fun addBox() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 30, boxesPerRepetition = 1, repetitions = 1)
        tap(string(R.string.prescriptions_buy))
        openTab(R.string.main_tab_boxes)
    }

    @Test
    fun `a short press on a card doesn't open it for editing`() {
        addBox()

        // Longer than the system's long press, shorter than the hold that opens a card
        composeRule.onNodeWithText("$today #1").performTouchInput {
            down(center)
            advanceEventTime(700)
            up()
        }

        assertNotShown(string(R.string.edit_confirm_title))
    }

    @Test
    fun `holding a card asks first, and cancelling doesn't open it for editing`() {
        addBox()

        composeRule.onNodeWithText("$today #1").performTouchInput { longClick(durationMillis = 1200) }
        assertShown(string(R.string.edit_confirm_title))
        assertNotShown(string(R.string.edit_box_title))
        tap(string(R.string.edit_cancel))

        assertNotShown(string(R.string.edit_confirm_title))
        assertNotShown(string(R.string.edit_box_title))
    }

    @Test
    fun `holding a box card edits its fields`() {
        addBox()

        hold("$today #1")
        assertShown(string(R.string.edit_box_title))
        fillField(R.string.edit_field_pills_left, "12")
        tap(string(R.string.edit_save))

        assertEquals(12, data.boxes.single().pillsLeft)
        assertNotShown(string(R.string.edit_box_title))
        assertShown(string(R.string.boxes_pills_left, 12, 30))
    }

    @Test
    fun `an edit that isn't valid can't be saved`() {
        addBox()

        hold("$today #1")
        fillField(R.string.edit_field_pills_left, "31")

        composeRule.onNodeWithText(string(R.string.edit_save)).assertIsNotEnabled()
    }

    @Test
    fun `deleting from the edit dialog asks first`() {
        addBox()

        hold("$today #1")
        tap(string(R.string.edit_delete))
        assertShown(string(R.string.edit_box_delete_message))
        tap(string(R.string.edit_cancel))
        assertEquals(1, data.boxes.size)
        assertShown(string(R.string.edit_box_title))

        tap(string(R.string.edit_delete))
        tap(string(R.string.edit_delete))
        assertTrue(data.boxes.isEmpty())
        assertNotShown("$today #1")
    }

    @Test
    fun `holding a prescription card edits it`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 30, boxesPerRepetition = 1, repetitions = 2)

        hold(string(R.string.prescriptions_prescribed_date, today))
        fillField(R.string.prescriptions_field_pharmacy, "Other Pharmacy")
        tap(string(R.string.edit_save))

        assertEquals("Other Pharmacy", data.prescriptions.single().pharmacy)
    }

    @Test
    fun `holding a refill card deletes it, returning its pills`() {
        addBox()
        tap(string(R.string.boxes_refill_weekly))
        openTab(R.string.main_tab_refills)

        hold(refillAmount(7, 23))
        tap(string(R.string.edit_delete))
        tap(string(R.string.edit_delete))

        assertTrue(data.refills.isEmpty())
        assertEquals(30, data.boxes.single().pillsLeft)
    }

    @Test
    fun `holding a missed dose card edits it`() {
        openTab(R.string.main_tab_refills)
        tap(string(R.string.refills_log_missed))

        hold(string(R.string.refills_missed))
        tap(string(R.string.refills_delayed))
        tap(string(R.string.edit_save))

        assertEquals(MedicationMiss.Kind.DELAYED, data.misses.single().kind)
    }

    @Test
    fun `every reminder starts on and can be turned off`() {
        openTab(R.string.main_tab_reminders)
        assertEquals(ReminderSettings(), data.reminderSettings)

        repeat(4) { composeRule.onAllNodes(isToggleable())[it].performClick() }

        assertFalse(data.reminderSettings.hasAnyEnabled)
    }

    @Test
    fun `notifications are asked for on launch, as reminders start on`() {
        assertEquals(
            Manifest.permission.POST_NOTIFICATIONS,
            shadowOf(composeRule.activity).lastRequestedPermission?.requestedPermissions?.single(),
        )
    }

    @Test
    fun `refill reminder is configured with a weekday and time`() {
        openTab(R.string.main_tab_reminders)
        tap(DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        tap(DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        assertEquals(DayOfWeek.MONDAY, data.reminderSettings.refillDay)

        tap(string(R.string.reminders_choose_time, "20:00"))
        // Stays in the evening, as the input is 12-hour here
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("07")
        tap(string(R.string.reminders_time_confirm))
        assertEquals(LocalTime.of(19, 0), data.reminderSettings.refillTime)
        assertShown(string(R.string.reminders_choose_time, "19:00"))
    }

    @Test
    fun `daily pill reminder is configured with a time`() {
        openTab(R.string.main_tab_reminders)
        tap(string(R.string.reminders_choose_time, "08:00"))
        assertShown(string(R.string.reminders_pill_time_dialog_title))
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("07")
        tap(string(R.string.reminders_time_confirm))

        assertEquals(LocalTime.of(7, 0), data.reminderSettings.pillTime)
        assertShown(string(R.string.reminders_choose_time, "07:00"))
    }

    @Test
    fun `pickup and run-out reminders are configured in days`() {
        openTab(R.string.main_tab_reminders)
        val pickup = string(R.string.reminders_pickup_title)
        val runOut = string(R.string.reminders_run_out_title)
        composeRule.onNodeWithContentDescription(string(R.string.reminders_more_days, pickup)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.reminders_fewer_days, runOut)).performClick()

        assertShown(plural(R.plurals.reminders_days, 5, 5))
        assertShown(plural(R.plurals.reminders_days, 13, 13))
        val settings = data.reminderSettings
        assertTrue(settings.isPickupEnabled && settings.isRunOutEnabled)
        assertEquals(5, settings.pickupDaysAfterOrder)
        assertEquals(13, settings.runOutDaysBefore)
    }

    @Test
    fun `reminders tab shows when medication runs out`() {
        openTab(R.string.main_tab_prescriptions)
        addPrescription(pillsPerBox = 30, boxesPerRepetition = 1, repetitions = 1)

        openTab(R.string.main_tab_reminders)

        val runOutDate = LocalDate.now().plusDays(30).toString()
        assertShown(string(R.string.reminders_run_out_date, runOutDate))
    }
}
