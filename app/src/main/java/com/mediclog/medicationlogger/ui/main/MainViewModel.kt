package com.mediclog.medicationlogger.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.LatestRelease
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.data.ReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

/**
 * What the app's frame shows: the current tab, the record a link pointed to, any save problem,
 * the tutorial step on screen, if any, with the settings it asks for, and a newer version to offer.
 */
data class MainUiState(
    val selectedTab: AppTab = AppTab.BOXES,
    val focusedPrescriptionId: Long? = null,
    val focusedBoxId: Long? = null,
    val isSaveFailed: Boolean = false,
    val tutorialStep: TutorialStep? = null,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val availableUpdate: LatestRelease? = null,
)

/**
 * Tracks the selected tab, follows links between records, and leads the tutorial, which opens until it's done.
 * The tutorial shows sample records made from [sampleDraft] in place of the saved data. At startup it asks
 * [findUpdate] for a newer version, to offer once the tutorial is out of the way.
 */
class MainViewModel(
    private val repository: MedicationRepository,
    private val clock: Clock,
    private val sampleDraft: PrescriptionDraft,
    private val findUpdate: suspend () -> LatestRelease? = { null },
) : ViewModel() {
    private val availableUpdate = MutableStateFlow<LatestRelease?>(null)
    private val tutorialStep = MutableStateFlow(
        TutorialStep.entries.first().takeUnless { repository.data.value.isTutorialDone },
    )
    private val navigation = MutableStateFlow(MainUiState(selectedTab = tutorialStep.value?.tab ?: AppTab.BOXES))

    val uiState: StateFlow<MainUiState> = combine(
        navigation,
        repository.isSaveFailed,
        tutorialStep,
        repository.data,
        availableUpdate,
    ) { state, isSaveFailed, step, data, update ->
        state.copy(
            isSaveFailed = isSaveFailed,
            tutorialStep = step,
            reminderSettings = data.reminderSettings,
            availableUpdate = update.takeIf { step == null },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        navigation.value.copy(
            isSaveFailed = repository.isSaveFailed.value,
            tutorialStep = tutorialStep.value,
            reminderSettings = repository.data.value.reminderSettings,
        ),
    )

    init {
        if (tutorialStep.value != null) showSample()
        viewModelScope.launch { availableUpdate.value = findUpdate() }
    }

    fun selectTab(tab: AppTab) {
        navigation.value = MainUiState(selectedTab = tab)
    }

    fun openPrescription(prescriptionId: Long) {
        navigation.value = MainUiState(selectedTab = AppTab.PRESCRIPTIONS, focusedPrescriptionId = prescriptionId)
    }

    fun openBox(boxId: Long) {
        navigation.value = MainUiState(selectedTab = AppTab.BOXES, focusedBoxId = boxId)
    }

    fun startTutorial() {
        showSample()
        showTutorialStep(TutorialStep.entries.first())
    }

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
        repository.showSample(null)
        repository.finishTutorial()
    }

    /** Stops offering the update until the next start. */
    fun dismissUpdate() {
        availableUpdate.value = null
    }

    fun updateReminderSettings(settings: ReminderSettings) = repository.updateReminderSettings(settings)

    override fun onCleared() {
        repository.showSample(null)
    }

    private fun showSample() {
        val settings = repository.data.value.reminderSettings
        repository.showSample(tutorialSample(sampleDraft, settings, LocalDateTime.now(clock)))
    }

    private fun showTutorialStep(step: TutorialStep) {
        tutorialStep.value = step
        navigation.value = MainUiState(selectedTab = step.tab)
    }
}
