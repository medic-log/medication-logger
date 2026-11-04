package com.mediclog.medicationlogger.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.mediclog.medicationlogger.data.LatestRelease
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Downloads a release's APK into a [PackageInstaller] session and installs it, the system asking the user first. */
object ApkInstaller {
    private const val TIMEOUT_MILLIS = 30_000

    /**
     * Streams [release]'s APK into a session and commits it if its SHA-256 matches; false, with nothing installed,
     * if the download fails or doesn't match. [InstallResultReceiver] then shows the system's install prompt.
     */
    suspend fun install(
        context: Context,
        release: LatestRelease,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Boolean = withContext(dispatcher) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)
        var isCommitted = false
        try {
            installer.openSession(sessionId).use { session ->
                if (!download(release, session)) return@withContext false
                ensureActive()
                session.commit(statusReceiver(context, sessionId).intentSender)
                isCommitted = true
            }
            true
        } catch (_: IOException) {
            false
        } finally {
            if (!isCommitted) installer.abandonSession(sessionId)
        }
    }

    /** Writes the APK into [session]; whether it downloaded fully and matches the release's SHA-256. */
    private fun download(release: LatestRelease, session: PackageInstaller.Session): Boolean {
        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.useCaches = false
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
            val digest = MessageDigest.getInstance("SHA-256")
            session.openWrite("base.apk", 0, connection.contentLengthLong).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
                session.fsync(output)
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            return hash.equals(release.sha256, ignoreCase = true)
        } finally {
            connection.disconnect()
        }
    }

    private fun statusReceiver(context: Context, sessionId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        sessionId,
        Intent(context, InstallResultReceiver::class.java),
        // Mutable so the installer can add the status and the confirmation intent
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
}
