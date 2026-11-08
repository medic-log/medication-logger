package com.mediclog.medicationlogger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.DateStyle
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

// The default locale is the app's language: Android 13+ sets it from the per-app locale, and AppLanguage.wrap before

/** The date in [style]: "2026-11-14", "14/11/2026", "11/14/2026" or, in English, "Nov 14, 2026". */
fun LocalDate.displayText(style: DateStyle): String = when (style) {
    DateStyle.YEAR_MONTH_DAY -> toString()
    DateStyle.DAY_MONTH_YEAR -> format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    DateStyle.MONTH_DAY_YEAR -> format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    DateStyle.LANGUAGE ->
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(this)
}

/** The month in [style]: "2026-11", "11/2026" or, in English, "Nov 2026". */
fun YearMonth.displayText(style: DateStyle): String = when (style) {
    DateStyle.YEAR_MONTH_DAY -> toString()
    DateStyle.DAY_MONTH_YEAR, DateStyle.MONTH_DAY_YEAR -> format(DateTimeFormatter.ofPattern("MM/yyyy"))
    DateStyle.LANGUAGE -> "${month.shortName(Locale.getDefault())} $year"
}

/** The month's short name in [locale] without the period some languages add, as the web app writes it: "Sep". */
fun Month.shortName(locale: Locale): String = getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")

/** The box's name, its bought date and number: "2026-11-14 #2". */
fun Box.displayName(style: DateStyle): String = "${boughtDate.displayText(style)} #$boxNumber"

/** The date style chosen in Settings, for the screens. */
val LocalDateStyle = staticCompositionLocalOf { DateStyle.YEAR_MONTH_DAY }

/** The date in the chosen style. */
@Composable
@ReadOnlyComposable
fun LocalDate.displayText(): String = displayText(LocalDateStyle.current)

/** The month in the chosen style. */
@Composable
@ReadOnlyComposable
fun YearMonth.displayText(): String = displayText(LocalDateStyle.current)

/** The box's name in the chosen style. */
@Composable
@ReadOnlyComposable
fun Box.displayName(): String = displayName(LocalDateStyle.current)
