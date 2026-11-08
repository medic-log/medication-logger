package com.mediclog.medicationlogger.ui.reminders

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R

/** The buttons in the settings section. */
enum class SettingsAction(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    HELP(R.string.settings_help, R.drawable.ic_help),
    UPDATE(R.string.settings_update, R.drawable.ic_update),
    EXPORT(R.string.settings_export, R.drawable.ic_export),
    IMPORT(R.string.settings_import, R.drawable.ic_import),
    SHARE(R.string.settings_share, R.drawable.ic_share),
    LANGUAGE(R.string.settings_language, R.drawable.ic_translation),
    DATE_FORMAT(R.string.settings_date_format, R.drawable.ic_calendar),
    FEEDBACK(R.string.settings_feedback, R.drawable.ic_feedback),
    ;

    companion object {
        /** The buttons as laid out, row by row. Without a download site there is nothing to update from or share. */
        val rows = listOf(listOf(HELP, LANGUAGE, DATE_FORMAT, UPDATE), listOf(SHARE, IMPORT, EXPORT, FEEDBACK))
    }
}
