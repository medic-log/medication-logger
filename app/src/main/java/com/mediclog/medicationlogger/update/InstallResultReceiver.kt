package com.mediclog.medicationlogger.update

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import com.mediclog.medicationlogger.BuildConfig
import com.mediclog.medicationlogger.MainActivity
import com.mediclog.medicationlogger.R

private const val CHANNEL_ID = "updates"
private const val NOTIFICATION_ID = 100

/**
 * Shows the system's install prompt for an update [ApkInstaller] committed, says if the install failed, and opens
 * the updated app, which the install closed. Both open directly when allowed, otherwise through a notification.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                    ?: return
                openOrNotify(
                    context,
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    context.getString(R.string.update_install_notification_title),
                    context.getString(R.string.update_install_notification_text),
                )
            }
            // This runs in the new version, started for this broadcast
            PackageInstaller.STATUS_SUCCESS -> openOrNotify(
                context,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                context.getString(R.string.update_notification_title, BuildConfig.VERSION_NAME),
                context.getString(R.string.update_notification_text),
            )
            // A user who cancels needs no message
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit
            else -> Toast.makeText(context, R.string.settings_update_install_failed, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Starts [activity]; Android 10 and later block that while the app is in the background, so there it posts a
     * notification that starts it when tapped.
     */
    private fun openOrNotify(context: Context, activity: Intent, title: String, text: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || isInForeground()) {
            context.startActivity(activity)
            return
        }
        val notificationManager = context.getSystemService<NotificationManager>() ?: return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_update)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    activity,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun isInForeground(): Boolean = ActivityManager.RunningAppProcessInfo()
        .also { ActivityManager.getMyMemoryState(it) }
        .importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}
