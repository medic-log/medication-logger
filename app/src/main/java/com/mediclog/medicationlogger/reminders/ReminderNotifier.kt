package com.mediclog.medicationlogger.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.mediclog.medicationlogger.MainActivity
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.ReminderCalculator
import com.mediclog.medicationlogger.data.ReminderKind
import java.time.Clock
import java.time.LocalDate

private const val CHANNEL_ID = "reminders"

/** Posts reminder notifications. Android drops them silently when notifications aren't allowed. */
class ReminderNotifier(private val context: Context, private val clock: Clock) {
    private val notificationManager = requireNotNull(context.getSystemService<NotificationManager>())

    /** Shows the notification for [kind], replacing any earlier one of the same kind. */
    fun show(kind: ReminderKind, data: MedicationData) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refill)
            .setContentTitle(context.getString(titleOf(kind)))
            .setContentText(textOf(kind, data))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(kind.ordinal + 1, notification)
    }

    private fun titleOf(kind: ReminderKind) = when (kind) {
        ReminderKind.REFILL -> R.string.reminders_notification_refill_title
        ReminderKind.PICKUP -> R.string.reminders_notification_pickup_title
        ReminderKind.RUN_OUT -> R.string.reminders_notification_run_out_title
        ReminderKind.PILL -> R.string.reminders_notification_pill_title
        ReminderKind.EXPIRY -> R.string.reminders_notification_expiry_title
    }

    private fun textOf(kind: ReminderKind, data: MedicationData) = when (kind) {
        ReminderKind.REFILL -> context.getString(R.string.reminders_notification_refill_text)
        ReminderKind.PICKUP -> context.getString(R.string.reminders_notification_pickup_text)
        ReminderKind.PILL -> context.getString(R.string.reminders_notification_pill_text)
        ReminderKind.RUN_OUT -> context.getString(
            if (data.prescriptions.any { it.canBuy }) {
                R.string.reminders_notification_run_out_repetition_text
            } else {
                R.string.reminders_notification_run_out_text
            },
            ReminderCalculator.boughtPillsRunOutDate(data, LocalDate.now(clock)).toString(),
        )
        ReminderKind.EXPIRY -> ReminderCalculator.expiringBox(data).let { box ->
            context.getString(
                R.string.reminders_notification_expiry_text,
                box?.name.orEmpty(),
                box?.expiryDate.toString(),
            )
        }
    }
}
