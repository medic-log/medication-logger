package com.mediclog.medicationlogger.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

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

    /** Posts it to [url] and tells how it went. Blocks, so call it off the main thread. */
    fun send(url: String): SendResult = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(toJson().encodeToByteArray()) }
            SendResult.of(connection.responseCode)
        } finally {
            connection.disconnect()
        }
    } catch (_: UnknownHostException) {
        // Without a connection the name can't be looked up
        SendResult.OFFLINE
    } catch (_: IOException) {
        SendResult.UNAVAILABLE
    }

    /** How sending went: [OFFLINE] and [UNAVAILABLE] never reached the Worker, [REJECTED] did but it said no. */
    enum class SendResult {
        SENT,
        OFFLINE,
        UNAVAILABLE,
        REJECTED;

        companion object {
            /** The result of the Worker answering with [code]: busy (429) and server errors are worth retrying later. */
            fun of(code: Int): SendResult = when (code) {
                in 200..299 -> SENT
                429, in 500..599 -> UNAVAILABLE
                else -> REJECTED
            }
        }
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
