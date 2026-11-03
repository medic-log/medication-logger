package com.mediclog.medicationlogger.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.ReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * What the app's frame shows: the current tab, the record a link pointed to, any save problem,
 * and the tutorial step on screen, if any, with the settings it asks for.
 */
data class MainUiState(
    val selectedTab: AppTab = AppTab.BOXES,
    val focusedPrescriptionId: Long? = null,
    val focusedBoxId: Long? = null,
    val isSaveFailed: Boolean = false,
    val tutorialStep: TutorialStep? = null,
    val reminderSettings: ReminderSettings = ReminderSettings(),
)

/** Tracks the selected tab, follows links between records, and leads the tutorial, which opens until it's done. */
class MainViewModel(private val repository: MedicationRepository) : ViewModel() {
    private val tutorialStep = MutableStateFlow(
        TutorialStep.entries.first().takeUnless { repository.data.value.isTutorialDone },
    )
    private val navigation = MutableStateFlow(MainUiState(selectedTab = tutorialStep.value?.tab ?: AppTab.BOXES))

    val uiState: StateFlow<MainUiState> = combine(
        navigation,
        repository.isSaveFailed,
        tutorialStep,
        repository.data,
    ) { state, isSaveFailed, step, data ->
        state.copy(isSaveFailed = isSaveFailed, tutorialStep = step, reminderSettings = data.reminderSettings)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        navigation.value.copy(
            isSaveFailed = repository.isSaveFailed.value,
            tutorialStep = tutorialStep.value,
            reminderSettings = repository.data.value.reminderSettings,
        ),
    )

    fun selectTab(tab: AppTab) {
        navigation.value = MainUiState(selectedTab = tab)
    }

    fun openPrescription(prescriptionId: Long) {
        navigation.value = MainUiState(selectedTab = AppTab.PRESCRIPTIONS, focusedPrescriptionId = prescriptionId)
    }

    fun openBox(boxId: Long) {
        navigation.value = MainUiState(selectedTab = AppTab.BOXES, focusedBoxId = boxId)
    }

    fun startTutorial() = showTutorialStep(TutorialStep.entries.first())

    fun nextTutorialStep() {
        val next = tutorialStep.value?.next
        if (next == null) finishTutorial() else showTutorialStep(next)
    }

    fun previousTutorialStep() {
        tutorialStep.value?.previous?.let(::showTutorialStep)
    }

    /** Closes the tutorial, from its last step or by skipping it, so it no longer opens on its own. */
    fun finishTutorial() {
        tutorialStep.value = null
        repository.finishTutorial()
    }

    fun updateReminderSettings(settings: ReminderSettings) = repository.updateReminderSettings(settings)

    private fun showTutorialStep(step: TutorialStep) {
        tutorialStep.value = step
        navigation.value = MainUiState(selectedTab = step.tab)
    }
}
