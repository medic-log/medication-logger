package com.mediclog.medicationlogger.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mediclog.medicationlogger.MedicationLoggerApplication
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.data.ReminderKind
import com.mediclog.medicationlogger.data.ReminderSettings
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
import java.time.LocalTime
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
    fun `scheduler sets an alarm for the next refill`() {
        val settings = ReminderSettings(
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
        scheduler.reschedule(MedicationData(reminderSettings = ReminderSettings(isRefillEnabled = true)))

        scheduler.reschedule(MedicationData())

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

    @Test
    fun `a run-out notification says a new prescription is needed when no repetition is left`() {
        app.repository.addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 7, 1, 1))
        app.repository.buyRepetition(app.repository.data.value.prescriptions.single().id)
        app.repository.updateReminderSettings(ReminderSettings(isRunOutEnabled = true, runOutDaysBefore = 365))

        // Due long ago, whatever time of day the test runs
        fire(ReminderKind.RUN_OUT)

        val notification = shadowOf(notifications.allNotifications.single())
        assertEquals(app.getString(R.string.reminders_notification_run_out_title), notification.contentTitle)
        assertEquals(
            app.getString(R.string.reminders_notification_run_out_text, LocalDate.now().plusDays(7).toString()),
            notification.contentText,
        )
    }

    @Test
    fun `a run-out notification says to get a repetition when one is left`() {
        app.repository.addPrescription(PrescriptionDraft("Dr Test", "Test Pharmacy", 7, 1, 2))
        app.repository.buyRepetition(app.repository.data.value.prescriptions.single().id)
        app.repository.updateReminderSettings(ReminderSettings(isRunOutEnabled = true, runOutDaysBefore = 365))

        fire(ReminderKind.RUN_OUT)

        assertEquals(
            app.getString(
                R.string.reminders_notification_run_out_repetition_text,
                LocalDate.now().plusDays(7).toString(),
            ),
            shadowOf(notifications.allNotifications.single()).contentText,
        )
    }

    @Test
    fun `an alarm that's no longer due shows nothing`() {
        fire(ReminderKind.PICKUP)
        fire(ReminderKind.REFILL)

        assertTrue(notifications.allNotifications.isEmpty())
    }
}
