package com.mediclog.medicationlogger.data

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * The newest published version, as the download site's `latest.json` describes it, with its APK always at
 * `medication-logger.apk` on the site and [sha256] the APK's hex SHA-256.
 */
data class LatestRelease(val versionCode: Int, val versionName: String, val apkUrl: String, val sha256: String) {
    companion object {
        private const val TIMEOUT_MILLIS = 10_000
        private const val APK_FILE = "medication-logger.apk"

        /** Reads `latest.json` from [siteUrl]; null if that fails. */
        fun fetch(siteUrl: String): LatestRelease? = try {
            val connection = URL(URL(siteUrl), "latest.json").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                connection.useCaches = false
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    parse(connection.inputStream.use { it.readBytes().decodeToString() }, siteUrl)
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: IOException) {
            null
        }

        /** Parses `latest.json`, with the APK's address on [siteUrl]; null if it isn't valid. */
        fun parse(json: String, siteUrl: String): LatestRelease? = try {
            val root = JSONObject(json)
            LatestRelease(
                versionCode = root.getInt("versionCode"),
                versionName = root.getString("versionName"),
                apkUrl = URL(URL(siteUrl), APK_FILE).toString(),
                sha256 = root.getString("sha256"),
            )
        } catch (_: JSONException) {
            null
        } catch (_: MalformedURLException) {
            null
        }
    }
}
