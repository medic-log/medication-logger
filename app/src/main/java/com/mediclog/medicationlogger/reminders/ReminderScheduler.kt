package com.mediclog.medicationlogger.reminders

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.mediclog.medicationlogger.MainActivity
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderKind
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/** Sets one alarm per reminder kind for its next trigger, at the UTC offset the reminder times were set at. */
class ReminderScheduler(private val context: Context, private val clock: Clock) {
    private val alarmManager = requireNotNull(context.getSystemService<AlarmManager>())

    /** Replaces the alarms to match [data]. */
    fun reschedule(data: MedicationData) {
        val zone = reminderZone(data, clock)
        val now = LocalDateTime.now(clock.withZone(zone))
        ReminderKind.entries.forEach { kind ->
            val intent = pendingIntent(kind)
            val trigger = ReminderCalculator.nextTrigger(kind, data, now)
            if (trigger == null) {
                alarmManager.cancel(intent)
            } else {
                val triggerMillis = trigger.atZone(zone).toInstant().toEpochMilli()
                // Inexact ones can come hours late; they're the fallback if exact ones were revoked (Android 12)
                val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
                when {
                    !canBeExact -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
                    // Never deferred, at the cost of the status bar's alarm icon
                    kind.isTimed -> alarmManager.setAlarmClock(AlarmClockInfo(triggerMillis, openApp()), intent)
                    else -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
                }
            }
        }
    }

    companion object {
        /** The offset the reminder times are at, which they keep when the device's changes, until the app opens. */
        fun reminderZone(data: MedicationData, clock: Clock): ZoneId = data.reminderSettings.utcOffset ?: clock.zone
    }

    private val ReminderKind.isTimed get() = this == ReminderKind.PILL || this == ReminderKind.REFILL

    private fun openApp() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun pendingIntent(kind: ReminderKind): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMINDER)
            .putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(
            context,
            kind.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
