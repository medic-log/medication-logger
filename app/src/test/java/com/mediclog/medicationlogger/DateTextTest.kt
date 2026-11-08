package com.mediclog.medicationlogger

import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.DateStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class DateTextTest {
    private val defaultLocale = Locale.getDefault()
    private val date = LocalDate.of(2026, 11, 4)
    private val month = YearMonth.of(2026, 11)

    @After
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    @Test
    fun `dates show in each fixed style`() {
        assertEquals("2026-11-04", date.displayText(DateStyle.YEAR_MONTH_DAY))
        assertEquals("04/11/2026", date.displayText(DateStyle.DAY_MONTH_YEAR))
        assertEquals("11/04/2026", date.displayText(DateStyle.MONTH_DAY_YEAR))
    }

    @Test
    fun `months show in each fixed style`() {
        assertEquals("2026-11", month.displayText(DateStyle.YEAR_MONTH_DAY))
        assertEquals("11/2026", month.displayText(DateStyle.DAY_MONTH_YEAR))
        assertEquals("11/2026", month.displayText(DateStyle.MONTH_DAY_YEAR))
    }

    @Test
    fun `the language style reads as the app's language writes dates`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Nov 4, 2026", date.displayText(DateStyle.LANGUAGE))
        assertEquals("Nov 2026", month.displayText(DateStyle.LANGUAGE))

        Locale.setDefault(Locale.forLanguageTag("es"))
        assertEquals("4 nov 2026", date.displayText(DateStyle.LANGUAGE))

        // Without the period Dutch puts after a short month, as on the web
        Locale.setDefault(Locale.forLanguageTag("nl"))
        assertEquals("sep 2026", YearMonth.of(2026, 9).displayText(DateStyle.LANGUAGE))
    }

    @Test
    fun `a box is named by its bought date and number`() {
        assertEquals("2026-11-04 #2", Box(1, 1, 2, date, 30, 30).displayName(DateStyle.YEAR_MONTH_DAY))
    }
}
