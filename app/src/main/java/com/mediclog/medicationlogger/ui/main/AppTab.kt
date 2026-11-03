package com.mediclog.medicationlogger.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R

/** The app's tabs, in display order. */
enum class AppTab(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    SETTINGS(R.string.main_tab_settings, R.drawable.ic_settings),
    PRESCRIPTIONS(R.string.main_tab_prescriptions, R.drawable.ic_prescription),
    BOXES(R.string.main_tab_boxes, R.drawable.ic_box),
    REFILLS(R.string.main_tab_refills, R.drawable.ic_refill),
}
