package com.mediclog.medicationlogger.ui.reminders

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R

/** The buttons in the settings section, in display order. */
enum class SettingsAction(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    HELP(R.string.settings_help, R.drawable.ic_help),
    UPDATE(R.string.settings_update, R.drawable.ic_update),
    EXPORT(R.string.settings_export, R.drawable.ic_export),
    IMPORT(R.string.settings_import, R.drawable.ic_import),
}
