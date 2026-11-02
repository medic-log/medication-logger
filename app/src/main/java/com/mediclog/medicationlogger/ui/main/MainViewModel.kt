package com.mediclog.medicationlogger.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the app's frame shows: the current tab, the record a link pointed to, and any save problem. */
data class MainUiState(
    val selectedTab: AppTab = AppTab.BOXES,
    val focusedPrescriptionId: Long? = null,
    val focusedBoxId: Long? = null,
    val isSaveFailed: Boolean = false,
)

/** Tracks the selected tab and follows links between records. */
class MainViewModel(repository: MedicationRepository) : ViewModel() {
    private val navigation = MutableStateFlow(MainUiState())

    val uiState: StateFlow<MainUiState> = combine(navigation, repository.isSaveFailed) { state, isSaveFailed ->
        state.copy(isSaveFailed = isSaveFailed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState(isSaveFailed = repository.isSaveFailed.value))

    fun selectTab(tab: AppTab) {
        navigation.value = MainUiState(selectedTab = tab)
    }

    fun openPrescription(prescriptionId: Long) {
        navigation.value = MainUiState(selectedTab = AppTab.PRESCRIPTIONS, focusedPrescriptionId = prescriptionId)
    }

    fun openBox(boxId: Long) {
        navigation.value = MainUiState(selectedTab = AppTab.BOXES, focusedBoxId = boxId)
    }
}
