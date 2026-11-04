package com.mediclog.medicationlogger.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// org.json is an Android class, stubbed out in plain JVM tests
@RunWith(RobolectricTestRunner::class)
class FeedbackTest {
    @Test
    fun `feedback json has the worker's fields and comes from the app`() {
        val json = JSONObject(Feedback("Crash", "bug", "0.3.3", "It \"closes\"\non start").toJson())

        assertEquals("Crash", json.getString("title"))
        assertEquals("bug", json.getString("topic"))
        assertEquals("0.3.3", json.getString("version"))
        assertEquals("It \"closes\"\non start", json.getString("message"))
        assertEquals("app", json.getString("source"))
        assertFalse(json.has("email"))
    }

    @Test
    fun `an email is sent when given`() {
        val json = JSONObject(Feedback("How?", "question", "0.3.3", "How do I add a box?", "me@example.org").toJson())

        assertEquals("me@example.org", json.getString("email"))
    }

    @Test
    fun `only email-like addresses are valid`() {
        assertTrue(Feedback.isValidEmail("me@example.org"))
        assertFalse(Feedback.isValidEmail("me@example"))
        assertFalse(Feedback.isValidEmail("me example@org.com"))
        assertFalse(Feedback.isValidEmail("@example.org"))
        assertFalse(Feedback.isValidEmail("a".repeat(250) + "@b.org"))
    }
}
