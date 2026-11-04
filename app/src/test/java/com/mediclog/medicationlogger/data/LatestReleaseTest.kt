package com.mediclog.medicationlogger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// org.json is an Android class, stubbed out in plain JVM tests
@RunWith(RobolectricTestRunner::class)
class LatestReleaseTest {
    private val siteUrl = "https://example.org/medication-logger/"

    @Test
    fun `latest json is read with the apk on the site`() {
        val json = """{"versionCode": 3, "versionName": "0.2.0", "apk": "medication-logger.apk", "sha256": "ab"}"""

        assertEquals(
            LatestRelease(3, "0.2.0", "https://example.org/medication-logger/medication-logger.apk", "ab"),
            LatestRelease.parse(json, siteUrl),
        )
    }

    @Test
    fun `invalid latest json gives null`() {
        assertNull(LatestRelease.parse("""{"versionName": "0.2.0"}""", siteUrl))
        assertNull(LatestRelease.parse("""{"versionCode": 3, "versionName": "0.2.0"}""", siteUrl))
        assertNull(LatestRelease.parse("<html>", siteUrl))
    }
}
