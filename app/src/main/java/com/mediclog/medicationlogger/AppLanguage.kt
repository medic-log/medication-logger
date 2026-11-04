package com.mediclog.medicationlogger

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

private const val PREFERENCES_NAME = "language"
private const val KEY_TAG = "tag"

/** The languages the app is translated into. It never follows the phone's language: English until another is chosen. */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    DUTCH("nl", "Nederlands"),
    ;

    companion object {
        /** The language chosen in the app, English if none is. */
        fun current(context: Context): AppLanguage {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java).applicationLocales[0]?.language
            } else {
                preferences(context).getString(KEY_TAG, null)
            }
            return entries.firstOrNull { it.tag == tag } ?: ENGLISH
        }

        /** On Android 13+, sets English as the app's language until one is chosen, so the phone's isn't used. */
        fun applyDefault(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.forLanguageTags(ENGLISH.tag)
            }
        }

        /** Switches the app to [language], redrawing [activity] in it. */
        fun choose(activity: Activity, language: AppLanguage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android keeps the choice and recreates the activity itself
                activity.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(language.tag)
            } else {
                preferences(activity).edit { putString(KEY_TAG, language.tag) }
                activity.recreate()
            }
        }

        /** Before Android 13, which has no per-app languages, gives [base] the chosen language. */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val locale = Locale.forLanguageTag(current(base).tag)
            // Dates and weekday names follow the default locale
            Locale.setDefault(locale)
            val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(configuration)
        }

        private fun preferences(context: Context) =
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}
