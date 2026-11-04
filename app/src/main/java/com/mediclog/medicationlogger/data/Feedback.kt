package com.mediclog.medicationlogger.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Feedback for the Cloudflare feedback Worker, which turns it into an issue; [topic] is one of its topics, and [email],
 * if not blank, is where to answer.
 */
data class Feedback(
    val title: String,
    val topic: String,
    val version: String,
    val message: String,
    val email: String = "",
) {
    /** The JSON the Worker reads, marked as sent from the app. */
    fun toJson(): String = JSONObject()
        .put("title", title)
        .put("topic", topic)
        .put("version", version)
        .put("message", message)
        .put("source", "app")
        .apply { if (email.isNotBlank()) put("email", email) }
        .toString()

    /** Posts it to [url]; true if the Worker accepted it. Blocks, so call it off the main thread. */
    fun send(url: String): Boolean = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(toJson().encodeToByteArray()) }
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    } catch (_: IOException) {
        false
    }

    companion object {
        /** The longest title the Worker accepts. */
        const val MAX_TITLE_LENGTH = 120

        /** The longest message the Worker accepts. */
        const val MAX_MESSAGE_LENGTH = 5000

        /** The longest email the Worker accepts. */
        const val MAX_EMAIL_LENGTH = 254

        private const val TIMEOUT_MILLIS = 10_000

        // The same check as the Worker's
        private val EMAIL = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")

        /** Whether [email] looks like an address the Worker accepts. */
        fun isValidEmail(email: String): Boolean = email.length <= MAX_EMAIL_LENGTH && EMAIL.matches(email)
    }
}
