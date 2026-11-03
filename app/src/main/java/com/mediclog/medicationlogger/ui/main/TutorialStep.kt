package com.mediclog.medicationlogger.ui.main

import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R

/** The tutorial's steps in order, each shown over the tab it explains. */
enum class TutorialStep(val tab: AppTab, @param:StringRes val titleRes: Int, @param:StringRes val textRes: Int) {
    WELCOME(AppTab.BOXES, R.string.tutorial_welcome_title, R.string.tutorial_welcome_text),
    PRESCRIPTIONS(AppTab.PRESCRIPTIONS, R.string.tutorial_prescriptions_title, R.string.tutorial_prescriptions_text),
    BOXES(AppTab.BOXES, R.string.tutorial_boxes_title, R.string.tutorial_boxes_text),
    REFILLS(AppTab.REFILLS, R.string.tutorial_refills_title, R.string.tutorial_refills_text),
    SETTINGS(AppTab.SETTINGS, R.string.tutorial_settings_title, R.string.tutorial_settings_text),
    PILL_TIME(AppTab.SETTINGS, R.string.tutorial_pill_time_title, R.string.tutorial_pill_time_text),
    REFILL_TIME(AppTab.SETTINGS, R.string.tutorial_refill_time_title, R.string.tutorial_refill_time_text),
    DONE(AppTab.BOXES, R.string.tutorial_done_title, R.string.tutorial_done_text),
    ;

    val previous: TutorialStep? get() = entries.getOrNull(ordinal - 1)
    val next: TutorialStep? get() = entries.getOrNull(ordinal + 1)
}
