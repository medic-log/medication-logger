package com.mediclog.medicationlogger.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mediclog.medicationlogger.MedicationLoggerApplication
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderKind
import java.time.LocalDateTime

/** Shows a reminder when its alarm fires, and sets the alarms again after that or after a reboot. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as MedicationLoggerApplication
        val data = app.repository.data.value
        if (intent.action == ACTION_REMINDER) {
            val kind = ReminderKind.entries.find { it.name == intent.getStringExtra(EXTRA_KIND) }
            if (kind != null && ReminderCalculator.isDue(kind, data, LocalDateTime.now(app.clock))) {
                ReminderNotifier(app, app.clock).show(kind, data)
            }
        }
        app.reminderScheduler.reschedule(data)
    }

    companion object {
        const val ACTION_REMINDER = "com.mediclog.medicationlogger.action.REMINDER"
        const val EXTRA_KIND = "kind"
    }
}
