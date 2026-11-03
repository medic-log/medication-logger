package com.mediclog.medicationlogger.ui.reminders

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.AllRemindersOff
import com.mediclog.medicationlogger.data.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun showScreen(
        settings: ReminderSettings,
        areNotificationsAllowed: Boolean,
        onSettingsAction: (SettingsAction) -> Unit = {},
        onOpen: () -> Unit = {},
    ) {
        composeRule.setContent {
            SettingsScreen(
                state = RemindersUiState(settings = settings, nextRefill = LocalDateTime.of(2026, 11, 8, 9, 0)),
                areNotificationsAllowed = areNotificationsAllowed,
                onSettingsChange = {},
                onOpenNotificationSettings = onOpen,
                onSettingsAction = onSettingsAction,
            )
        }
    }

    @Test
    fun `denied notifications are explained, with a way to allow them`() {
        var isOpened = false
        showScreen(ReminderSettings(isRefillEnabled = true), areNotificationsAllowed = false) { isOpened = true }

        composeRule.onNodeWithText(string(R.string.reminders_notifications_off)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reminders_open_settings)).performClick()

        assertTrue(isOpened)
    }

    @Test
    fun `no notice while every reminder is off`() {
        showScreen(AllRemindersOff, areNotificationsAllowed = false)

        composeRule.onNodeWithText(string(R.string.reminders_notifications_off)).assertDoesNotExist()
    }

    @Test
    fun `each settings button reports its action`() {
        val tapped = mutableListOf<SettingsAction>()
        showScreen(ReminderSettings(), areNotificationsAllowed = true, onSettingsAction = { tapped += it })

        val shown = SettingsAction.rows.flatten()
        shown.forEach { action ->
            composeRule.onNodeWithText(composeRule.activity.getString(action.labelRes))
                .performScrollTo()
                .performClick()
        }

        assertEquals(shown, tapped)
    }
}
