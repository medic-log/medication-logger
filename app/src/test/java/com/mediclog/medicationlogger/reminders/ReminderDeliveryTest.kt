package com.mediclog.medicationlogger.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mediclog.medicationlogger.MedicationLoggerApplication
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.AllRemindersOff
import com.mediclog.medicationlogger.data.DateStyle
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderKind
import com.mediclog.medicationlogger.data.ReminderSettings
import com.mediclog.medicationlogger.displayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class ReminderDeliveryTest {
    private val app = ApplicationProvider.getApplicationContext<MedicationLoggerApplication>()
    private val alarms = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    private val notifications = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    // A Monday
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun fire(kind: ReminderKind) {
        val intent = Intent(app, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMINDER)
            .putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
        ReminderReceiver().onReceive(app, intent)
    }

    @Test
    fun `alarms keep the offset the reminders were set at`() {
        val settings = AllRemindersOff.copy(
            isPillEnabled = true,
            pillTime = LocalTime.of(7, 30),
            utcOffset = ZoneOffset.ofHours(2),
        )

        ReminderScheduler(app, clock).reschedule(MedicationData(reminderSettings = settings))

        // 07:30 at UTC+2 is 05:30 UTC, which is past at noon, so tomorrow's
        val expected = today.plusDays(1).atTime(5, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, alarms.peekNextScheduledAlarm()?.triggerAtMs)
    }

    @Test
    fun `scheduler sets an alarm for the next refill`() {
        val settings = AllRemindersOff.copy(
            isRefillEnabled = true,
            refillDay = DayOfWeek.WEDNESDAY,
            refillTime = LocalTime.of(8, 30),
        )

        ReminderScheduler(app, clock).reschedule(MedicationData(reminderSettings = settings))

        val expected = today.plusDays(2).atTime(8, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, alarms.peekNextScheduledAlarm()?.triggerAtMs)
    }

    @Test
    fun `scheduler cancels the alarm when a reminder is turned off`() {
        val scheduler = ReminderScheduler(app, clock)
        scheduler.reschedule(MedicationData(reminderSettings = AllRemindersOff.copy(isRefillEnabled = true)))

        scheduler.reschedule(MedicationData(reminderSettings = AllRemindersOff))

        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun `a firing refill alarm shows the notification and sets the next one`() {
        app.repository.updateReminderSettings(ReminderSettings(isRefillEnabled = true))

        fire(ReminderKind.REFILL)

        val notification = notifications.allNotifications.single()
        assertEquals(
            app.getString(R.string.reminders_notification_refill_title),
            shadowOf(notification).contentTitle,
        )
        assertTrue(alarms.peekNextScheduledAlarm() != null)
    }

    // Today's pill counts only if its time is still to come
    private fun runOutDate(pills: Long) =
        ReminderCalculator.nextPill(ReminderSettings(), LocalDateTime.now()).toLocalDate().plusDays(pills).toString()

    @Test
    fun `a run-out notification says a new prescription is needed when no repetition is left`() {
        app.repository.addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 7, 1, 1))
        app.repository.buyRepetition(app.repository.data.value.prescriptions.single().id)
        app.repository.updateReminderSettings(ReminderSettings(isRunOutEnabled = true, runOutDaysBefore = 365))

        // Due long ago, whatever time of day the test runs
        fire(ReminderKind.RUN_OUT)

        val notification = shadowOf(notifications.allNotifications.single())
        assertEquals(app.getString(R.string.reminders_notification_run_out_title), notification.contentTitle)
        assertEquals(
            app.getString(R.string.reminders_notification_run_out_text, runOutDate(pills = 7)),
            notification.contentText,
        )
    }

    @Test
    fun `a run-out notification says to get a repetition when one is left`() {
        app.repository.addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 7, 1, 2))
        app.repository.buyRepetition(app.repository.data.value.prescriptions.single().id)
        app.repository.updateReminderSettings(ReminderSettings(isRunOutEnabled = true, runOutDaysBefore = 365))

        fire(ReminderKind.RUN_OUT)

        assertEquals(
            app.getString(
                R.string.reminders_notification_run_out_repetition_text,
                runOutDate(pills = 7),
            ),
            shadowOf(notifications.allNotifications.single()).contentText,
        )
    }

    @Test
    fun `a firing pill alarm shows the notification and sets the next one`() {
        app.repository.updateReminderSettings(ReminderSettings(isPillEnabled = true))

        fire(ReminderKind.PILL)

        val notification = shadowOf(notifications.allNotifications.single())
        assertEquals(app.getString(R.string.reminders_notification_pill_title), notification.contentTitle)
        assertEquals(app.getString(R.string.reminders_notification_pill_text), notification.contentText)
        assertTrue(alarms.peekNextScheduledAlarm() != null)
    }

    @Test
    fun `a firing expiry alarm names the box`() {
        app.repository.addPrescription(PrescriptionDraft("Test medication", "Dr Test", "Test Pharmacy", 7, 1, 1))
        val prescription = app.repository.data.value.prescriptions.single()
        app.repository.buyRepetition(prescription.id, listOf(YearMonth.now()))
        app.repository.updateReminderSettings(AllRemindersOff.copy(isExpiryEnabled = true))

        fire(ReminderKind.EXPIRY)

        val box = app.repository.data.value.boxes.single()
        val notification = shadowOf(notifications.allNotifications.single())
        assertEquals(app.getString(R.string.reminders_notification_expiry_title), notification.contentTitle)
        assertEquals(
            app.getString(R.string.reminders_notification_expiry_text, box.displayName(DateStyle.YEAR_MONTH_DAY), YearMonth.now().toString()),
            notification.contentText,
        )
    }

    @Test
    fun `an alarm that's no longer due shows nothing`() {
        app.repository.updateReminderSettings(AllRemindersOff)

        fire(ReminderKind.PICKUP)
        fire(ReminderKind.REFILL)
        fire(ReminderKind.PILL)
        fire(ReminderKind.EXPIRY)

        assertTrue(notifications.allNotifications.isEmpty())
    }
}
