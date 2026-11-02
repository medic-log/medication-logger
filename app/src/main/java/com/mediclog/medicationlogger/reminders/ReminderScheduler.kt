package com.mediclog.medicationlogger.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderKind
import java.time.Clock
import java.time.LocalDateTime

/** Sets one alarm per reminder kind for its next trigger. */
class ReminderScheduler(private val context: Context, private val clock: Clock) {
    private val alarmManager = requireNotNull(context.getSystemService<AlarmManager>())

    /** Replaces the alarms to match [data]. */
    fun reschedule(data: MedicationData) {
        val now = LocalDateTime.now(clock)
        ReminderKind.entries.forEach { kind ->
            val intent = pendingIntent(kind)
            val trigger = ReminderCalculator.nextTrigger(kind, data, now)
            if (trigger == null) {
                alarmManager.cancel(intent)
            } else {
                // Inexact: exact alarms need a special permission, and a reminder a few minutes late is fine
                val triggerMillis = trigger.atZone(clock.zone).toInstant().toEpochMilli()
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
            }
        }
    }

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
