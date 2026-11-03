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
    TABS(AppTab.BOXES, TutorialTarget.TAB_BAR, R.string.tutorial_tabs_title, R.string.tutorial_tabs_text),
    PRESCRIPTION(
        AppTab.PRESCRIPTIONS,
        TutorialTarget.FIRST_PRESCRIPTION,
        R.string.tutorial_prescription_title,
        R.string.tutorial_prescription_text,
    ),
    ADD(AppTab.PRESCRIPTIONS, TutorialTarget.NEW_PRESCRIPTION, R.string.tutorial_add_title, R.string.tutorial_add_text),
    BOX(AppTab.BOXES, TutorialTarget.FIRST_BOX, R.string.tutorial_box_title, R.string.tutorial_box_text),
    EDIT(AppTab.BOXES, TutorialTarget.FIRST_BOX, R.string.tutorial_edit_title, R.string.tutorial_edit_text),
    REFILLS(
        AppTab.REFILLS,
        TutorialTarget.FIRST_REFILL,
        R.string.tutorial_refills_title,
        R.string.tutorial_refills_text,
    ),
    DOSES(AppTab.REFILLS, TutorialTarget.DOSE_LOG, R.string.tutorial_doses_title, R.string.tutorial_doses_text),
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
