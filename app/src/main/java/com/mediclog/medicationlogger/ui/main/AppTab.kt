package com.mediclog.medicationlogger.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.components.TutorialTarget

/** The app's tabs, in display order, each with how the tutorial points at it in the tab bar. */
enum class AppTab(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val tutorialTarget: TutorialTarget,
) {
    SETTINGS(R.string.main_tab_settings, R.drawable.ic_settings, TutorialTarget.TAB_SETTINGS),
    PRESCRIPTIONS(R.string.main_tab_prescriptions, R.drawable.ic_prescription, TutorialTarget.TAB_PRESCRIPTIONS),
    BOXES(R.string.main_tab_boxes, R.drawable.ic_box, TutorialTarget.TAB_BOXES),
    REFILLS(R.string.main_tab_refills, R.drawable.ic_refill, TutorialTarget.TAB_REFILLS),
}
