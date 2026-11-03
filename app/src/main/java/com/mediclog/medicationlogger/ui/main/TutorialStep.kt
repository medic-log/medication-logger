package com.mediclog.medicationlogger.ui.main

import androidx.annotation.StringRes
import com.mediclog.medicationlogger.R
import com.mediclog.medicationlogger.ui.components.TutorialTarget

/**
 * The tutorial's steps in order, each shown over the tab it explains and spotlighting its [target],
 * or its tab when that isn't on screen. Steps without a target blur the app instead.
 */
enum class TutorialStep(
    val tab: AppTab,
    val target: TutorialTarget?,
    @param:StringRes val titleRes: Int,
    @param:StringRes val textRes: Int,
) {
    WELCOME(AppTab.BOXES, null, R.string.tutorial_welcome_title, R.string.tutorial_welcome_text),
    PRESCRIPTIONS(
        AppTab.PRESCRIPTIONS,
        TutorialTarget.NEW_PRESCRIPTION,
        R.string.tutorial_prescriptions_title,
        R.string.tutorial_prescriptions_text,
    ),
    BOXES(AppTab.BOXES, TutorialTarget.FIRST_BOX, R.string.tutorial_boxes_title, R.string.tutorial_boxes_text),
    REFILLS(AppTab.REFILLS, TutorialTarget.DOSE_LOG, R.string.tutorial_refills_title, R.string.tutorial_refills_text),
    SETTINGS(
        AppTab.SETTINGS,
        TutorialTarget.TAB_SETTINGS,
        R.string.tutorial_settings_title,
        R.string.tutorial_settings_text,
    ),
    PILL_TIME(
        AppTab.SETTINGS,
        TutorialTarget.PILL_REMINDER,
        R.string.tutorial_pill_time_title,
        R.string.tutorial_pill_time_text,
    ),
    REFILL_TIME(
        AppTab.SETTINGS,
        TutorialTarget.REFILL_REMINDER,
        R.string.tutorial_refill_time_title,
        R.string.tutorial_refill_time_text,
    ),
    DONE(AppTab.BOXES, null, R.string.tutorial_done_title, R.string.tutorial_done_text),
    ;

    val previous: TutorialStep? get() = entries.getOrNull(ordinal - 1)
    val next: TutorialStep? get() = entries.getOrNull(ordinal + 1)
}
