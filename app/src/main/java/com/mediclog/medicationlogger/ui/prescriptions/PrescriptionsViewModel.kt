package com.mediclog.medicationlogger.ui.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediclog.medicationlogger.data.Box
import com.mediclog.medicationlogger.data.MedicationData
import com.mediclog.medicationlogger.data.MedicationRepository
import com.mediclog.medicationlogger.data.Prescription
import com.mediclog.medicationlogger.data.PrescriptionDraft
import com.mediclog.medicationlogger.ui.components.ListFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A prescription with the boxes bought with it. */
data class PrescriptionItem(val prescription: Prescription, val boxes: List<Box>)

/** The prescriptions to show, and the one that a new prescription starts from. */
data class PrescriptionsUiState(
    val filter: ListFilter = ListFilter.ACTIVE,
    val items: List<PrescriptionItem> = emptyList(),
    val previousPrescription: Prescription? = null,
)

/** Lists prescriptions, creates new ones, and orders and buys repetitions. */
class PrescriptionsViewModel(private val repository: MedicationRepository) : ViewModel() {
    private val filter = MutableStateFlow(ListFilter.ACTIVE)

    val uiState: StateFlow<PrescriptionsUiState> = combine(repository.data, filter, ::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(repository.data.value, filter.value))

    fun setFilter(newFilter: ListFilter) {
        filter.value = newFilter
    }

    fun addPrescription(draft: PrescriptionDraft) = repository.addPrescription(draft)

    fun orderRepetition(prescriptionId: Long) = repository.orderRepetition(prescriptionId)

    fun buyRepetition(prescriptionId: Long) = repository.buyRepetition(prescriptionId)

    private fun toUiState(data: MedicationData, filter: ListFilter) = PrescriptionsUiState(
        filter = filter,
        items = data.prescriptions
            .filter { filter == ListFilter.ALL || it.isActive }
            // Newest first within a status, as a new prescription is the one most likely to need attention
            .sortedWith(
                compareBy<Prescription> { it.status.sortRank() }
                    .thenByDescending { it.prescribedDate }
                    .thenByDescending { it.id },
            )
            .map { prescription ->
                PrescriptionItem(prescription, data.boxes.filter { it.prescriptionId == prescription.id })
            },
        previousPrescription = data.prescriptions.lastOrNull(),
    )

    private fun Prescription.Status.sortRank() = when (this) {
        Prescription.Status.ORDERED -> 0
        Prescription.Status.IN_PROGRESS -> 1
        Prescription.Status.NOT_STARTED -> 2
        Prescription.Status.FINISHED -> 3
    }
}
