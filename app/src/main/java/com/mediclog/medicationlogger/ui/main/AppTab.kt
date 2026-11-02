package com.mediclog.medicationlogger.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R

/** The app's tabs, in display order. */
enum class AppTab(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    REMINDERS(R.string.main_tab_reminders, R.drawable.ic_reminders),
    PRESCRIPTIONS(R.string.main_tab_prescriptions, R.drawable.ic_prescription),
    BOXES(R.string.main_tab_boxes, R.drawable.ic_box),
    REFILLS(R.string.main_tab_refills, R.drawable.ic_refill),
}
