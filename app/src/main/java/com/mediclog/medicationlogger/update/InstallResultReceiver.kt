package com.mediclog.medicationlogger.update

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
 * the updated app, which the install closed.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            // This runs in the new version, started for this broadcast
            PackageInstaller.STATUS_SUCCESS -> openUpdatedApp(context)
            // A user who cancels needs no message
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit
            else -> Toast.makeText(context, R.string.settings_update_install_failed, Toast.LENGTH_LONG).show()
        }
    }

    /** Opens the app; Android 10 and later block that from the background, so there it posts a notification to tap. */
    private fun openUpdatedApp(context: Context) {
        val openApp = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            context.startActivity(openApp)
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
            .setContentTitle(context.getString(R.string.update_notification_title, BuildConfig.VERSION_NAME))
            .setContentText(context.getString(R.string.update_notification_text))
            .setContentIntent(PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
